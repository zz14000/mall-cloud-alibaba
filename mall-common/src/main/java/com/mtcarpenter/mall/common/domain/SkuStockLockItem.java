package com.mtcarpenter.mall.common.domain;

import java.io.Serializable;

/**
 * A single SKU stock lock item.
 */
public class SkuStockLockItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Integer quantity;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
