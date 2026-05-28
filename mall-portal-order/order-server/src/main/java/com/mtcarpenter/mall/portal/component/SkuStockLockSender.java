package com.mtcarpenter.mall.portal.component;

import com.mtcarpenter.mall.common.constant.ProductStockQueueConstants;
import com.mtcarpenter.mall.common.domain.SkuStockLockMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sends SKU stock lock messages after the order transaction commits.
 */
@Component
public class SkuStockLockSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(SkuStockLockSender.class);

    @Autowired
    private AmqpTemplate amqpTemplate;

    public void sendMessage(SkuStockLockMessage message) {
        amqpTemplate.convertAndSend(
                ProductStockQueueConstants.STOCK_EXCHANGE,
                ProductStockQueueConstants.STOCK_LOCK_ROUTE_KEY,
                message);
        LOGGER.info("send sku stock lock message, orderSn:{}", message.getOrderSn());
    }
}
