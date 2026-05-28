package com.mtcarpenter.mall.portal.component;

import com.mtcarpenter.mall.portal.service.OmsPortalOrderOptimizedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles timeout cancellation for optimized orders.
 */
@Component
@RabbitListener(queues = OptimizedCancelOrderSender.QUEUE)
public class OptimizedCancelOrderReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizedCancelOrderReceiver.class);

    @Autowired
    private OmsPortalOrderOptimizedService optimizedOrderService;

    @RabbitHandler
    public void handle(Long orderId) {
        optimizedOrderService.cancelOrder(orderId);
        LOGGER.info("process optimized cancel orderId:{}", orderId);
    }
}
