package com.mtcarpenter.mall.portal.component;

import com.mtcarpenter.mall.common.constant.ProductStockQueueConstants;
import com.mtcarpenter.mall.common.domain.SkuStockLockMessage;
import com.mtcarpenter.mall.portal.service.OptimizedSkuStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Consumes SKU stock lock messages.
 */
@Component
@RabbitListener(queues = ProductStockQueueConstants.STOCK_LOCK_QUEUE)
public class SkuStockLockReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkuStockLockReceiver.class);

    @Autowired
    private OptimizedSkuStockService optimizedSkuStockService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 函数作用：处理SKU库存锁定消息，数据库级锁，防止并发场景下的库存不足问题
     * 后续流程
     * - 用户支付成功 → 扣减实际库存（stock - 2）
     * - 用户取消订单 → 释放锁定库存（lock_stock - 2）
     * - 超时未支付 → 自动取消订单，释放锁定库存
     * @param message
     */
    @RabbitHandler
    public void handle(SkuStockLockMessage message) {
        // 检查订单是否已取消
        // 如果订单已取消，直接返回
        // 避免处理已取消订单的库存锁
        if (isCanceled(message.getOrderSn())) {
            LOGGER.info("skip canceled sku stock lock message, orderSn:{}", message.getOrderSn());
            return;
        }
        // 尝试锁定库存，数据库级锁
        // 如果锁定失败，直接返回
        // 避免处理库存不足的情况
        optimizedSkuStockService.lockStock(message);
        //为什么需要第二次检查？                                   
        //- 防止在锁库存的过程中订单被取消                         
        //- 并发场景下的安全保护 
        if (isCanceled(message.getOrderSn())) {
            // 如果在锁库存过程中订单被取消了，需要解锁
            optimizedSkuStockService.unlockStock(message);
            LOGGER.info("release stock lock for canceled order, orderSn:{}", message.getOrderSn());
            return;
        }
        // 标记订单为已锁定
        stringRedisTemplate.opsForValue().set(
                ProductStockQueueConstants.STOCK_LOCKED_KEY_PREFIX + message.getOrderSn(),
                "1", // 值不重要，关键是 key 存在
                ProductStockQueueConstants.STOCK_MARK_EXPIRE_SECONDS,
                TimeUnit.SECONDS);
        LOGGER.info("process sku stock lock message, orderSn:{}", message.getOrderSn());
    }

    private boolean isCanceled(String orderSn) {
        Boolean result = stringRedisTemplate.hasKey(ProductStockQueueConstants.STOCK_CANCELED_KEY_PREFIX + orderSn);
        return Boolean.TRUE.equals(result);
    }
}
