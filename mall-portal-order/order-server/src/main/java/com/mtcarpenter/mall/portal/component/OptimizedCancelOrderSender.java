package com.mtcarpenter.mall.portal.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Delay message sender for optimized orders.
 */
@Component
public class OptimizedCancelOrderSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizedCancelOrderSender.class);

    public static final String EXCHANGE = "mall.order.optimized.direct";
    public static final String TTL_EXCHANGE = "mall.order.optimized.direct.ttl";
    public static final String QUEUE = "mall.order.optimized.cancel";
    public static final String TTL_QUEUE = "mall.order.optimized.cancel.ttl";
    public static final String ROUTE_KEY = "mall.order.optimized.cancel";
    public static final String TTL_ROUTE_KEY = "mall.order.optimized.cancel.ttl";

    @Autowired
    private AmqpTemplate amqpTemplate;

    public void sendMessage(Long orderId, final long delayTimes) {
        amqpTemplate.convertAndSend(TTL_EXCHANGE, TTL_ROUTE_KEY, orderId, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setExpiration(String.valueOf(delayTimes));
                return message;
            }
        });
        LOGGER.info("send optimized cancel orderId:{}", orderId);
    }
}
