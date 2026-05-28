package com.mtcarpenter.mall.portal.service;

import com.mtcarpenter.mall.domain.CartPromotionItem;

import java.util.List;

/**
 * Redis SKU stock reservation service.
 */
public interface RedisSkuStockReserveService {
    void initStockIfAbsent(List<CartPromotionItem> cartPromotionItemList);

    void reserve(String orderSn, List<CartPromotionItem> cartPromotionItemList);

    void rollback(String orderSn);

    void finish(String orderSn);

    void markCanceled(String orderSn);

    boolean isStockLockPersisted(String orderSn);
}
