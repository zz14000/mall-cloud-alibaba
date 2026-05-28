package com.mtcarpenter.mall.portal.dao;

import org.apache.ibatis.annotations.Param;

/**
 * Optimized SKU stock persistence DAO.
 */
public interface PortalSkuStockDao {
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int unlockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
