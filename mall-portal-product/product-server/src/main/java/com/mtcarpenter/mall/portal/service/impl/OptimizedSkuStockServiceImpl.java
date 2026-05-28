package com.mtcarpenter.mall.portal.service.impl;

import com.mtcarpenter.mall.common.domain.SkuStockLockItem;
import com.mtcarpenter.mall.common.domain.SkuStockLockMessage;
import com.mtcarpenter.mall.portal.dao.PortalSkuStockDao;
import com.mtcarpenter.mall.portal.service.OptimizedSkuStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Product-side local transaction for SKU lock_stock persistence.
 */
@Service
public class OptimizedSkuStockServiceImpl implements OptimizedSkuStockService {

    @Autowired
    private PortalSkuStockDao portalSkuStockDao;

    @Override
    public void lockStock(SkuStockLockMessage message) {
        if (message == null || CollectionUtils.isEmpty(message.getItemList())) {
            return;
        }
        for (SkuStockLockItem item : message.getItemList()) {
            int count = portalSkuStockDao.lockStock(item.getSkuId(), item.getQuantity());
            if (count <= 0) {
                throw new IllegalStateException("Persist SKU stock lock failed, orderSn="
                        + message.getOrderSn() + ", skuId=" + item.getSkuId());
            }
        }
    }

    @Override
    public void unlockStock(SkuStockLockMessage message) {
        if (message == null || CollectionUtils.isEmpty(message.getItemList())) {
            return;
        }
        for (SkuStockLockItem item : message.getItemList()) {
            int count = portalSkuStockDao.unlockStock(item.getSkuId(), item.getQuantity());
            if (count <= 0) {
                throw new IllegalStateException("Release SKU stock lock failed, orderSn="
                        + message.getOrderSn() + ", skuId=" + item.getSkuId());
            }
        }
    }
}
