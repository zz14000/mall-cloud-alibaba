package com.mtcarpenter.mall.common.domain;

import java.io.Serializable;
import java.util.List;

/**
 * Message used to persist Redis-reserved SKU stock into MySQL.
 */
public class SkuStockLockMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderSn;
    private List<SkuStockLockItem> itemList;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderSn() {
        return orderSn;
    }

    public void setOrderSn(String orderSn) {
        this.orderSn = orderSn;
    }

    public List<SkuStockLockItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<SkuStockLockItem> itemList) {
        this.itemList = itemList;
    }
}
