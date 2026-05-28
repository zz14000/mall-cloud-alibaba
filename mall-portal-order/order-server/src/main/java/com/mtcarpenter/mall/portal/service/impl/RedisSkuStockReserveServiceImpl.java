package com.mtcarpenter.mall.portal.service.impl;

import com.mtcarpenter.mall.common.exception.Asserts;
import com.mtcarpenter.mall.common.constant.ProductStockQueueConstants;
import com.mtcarpenter.mall.domain.CartPromotionItem;
import com.mtcarpenter.mall.portal.service.RedisSkuStockReserveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Atomic Redis stock reservation based on Lua.
 */
@Service
public class RedisSkuStockReserveServiceImpl implements RedisSkuStockReserveService {
    private static final String SKU_STOCK_KEY_PREFIX = "mall:pms:sku:stock:";
    private static final String SKU_RESERVED_KEY_PREFIX = "mall:pms:sku:reserved:";
    private static final long RESERVE_EXPIRE_SECONDS = 30 * 60;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 这段代码用于秒杀或库存预扣场景 ：
     * - 在活动开始前，将商品库存预热到 Redis
     * - 使用 setIfAbsent 确保即使多次调用也不会覆盖已有的库存数据，与分布式锁不同，没有设置过期时间
     * - 后续的库存扣减会基于 Redis 中的数据进行
     */
    @Override
    public void initStockIfAbsent(List<CartPromotionItem> cartPromotionItemList) {
        for (CartPromotionItem item : cartPromotionItemList) {
            if (item.getProductSkuId() == null || item.getRealStock() == null) {
                Asserts.fail("SKU stock data is incomplete");
            }
            stringRedisTemplate.opsForValue().setIfAbsent(
                    buildStockKey(item.getProductSkuId()),
                    String.valueOf(item.getRealStock()));
        }
    }

    @Override
    public void reserve(String orderSn, List<CartPromotionItem> cartPromotionItemList) {
        if (cartPromotionItemList == null || cartPromotionItemList.isEmpty()) {
            Asserts.fail("No SKU stock needs to be reserved");
        }
        // 从购物车中提取商品 ID 和数量,以skuId为键
        // 键：商品 ID，值：数量
        Map<Long, Integer> skuQuantityMap = getSkuQuantityMap(cartPromotionItemList);
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        // 假设 skuQuantityMap 有 2 个商品：{skuId1: 2, skuId2: 1}
        // 第 1 步：收集所有 SKU 的库存 key 和扣减数量
        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            keys.add(buildStockKey(entry.getKey()));      // KEYS[1] = "mall:pms:sku:stock:skuId1"
            args.add(String.valueOf(entry.getValue()));   // ARGV[1] = "2"
        }                                                 // KEYS[2] = "mall:pms:sku:stock:skuId2"
                                                          // ARGV[2] = "1"

        // 第 2 步：收集所有 SKU ID（用于记录预扣明细）
        for (Map.Entry<Long, Integer> entry : skuQuantityMap.entrySet()) {
            args.add(String.valueOf(entry.getKey()));     // ARGV[3] = "skuId1"
        }                                                 // ARGV[4] = "skuId2"

        // 第 3 步：添加过期时间
        args.add(String.valueOf(RESERVE_EXPIRE_SECONDS)); // ARGV[5] = "1800" (30 分钟)

        // 第 4 步：添加预扣记录 key（按订单号）
        keys.add(buildReservedKey(orderSn));              // KEYS[3] = "mall:pms:sku:reserved:orderSn"

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(reserveScript());
        redisScript.setResultType(Long.class);
        Long result = stringRedisTemplate.execute(redisScript, keys, args.toArray());
        if (result == null || result == -1L) {
            Asserts.fail("SKU stock cache is not initialized");
        }
        if (result == 0L) {
            Asserts.fail("库存不足，无法下单");
        }
    }

    @Override
    public void rollback(String orderSn) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(rollbackScript());
        redisScript.setResultType(Long.class);
        stringRedisTemplate.execute(
                redisScript,
                Collections.singletonList(buildReservedKey(orderSn)),
                SKU_STOCK_KEY_PREFIX);
    }

    @Override
    public void finish(String orderSn) {
        // 完成订单后，删除预扣记录和锁定状态
        stringRedisTemplate.delete(buildReservedKey(orderSn));
        // 删除锁定状态
        stringRedisTemplate.delete(ProductStockQueueConstants.STOCK_LOCKED_KEY_PREFIX + orderSn);
        // 删除取消状态
        stringRedisTemplate.delete(ProductStockQueueConstants.STOCK_CANCELED_KEY_PREFIX + orderSn);
    }

    @Override
    public void markCanceled(String orderSn) {
        stringRedisTemplate.opsForValue().set(
                ProductStockQueueConstants.STOCK_CANCELED_KEY_PREFIX + orderSn,
                "1",
                ProductStockQueueConstants.STOCK_MARK_EXPIRE_SECONDS,
                java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public boolean isStockLockPersisted(String orderSn) {
        Boolean result = stringRedisTemplate.hasKey(ProductStockQueueConstants.STOCK_LOCKED_KEY_PREFIX + orderSn);
        return Boolean.TRUE.equals(result);
    }

    private String buildStockKey(Long skuId) {
        return SKU_STOCK_KEY_PREFIX + skuId;
    }

    private String buildReservedKey(String orderSn) {
        return SKU_RESERVED_KEY_PREFIX + orderSn;
    }

    private Map<Long, Integer> getSkuQuantityMap(List<CartPromotionItem> cartPromotionItemList) {
        Map<Long, Integer> skuQuantityMap = new LinkedHashMap<>();
        for (CartPromotionItem item : cartPromotionItemList) {
            Integer quantity = skuQuantityMap.get(item.getProductSkuId());
            if (quantity == null) {
                skuQuantityMap.put(item.getProductSkuId(), item.getQuantity());
            } else {
                skuQuantityMap.put(item.getProductSkuId(), quantity + item.getQuantity());
            }
        }
        return skuQuantityMap;
    }

    private String reserveScript() {
        return "local reserveKey = KEYS[#KEYS]\n" +
                "local n = #KEYS - 1\n" +
                "for i = 1, n do\n" +
                "  local stock = redis.call('get', KEYS[i])\n" +
                "  if stock == false then return -1 end\n" +
                "  if tonumber(stock) < tonumber(ARGV[i]) then return 0 end\n" +
                "end\n" +
                "for i = 1, n do\n" +
                "  redis.call('decrby', KEYS[i], tonumber(ARGV[i]))\n" +
                "  redis.call('hincrby', reserveKey, ARGV[n + i], tonumber(ARGV[i]))\n" +
                "end\n" +
                "redis.call('expire', reserveKey, tonumber(ARGV[2 * n + 1]))\n" +
                "return 1";
    }

    private String rollbackScript() {
        return "local data = redis.call('hgetall', KEYS[1])\n" +
                "if #data == 0 then return 0 end\n" +
                "for i = 1, #data, 2 do\n" +
                "  redis.call('incrby', ARGV[1] .. data[i], tonumber(data[i + 1]))\n" +
                "end\n" +
                "redis.call('del', KEYS[1])\n" +
                "return 1";
    }
}
