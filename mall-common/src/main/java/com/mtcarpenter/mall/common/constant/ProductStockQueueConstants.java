package com.mtcarpenter.mall.common.constant;

/**
 * Queue names and Redis marker keys for async SKU stock persistence.
 */
public class ProductStockQueueConstants {
    public static final String STOCK_EXCHANGE = "mall.product.stock.direct";
    public static final String STOCK_LOCK_QUEUE = "mall.product.stock.lock";
    public static final String STOCK_LOCK_ROUTE_KEY = "mall.product.stock.lock";
    public static final String STOCK_LOCKED_KEY_PREFIX = "mall:pms:sku:locked:";
    public static final String STOCK_CANCELED_KEY_PREFIX = "mall:pms:sku:canceled:";
    public static final long STOCK_MARK_EXPIRE_SECONDS = 24 * 60 * 60;

    private ProductStockQueueConstants() {
    }
}
