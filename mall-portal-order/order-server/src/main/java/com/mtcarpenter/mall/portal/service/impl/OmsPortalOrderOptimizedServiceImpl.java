package com.mtcarpenter.mall.portal.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mtcarpenter.mall.client.CouponFeign;
import com.mtcarpenter.mall.client.MemberFeign;
import com.mtcarpenter.mall.common.domain.SkuStockLockItem;
import com.mtcarpenter.mall.common.domain.SkuStockLockMessage;
import com.mtcarpenter.mall.common.exception.Asserts;
import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.domain.SmsCouponHistoryDetail;
import com.mtcarpenter.mall.mapper.OmsOrderItemMapper;
import com.mtcarpenter.mall.mapper.OmsOrderMapper;
import com.mtcarpenter.mall.mapper.OmsOrderSettingMapper;
import com.mtcarpenter.mall.model.OmsOrder;
import com.mtcarpenter.mall.model.OmsOrderExample;
import com.mtcarpenter.mall.model.OmsOrderItem;
import com.mtcarpenter.mall.model.OmsOrderItemExample;
import com.mtcarpenter.mall.model.OmsOrderSetting;
import com.mtcarpenter.mall.model.OmsOrderSettingExample;
import com.mtcarpenter.mall.model.SmsCoupon;
import com.mtcarpenter.mall.model.SmsCouponProductCategoryRelation;
import com.mtcarpenter.mall.model.SmsCouponProductRelation;
import com.mtcarpenter.mall.model.UmsIntegrationConsumeSetting;
import com.mtcarpenter.mall.model.UmsMember;
import com.mtcarpenter.mall.model.UmsMemberReceiveAddress;
import com.mtcarpenter.mall.portal.component.OptimizedCancelOrderSender;
import com.mtcarpenter.mall.portal.component.SkuStockLockSender;
import com.mtcarpenter.mall.portal.dao.PortalOrderDao;
import com.mtcarpenter.mall.portal.dao.PortalOrderItemDao;
import com.mtcarpenter.mall.portal.domain.OmsOrderDetail;
import com.mtcarpenter.mall.portal.domain.OrderParam;
import com.mtcarpenter.mall.portal.service.OmsCartItemService;
import com.mtcarpenter.mall.portal.service.OmsPortalOrderOptimizedService;
import com.mtcarpenter.mall.portal.service.RedisOrderCreateLockService;
import com.mtcarpenter.mall.portal.service.RedisSkuStockReserveService;
import com.mtcarpenter.mall.portal.util.MemberUtil;
import com.mtcarpenter.mall.security.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optimized order flow.
 */
@Service
public class OmsPortalOrderOptimizedServiceImpl implements OmsPortalOrderOptimizedService {
    private static final long ORDER_CREATE_LOCK_EXPIRE_SECONDS = 10;

    @Autowired
    private OmsCartItemService cartItemService;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private PortalOrderItemDao orderItemDao;
    @Autowired
    private RedisService redisService;
    @Value("${redis.key.orderId}")
    private String REDIS_KEY_ORDER_ID;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    @Autowired
    private PortalOrderDao portalOrderDao;
    @Autowired
    private OmsOrderSettingMapper orderSettingMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private OptimizedCancelOrderSender optimizedCancelOrderSender;
    @Autowired
    private CouponFeign couponFeign;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private MemberFeign memberFeign;
    @Autowired
    private MemberUtil memberUtil;
    @Autowired
    private RedisOrderCreateLockService orderCreateLockService;
    @Autowired
    private RedisSkuStockReserveService skuStockReserveService;
    @Autowired
    private SkuStockLockSender skuStockLockSender;

    @Override
    public Map<String, Object> generateOrder(OrderParam orderParam) {
        UmsMember currentMember = memberUtil.getRedisUmsMember(request);
        String requestId = UUID.randomUUID().toString();
        boolean locked = orderCreateLockService.tryLock(
                currentMember.getId(), requestId, ORDER_CREATE_LOCK_EXPIRE_SECONDS);
        if (!locked) {
            Asserts.fail("订单正在处理中，请勿重复提交");
        }
        registerLockRelease(currentMember.getId(), requestId);

        String orderSn = null;
        boolean stockReserved = false;
        try {
            List<OmsOrderItem> orderItemList = new ArrayList<>();
            List<CartPromotionItem> cartPromotionItemList = cartItemService.listPromotion(
                    currentMember.getId(), orderParam.getCartIds());
            for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
                OmsOrderItem orderItem = new OmsOrderItem();
                orderItem.setProductId(cartPromotionItem.getProductId());
                orderItem.setProductName(cartPromotionItem.getProductName());
                orderItem.setProductPic(cartPromotionItem.getProductPic());
                orderItem.setProductAttr(cartPromotionItem.getProductAttr());
                orderItem.setProductBrand(cartPromotionItem.getProductBrand());
                orderItem.setProductSn(cartPromotionItem.getProductSn());
                orderItem.setProductPrice(cartPromotionItem.getPrice());
                orderItem.setProductQuantity(cartPromotionItem.getQuantity());
                orderItem.setProductSkuId(cartPromotionItem.getProductSkuId());
                orderItem.setProductSkuCode(cartPromotionItem.getProductSkuCode());
                orderItem.setProductCategoryId(cartPromotionItem.getProductCategoryId());
                orderItem.setPromotionAmount(cartPromotionItem.getReduceAmount());
                orderItem.setPromotionName(cartPromotionItem.getPromotionMessage());
                orderItem.setGiftIntegration(cartPromotionItem.getIntegration());
                orderItem.setGiftGrowth(cartPromotionItem.getGrowth());
                orderItemList.add(orderItem);
            }
            if (!hasStock(cartPromotionItemList)) {
                Asserts.fail("库存不足，无法下单");
            }
            handleCoupon(orderParam, cartPromotionItemList, orderItemList, currentMember);
            handleIntegration(orderParam, currentMember, orderItemList);
            handleRealAmount(orderItemList);

            OmsOrder order = buildOrder(orderParam, currentMember, orderItemList);
            orderSn = order.getOrderSn();
            //在redis侧存入商品数量
            skuStockReserveService.initStockIfAbsent(cartPromotionItemList);
            //库存预扣
            skuStockReserveService.reserve(orderSn, cartPromotionItemList);
            stockReserved = true;

            orderMapper.insert(order);
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setOrderId(order.getId());
                orderItem.setOrderSn(order.getOrderSn());
            }
            orderItemDao.insertList(orderItemList);

            if (orderParam.getCouponId() != null) {
                couponFeign.updateCouponStatus(orderParam.getCouponId(), currentMember.getId(), 1);
            }
            if (orderParam.getUseIntegration() != null) {
                order.setUseIntegration(orderParam.getUseIntegration());
                memberFeign.updateIntegration(currentMember.getId(), -orderParam.getUseIntegration());
            }
            deleteCartItemList(cartPromotionItemList, currentMember);

            SkuStockLockMessage stockLockMessage = buildStockLockMessage(order, cartPromotionItemList);
            runAfterCommit(new Runnable() {
                @Override
                public void run() {
                    //消费者在mall-portal-product
                    skuStockLockSender.sendMessage(stockLockMessage);
                    sendDelayMessageCancelOrder(order.getId());
                }
            });

            Map<String, Object> result = new HashMap<>();
            result.put("order", order);
            result.put("orderItemList", orderItemList);
            return result;
        } catch (RuntimeException e) {
            if (stockReserved) {
                skuStockReserveService.rollback(orderSn);
            }
            throw e;
        }
    }

    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        OmsOrderDetail orderDetail = portalOrderDao.getDetail(orderId);
        waitStockLockPersisted(orderDetail.getOrderSn());
        OmsOrder order = new OmsOrder();
        order.setId(orderId);
        order.setStatus(1);
        order.setPaymentTime(new Date());
        order.setPayType(payType);
        orderMapper.updateByPrimaryKeySelective(order);
        int count = portalOrderDao.updateSkuStock(orderDetail.getOrderItemList());
        skuStockReserveService.finish(orderDetail.getOrderSn());
        return count;
    }

    @Override
    public void cancelOrder(Long orderId) {
        OmsOrderExample example = new OmsOrderExample();
        example.createCriteria().andIdEqualTo(orderId).andStatusEqualTo(0).andDeleteStatusEqualTo(0);
        List<OmsOrder> cancelOrderList = orderMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cancelOrderList)) {
            return;
        }
        OmsOrder cancelOrder = cancelOrderList.get(0);
        skuStockReserveService.markCanceled(cancelOrder.getOrderSn());
        cancelOrder.setStatus(4);
        orderMapper.updateByPrimaryKeySelective(cancelOrder);
        OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
        orderItemExample.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
        if (!CollectionUtils.isEmpty(orderItemList) && skuStockReserveService.isStockLockPersisted(cancelOrder.getOrderSn())) {
            portalOrderDao.releaseSkuStockLock(orderItemList);
        }
        skuStockReserveService.rollback(cancelOrder.getOrderSn());
        couponFeign.updateCouponStatus(cancelOrder.getCouponId(), cancelOrder.getMemberId(), 0);
        if (cancelOrder.getUseIntegration() != null) {
            memberFeign.updateIntegration(cancelOrder.getMemberId(), cancelOrder.getUseIntegration());
        }
    }

    @Override
    public void sendDelayMessageCancelOrder(Long orderId) {
        OmsOrderSetting orderSetting = orderSettingMapper.selectByPrimaryKey(1L);
        long delayTimes = orderSetting.getNormalOrderOvertime() * 60 * 1000;
        optimizedCancelOrderSender.sendMessage(orderId, delayTimes);
    }

    private void waitStockLockPersisted(String orderSn) {
        for (int i = 0; i < 20; i++) {
            if (skuStockReserveService.isStockLockPersisted(orderSn)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Asserts.fail("库存锁定落库处理中，请稍后重试");
    }

    private void registerLockRelease(final Long memberId, final String requestId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                //afterCompletion事务完成后执行，无论是提交还是回滚
                @Override
                public void afterCompletion(int status) {
                    orderCreateLockService.release(memberId, requestId);
                }
            });
        } else {
            orderCreateLockService.release(memberId, requestId);
        }
    }

    /**
     * 这个函数是 Spring 事务同步机制的 封装工具方法 ，核心作用是：
     * 1. 保证执行时机 ：确保某些操作在事务提交后才执行
     * 2. 避免数据不一致 ：防止事务回滚但后续操作已执行的问题
     * 3. 解耦业务逻辑 ：将依赖事务成功的操作与主业务分离
     * @param runnable
     */
    private void runAfterCommit(final Runnable runnable) {
        //- TransactionSynchronizationManager ：Spring 提供的事务同步管理器
        //- isSynchronizationActive() ：判断当前线程是否有活跃的事务
        //- 底层原理 ：检查 ThreadLocal 中是否绑定了事务同步资源
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            //- 创建事务同步适配器 ： TransactionSynchronizationAdapter 是 Spring 提供的适配器类
            //- 重写 afterCommit() 方法 ：事务提交后执行的回调逻辑
            //- 注册到事务管理器 ：将回调注册到当前事务
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        //如果没有事务上下文，立即执行任务
        } else {
            runnable.run();
        }
    }

    private void handleCoupon(OrderParam orderParam, List<CartPromotionItem> cartPromotionItemList,
                              List<OmsOrderItem> orderItemList, UmsMember currentMember) {
        if (orderParam.getCouponId() == null) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setCouponAmount(new BigDecimal(0));
            }
        } else {
            SmsCouponHistoryDetail couponHistoryDetail = getUseCoupon(
                    cartPromotionItemList, orderParam.getCouponId(), currentMember.getId());
            if (couponHistoryDetail == null) {
                Asserts.fail("该优惠券不可用");
            }
            handleCouponAmount(orderItemList, couponHistoryDetail);
        }
    }

    private void handleIntegration(OrderParam orderParam, UmsMember currentMember, List<OmsOrderItem> orderItemList) {
        if (orderParam.getUseIntegration() == null || orderParam.getUseIntegration().equals(0)) {
            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setIntegrationAmount(new BigDecimal(0));
            }
        } else {
            BigDecimal totalAmount = calcTotalAmount(orderItemList);
            BigDecimal integrationAmount = getUseIntegrationAmount(
                    orderParam.getUseIntegration(),
                    totalAmount,
                    currentMember,
                    orderParam.getCouponId() != null);
            if (integrationAmount.compareTo(new BigDecimal(0)) == 0) {
                Asserts.fail("积分不可用");
            }
            for (OmsOrderItem orderItem : orderItemList) {
                BigDecimal perAmount = orderItem.getProductPrice()
                        .divide(totalAmount, 3, RoundingMode.HALF_EVEN)
                        .multiply(integrationAmount);
                orderItem.setIntegrationAmount(perAmount);
            }
        }
    }

    private OmsOrder buildOrder(OrderParam orderParam, UmsMember currentMember, List<OmsOrderItem> orderItemList) {
        OmsOrder order = new OmsOrder();
        order.setDiscountAmount(new BigDecimal(0));
        order.setTotalAmount(calcTotalAmount(orderItemList));
        order.setFreightAmount(new BigDecimal(0));
        order.setPromotionAmount(calcPromotionAmount(orderItemList));
        order.setPromotionInfo(getOrderPromotionInfo(orderItemList));
        if (orderParam.getCouponId() == null) {
            order.setCouponAmount(new BigDecimal(0));
        } else {
            order.setCouponId(orderParam.getCouponId());
            order.setCouponAmount(calcCouponAmount(orderItemList));
        }
        if (orderParam.getUseIntegration() == null) {
            order.setIntegration(0);
            order.setIntegrationAmount(new BigDecimal(0));
        } else {
            order.setIntegration(orderParam.getUseIntegration());
            order.setIntegrationAmount(calcIntegrationAmount(orderItemList));
        }
        order.setPayAmount(calcPayAmount(order));
        order.setMemberId(currentMember.getId());
        order.setCreateTime(new Date());
        order.setMemberUsername(currentMember.getUsername());
        order.setPayType(orderParam.getPayType());
        order.setSourceType(1);
        order.setStatus(0);
        order.setOrderType(0);
        UmsMemberReceiveAddress address = memberFeign.getItem(orderParam.getMemberReceiveAddressId()).getData();
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhoneNumber());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverRegion(address.getRegion());
        order.setReceiverDetailAddress(address.getDetailAddress());
        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        order.setIntegration(calcGifIntegration(orderItemList));
        order.setGrowth(calcGiftGrowth(orderItemList));
        order.setOrderSn(generateOrderSn(order));
        List<OmsOrderSetting> orderSettings = orderSettingMapper.selectByExample(new OmsOrderSettingExample());
        if (CollUtil.isNotEmpty(orderSettings)) {
            order.setAutoConfirmDay(orderSettings.get(0).getConfirmOvertime());
        }
        return order;
    }

    private SkuStockLockMessage buildStockLockMessage(OmsOrder order, List<CartPromotionItem> cartPromotionItemList) {
        List<SkuStockLockItem> itemList = new ArrayList<>();
        Map<Long, Integer> skuQuantityMap = new HashMap<>();
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            Integer quantity = skuQuantityMap.get(cartPromotionItem.getProductSkuId());
            if (quantity == null) {
                skuQuantityMap.put(cartPromotionItem.getProductSkuId(), cartPromotionItem.getQuantity());
            } else {
                skuQuantityMap.put(cartPromotionItem.getProductSkuId(), quantity + cartPromotionItem.getQuantity());
            }
        }
        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            SkuStockLockItem item = new SkuStockLockItem();
            item.setSkuId(entry.getKey());
            item.setQuantity(entry.getValue());
            itemList.add(item);
        }
        SkuStockLockMessage message = new SkuStockLockMessage();
        message.setOrderId(order.getId());
        message.setOrderSn(order.getOrderSn());
        message.setItemList(itemList);
        return message;
    }

    private String generateOrderSn(OmsOrder order) {
        StringBuilder sb = new StringBuilder();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = REDIS_DATABASE + ":" + REDIS_KEY_ORDER_ID + date;
        Long increment = redisService.incr(key, 1);
        sb.append(date);
        sb.append(String.format("%02d", order.getSourceType()));
        sb.append(String.format("%02d", order.getPayType()));
        String incrementStr = increment.toString();
        if (incrementStr.length() <= 6) {
            sb.append(String.format("%06d", increment));
        } else {
            sb.append(incrementStr);
        }
        return sb.toString();
    }

    private void deleteCartItemList(List<CartPromotionItem> cartPromotionItemList, UmsMember currentMember) {
        List<Long> ids = new ArrayList<>();
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            ids.add(cartPromotionItem.getId());
        }
        cartItemService.delete(currentMember.getId(), ids);
    }

    private Integer calcGiftGrowth(List<OmsOrderItem> orderItemList) {
        Integer sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum = sum + orderItem.getGiftGrowth() * orderItem.getProductQuantity();
        }
        return sum;
    }

    private Integer calcGifIntegration(List<OmsOrderItem> orderItemList) {
        int sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum += orderItem.getGiftIntegration() * orderItem.getProductQuantity();
        }
        return sum;
    }

    private void handleRealAmount(List<OmsOrderItem> orderItemList) {
        for (OmsOrderItem orderItem : orderItemList) {
            BigDecimal realAmount = orderItem.getProductPrice()
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getIntegrationAmount());
            orderItem.setRealAmount(realAmount);
        }
    }

    private String getOrderPromotionInfo(List<OmsOrderItem> orderItemList) {
        StringBuilder sb = new StringBuilder();
        for (OmsOrderItem orderItem : orderItemList) {
            sb.append(orderItem.getPromotionName());
            sb.append(";");
        }
        String result = sb.toString();
        if (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private BigDecimal calcPayAmount(OmsOrder order) {
        return order.getTotalAmount()
                .add(order.getFreightAmount())
                .subtract(order.getPromotionAmount())
                .subtract(order.getCouponAmount())
                .subtract(order.getIntegrationAmount());
    }

    private BigDecimal calcIntegrationAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal integrationAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getIntegrationAmount() != null) {
                integrationAmount = integrationAmount.add(
                        orderItem.getIntegrationAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return integrationAmount;
    }

    private BigDecimal calcCouponAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal couponAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getCouponAmount() != null) {
                couponAmount = couponAmount.add(
                        orderItem.getCouponAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return couponAmount;
    }

    private BigDecimal calcPromotionAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal promotionAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getPromotionAmount() != null) {
                promotionAmount = promotionAmount.add(
                        orderItem.getPromotionAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return promotionAmount;
    }

    private BigDecimal getUseIntegrationAmount(Integer useIntegration, BigDecimal totalAmount,
                                               UmsMember currentMember, boolean hasCoupon) {
        BigDecimal zeroAmount = new BigDecimal(0);
        if (useIntegration.compareTo(currentMember.getIntegration()) > 0) {
            return zeroAmount;
        }
        UmsIntegrationConsumeSetting integrationConsumeSetting = memberFeign.integrationConsumeSetting(1L).getData();
        if (hasCoupon && integrationConsumeSetting.getCouponStatus().equals(0)) {
            return zeroAmount;
        }
        if (useIntegration.compareTo(integrationConsumeSetting.getUseUnit()) < 0) {
            return zeroAmount;
        }
        BigDecimal integrationAmount = new BigDecimal(useIntegration)
                .divide(new BigDecimal(integrationConsumeSetting.getUseUnit()), 2, RoundingMode.HALF_EVEN);
        BigDecimal maxPercent = new BigDecimal(integrationConsumeSetting.getMaxPercentPerOrder())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        if (integrationAmount.compareTo(totalAmount.multiply(maxPercent)) > 0) {
            return zeroAmount;
        }
        return integrationAmount;
    }

    private void handleCouponAmount(List<OmsOrderItem> orderItemList, SmsCouponHistoryDetail couponHistoryDetail) {
        SmsCoupon coupon = couponHistoryDetail.getCoupon();
        if (coupon.getUseType().equals(0)) {
            calcPerCouponAmount(orderItemList, coupon);
        } else if (coupon.getUseType().equals(1)) {
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(
                    couponHistoryDetail, orderItemList, 0);
            calcPerCouponAmount(couponOrderItemList, coupon);
        } else if (coupon.getUseType().equals(2)) {
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(
                    couponHistoryDetail, orderItemList, 1);
            calcPerCouponAmount(couponOrderItemList, coupon);
        }
    }

    private void calcPerCouponAmount(List<OmsOrderItem> orderItemList, SmsCoupon coupon) {
        BigDecimal totalAmount = calcTotalAmount(orderItemList);
        for (OmsOrderItem orderItem : orderItemList) {
            BigDecimal couponAmount = orderItem.getProductPrice()
                    .divide(totalAmount, 3, RoundingMode.HALF_EVEN)
                    .multiply(coupon.getAmount());
            orderItem.setCouponAmount(couponAmount);
        }
    }

    private List<OmsOrderItem> getCouponOrderItemByRelation(SmsCouponHistoryDetail couponHistoryDetail,
                                                            List<OmsOrderItem> orderItemList,
                                                            int type) {
        List<OmsOrderItem> result = new ArrayList<>();
        if (type == 0) {
            List<Long> categoryIdList = new ArrayList<>();
            for (SmsCouponProductCategoryRelation productCategoryRelation : couponHistoryDetail.getCategoryRelationList()) {
                categoryIdList.add(productCategoryRelation.getProductCategoryId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (categoryIdList.contains(orderItem.getProductCategoryId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        } else if (type == 1) {
            List<Long> productIdList = new ArrayList<>();
            for (SmsCouponProductRelation productRelation : couponHistoryDetail.getProductRelationList()) {
                productIdList.add(productRelation.getProductId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (productIdList.contains(orderItem.getProductId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        }
        return result;
    }

    private SmsCouponHistoryDetail getUseCoupon(List<CartPromotionItem> cartPromotionItemList,
                                                Long couponId,
                                                Long memberId) {
        List<SmsCouponHistoryDetail> couponHistoryDetailList =
                couponFeign.listCartPromotion(1, cartPromotionItemList, memberId).getData();
        for (SmsCouponHistoryDetail couponHistoryDetail : couponHistoryDetailList) {
            if (couponHistoryDetail.getCoupon().getId().equals(couponId)) {
                return couponHistoryDetail;
            }
        }
        return null;
    }

    private BigDecimal calcTotalAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsOrderItem item : orderItemList) {
            totalAmount = totalAmount.add(item.getProductPrice().multiply(new BigDecimal(item.getProductQuantity())));
        }
        return totalAmount;
    }

    private boolean hasStock(List<CartPromotionItem> cartPromotionItemList) {
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            if (cartPromotionItem.getRealStock() == null || cartPromotionItem.getRealStock() <= 0) {
                return false;
            }
        }
        return true;
    }
}
