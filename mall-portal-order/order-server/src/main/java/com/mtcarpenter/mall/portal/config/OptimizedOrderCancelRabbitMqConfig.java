package com.mtcarpenter.mall.portal.config;

import com.mtcarpenter.mall.portal.component.OptimizedCancelOrderSender;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Delay queue config for optimized order cancel flow.
 */
@Configuration
public class OptimizedOrderCancelRabbitMqConfig {

    @Bean
    DirectExchange optimizedOrderDirect() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(OptimizedCancelOrderSender.EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    DirectExchange optimizedOrderTtlDirect() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(OptimizedCancelOrderSender.TTL_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue optimizedOrderQueue() {
        return new Queue(OptimizedCancelOrderSender.QUEUE, true);
    }

    @Bean
    public Queue optimizedOrderTtlQueue() {
        return QueueBuilder
                .durable(OptimizedCancelOrderSender.TTL_QUEUE)
                .withArgument("x-dead-letter-exchange", OptimizedCancelOrderSender.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OptimizedCancelOrderSender.ROUTE_KEY)
                .build();
    }

    @Bean
    Binding optimizedOrderBinding(DirectExchange optimizedOrderDirect, Queue optimizedOrderQueue) {
        return BindingBuilder
                .bind(optimizedOrderQueue)
                .to(optimizedOrderDirect)
                .with(OptimizedCancelOrderSender.ROUTE_KEY);
    }

    @Bean
    Binding optimizedOrderTtlBinding(DirectExchange optimizedOrderTtlDirect, Queue optimizedOrderTtlQueue) {
        return BindingBuilder
                .bind(optimizedOrderTtlQueue)
                .to(optimizedOrderTtlDirect)
                .with(OptimizedCancelOrderSender.TTL_ROUTE_KEY);
    }
}
