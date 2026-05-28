package com.mtcarpenter.mall.portal.config;

import com.mtcarpenter.mall.common.constant.ProductStockQueueConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ config for optimized async SKU stock lock persistence.
 */
@Configuration
public class OptimizedStockRabbitMqConfig {

    @Bean
    DirectExchange productStockDirect() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(ProductStockQueueConstants.STOCK_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue productStockLockQueue() {
        return new Queue(ProductStockQueueConstants.STOCK_LOCK_QUEUE, true);
    }

    @Bean
    Binding productStockLockBinding(DirectExchange productStockDirect, Queue productStockLockQueue) {
        return BindingBuilder
                .bind(productStockLockQueue)
                .to(productStockDirect)
                .with(ProductStockQueueConstants.STOCK_LOCK_ROUTE_KEY);
    }
}
