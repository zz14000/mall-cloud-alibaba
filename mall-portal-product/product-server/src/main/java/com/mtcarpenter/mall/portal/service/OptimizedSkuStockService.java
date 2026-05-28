package com.mtcarpenter.mall.portal.service;

import com.mtcarpenter.mall.common.domain.SkuStockLockMessage;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persist optimized SKU stock lock messages.
 */
public interface OptimizedSkuStockService {
    @Transactional
    void lockStock(SkuStockLockMessage message);

    @Transactional
    void unlockStock(SkuStockLockMessage message);
}
