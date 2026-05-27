package com.mtcarpenter.mall.demo.mq.config;

import com.mtcarpenter.mall.demo.mq.constant.MqDemoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础设施配置。
 *
 * 这个类做两类事情：
 *
 * 1. 声明 RabbitMQ 里的资源：交换机、队列、绑定关系。
 * 2. 配置 RabbitTemplate：让生产者发送 JSON 消息，并能收到 confirm/return 回调。
 *
 * Spring Boot 启动时会根据这些 Bean 自动向 RabbitMQ 声明资源。
 * 如果资源已经存在且参数一致，不会重复创建；如果参数不一致，RabbitMQ 会拒绝声明。
 */
@EnableRabbit
@Configuration
public class RabbitMqDemoConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDemoConfig.class);

    /**
     * JSON 消息转换器。
     *
     * 默认情况下 RabbitTemplate 可能使用 Java 序列化，消息体不直观。
     * 使用 JSON 后，可以在 RabbitMQ Management 页面直接看懂消息内容。
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 是生产者发送消息的核心工具。
     *
     * ConfirmCallback：
     * - ack=true：消息已经到达交换机。
     * - ack=false：消息没有到达交换机，可能是交换机不存在、连接异常等。
     *
     * ReturnCallback：
     * - 消息到达了交换机，但交换机根据 routing key 找不到任何队列时触发。
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // mandatory=true 才能让“无法路由到队列”的消息回到 ReturnCallback。
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String correlationId = correlationData == null ? "null" : correlationData.getId();
            if (ack) {
                LOGGER.info("[生产者确认] 消息已到达交换机, correlationId={}", correlationId);
            } else {
                LOGGER.warn("[生产者确认] 消息没有到达交换机, correlationId={}, cause={}", correlationId, cause);
            }
        });

        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) ->
                LOGGER.warn("[消息退回] 交换机收到消息但无法路由到队列, exchange={}, routingKey={}, replyCode={}, replyText={}, body={}",
                        exchange, routingKey, replyCode, replyText, new String(message.getBody())));

        return rabbitTemplate;
    }

    /**
     * 普通 direct 交换机。
     *
     * direct 类型的规则很简单：
     * 发送消息时的 routing key 必须和绑定时的 routing key 完全相等，消息才会进入对应队列。
     */
    @Bean
    public DirectExchange demoDirectExchange() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(MqDemoConstants.DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 普通队列。
     *
     * durable=true 表示队列持久化，RabbitMQ 重启后队列还在。
     * 这里只是队列持久化；如果消息也要持久化，发送消息时还要让消息 deliveryMode=PERSISTENT。
     * Spring AMQP 默认会把普通对象消息作为持久消息发送。
     */
    @Bean
    public Queue demoDirectQueue() {
        return QueueBuilder
                .durable(MqDemoConstants.DIRECT_QUEUE)
                .build();
    }

    /**
     * 把普通队列绑定到普通 direct 交换机。
     *
     * 绑定关系可以理解成：
     * demo.direct.exchange 收到 routing key 为 demo.direct 的消息时，把它投递给 demo.direct.queue。
     */
    @Bean
    public Binding demoDirectBinding(DirectExchange demoDirectExchange, Queue demoDirectQueue) {
        return BindingBuilder
                .bind(demoDirectQueue)
                .to(demoDirectExchange)
                .with(MqDemoConstants.DIRECT_ROUTING_KEY);
    }

    /**
     * 延迟交换机：只负责把消息投递到“延迟队列”。
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(MqDemoConstants.ORDER_DELAY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信交换机：只负责接收“过期后的消息”。
     */
    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return (DirectExchange) ExchangeBuilder
                .directExchange(MqDemoConstants.ORDER_DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 延迟队列。
     *
     * 这个队列最关键的两个参数：
     *
     * x-dead-letter-exchange：
     * - 消息过期、被拒绝且不重新入队、队列满等情况发生时，消息会变成“死信”。
     * - 这里指定死信要转发到哪个交换机。
     *
     * x-dead-letter-routing-key：
     * - 死信转发时使用哪个 routing key。
     *
     * 本 demo 的流程是：
     * 1. 生产者发送消息到 demo.order.delay.exchange。
     * 2. 消息进入 demo.order.delay.queue。
     * 3. 消息 TTL 到期。
     * 4. RabbitMQ 把消息作为死信转发到 demo.order.dead.exchange。
     * 5. 死信交换机根据 demo.order.cancel 路由键，把消息投递到 demo.order.cancel.queue。
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder
                .durable(MqDemoConstants.ORDER_DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", MqDemoConstants.ORDER_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqDemoConstants.ORDER_CANCEL_ROUTING_KEY)
                .build();
    }

    /**
     * 把延迟队列绑定到延迟交换机。
     */
    @Bean
    public Binding orderDelayBinding(DirectExchange orderDelayExchange, Queue orderDelayQueue) {
        return BindingBuilder
                .bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with(MqDemoConstants.ORDER_DELAY_ROUTING_KEY);
    }

    /**
     * 订单取消队列。
     *
     * 这才是真正被消费者监听的队列。
     * 延迟队列只是“等待室”，订单取消队列才是“处理室”。
     */
    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder
                .durable(MqDemoConstants.ORDER_CANCEL_QUEUE)
                .build();
    }

    /**
     * 把订单取消队列绑定到死信交换机。
     */
    @Bean
    public Binding orderCancelBinding(DirectExchange orderDeadLetterExchange, Queue orderCancelQueue) {
        return BindingBuilder
                .bind(orderCancelQueue)
                .to(orderDeadLetterExchange)
                .with(MqDemoConstants.ORDER_CANCEL_ROUTING_KEY);
    }

    // ==================== Topic 交换机配置 ====================

    /**
     * Topic 交换机。
     *
     * Topic 交换机的特点是支持模式匹配：
     * - * 匹配一个单词
     * - # 匹配零个或多个单词
     *
     * 例如：
     * - 绑定 "topic.#" 可以匹配：topic.user.create、topic.order.create、topic.any.thing.here
     * - 绑定 "topic.user.*" 只能匹配：topic.user.create、topic.user.delete（但 topic.user.a.b 不匹配）
     * - 绑定 "topic.order.create" 只能精确匹配：topic.order.create
     */
    @Bean
    public TopicExchange topicExchange() {
        return (TopicExchange) ExchangeBuilder
                .topicExchange(MqDemoConstants.TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Topic 队列 A。
     */
    @Bean
    public Queue topicQueueA() {
        return QueueBuilder
                .durable(MqDemoConstants.TOPIC_QUEUE_A)
                .build();
    }

    /**
     * Topic 队列 B。
     */
    @Bean
    public Queue topicQueueB() {
        return QueueBuilder
                .durable(MqDemoConstants.TOPIC_QUEUE_B)
                .build();
    }

    /**
     * Topic 队列 C。
     */
    @Bean
    public Queue topicQueueC() {
        return QueueBuilder
                .durable(MqDemoConstants.TOPIC_QUEUE_C)
                .build();
    }

    /**
     * 队列 A 绑定到 Topic 交换机，使用 "topic.#"。
     *
     * 这意味着：所有以 "topic." 开头的路由键都会被投递到这个队列。
     * 例如：topic.user.create、topic.order.create、topic.any.thing
     */
    @Bean
    public Binding topicBindingA(TopicExchange topicExchange, Queue topicQueueA) {
        return BindingBuilder
                .bind(topicQueueA)
                .to(topicExchange)
                .with("topic.#");
    }

    /**
     * 队列 B 绑定到 Topic 交换机，使用 "topic.user.*"。
     *
     * 这意味着：只有 "topic.user." 后面跟一个单词的路由键才会被投递到这个队列。
     * 例如：topic.user.create、topic.user.delete ✅
     *      topic.user.a.b ❌（两个单词）
     *      topic.order.create ❌（不是 topic.user 开头）
     */
    @Bean
    public Binding topicBindingB(TopicExchange topicExchange, Queue topicQueueB) {
        return BindingBuilder
                .bind(topicQueueB)
                .to(topicExchange)
                .with("topic.user.*");
    }

    /**
     * 队列 C 绑定到 Topic 交换机，使用 "topic.order.create"。
     *
     * 这意味着：只有精确匹配 "topic.order.create" 的路由键才会被投递到这个队列。
     * 这其实和 Direct 交换机的行为一样。
     */
    @Bean
    public Binding topicBindingC(TopicExchange topicExchange, Queue topicQueueC) {
        return BindingBuilder
                .bind(topicQueueC)
                .to(topicExchange)
                .with(MqDemoConstants.TOPIC_QUEUE_C);
    }
}
