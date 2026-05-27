package com.mtcarpenter.mall.demo.mq.service;

import com.mtcarpenter.mall.demo.mq.constant.MqDemoConstants;
import com.mtcarpenter.mall.demo.mq.domain.MqDemoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 生产者服务。
 *
 * RabbitMQ 发送消息时，一般要明确三个信息：
 * 1. 发到哪个 exchange。
 * 2. 使用哪个 routing key。
 * 3. 消息体是什么。
 *
 * 发送完成不代表消费者已经处理完成。
 * 如果开启 publisher confirm，只能确认消息是否到达交换机。
 */
@Service
public class RabbitMqDemoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDemoService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqDemoService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送一条普通 direct 消息。
     *
     * 正常流程：
     * 1. 生产者发送消息到 demo.direct.exchange。
     * 2. 交换机根据 routing key=demo.direct 找到绑定队列 demo.direct.queue。
     * 3. DirectMessageListener 从队列里消费消息。
     * 4. 消费者业务处理成功后 basicAck。
     */
    public MqDemoMessage sendDirectMessage(String content) {
        MqDemoMessage message = buildMessage("DIRECT_MESSAGE", null, content);
        // 关联消息 ID，方便确认消息是否到达交换机。
        //当消息发送到 RabbitMQ 的交换机后，RabbitMQ 会回调一个 ConfirmCallback
        //CorrelationData 就是这个回调中的"关联凭证"，让你知道"哪条消息"被确认了
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(
                MqDemoConstants.DIRECT_EXCHANGE,
                MqDemoConstants.DIRECT_ROUTING_KEY,
                message,
                correlationData);
        LOGGER.info("[发送普通消息] exchange={}, routingKey={}, message={}",
                MqDemoConstants.DIRECT_EXCHANGE, MqDemoConstants.DIRECT_ROUTING_KEY, message);
        return message;
    }

    /**
     * 发送一条故意无法路由的消息。
     *
     * 这条消息能到达 demo.direct.exchange，所以 ConfirmCallback 通常是 ack=true。
     * 但是 routing key 没有任何队列绑定，所以 ReturnCallback 会被触发。
     *
     * 这个接口用来区分两个概念：
     * - confirm 关注“到没到交换机”。
     * - return 关注“交换机能不能把消息投递到队列”。
     */
    public MqDemoMessage sendUnroutableMessage(String content) {
        MqDemoMessage message = buildMessage("UNROUTABLE_MESSAGE", null, content);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(
                MqDemoConstants.DIRECT_EXCHANGE,
                MqDemoConstants.UNROUTABLE_ROUTING_KEY,
                message,
                correlationData);
        LOGGER.info("[发送无法路由消息] exchange={}, routingKey={}, message={}",
                MqDemoConstants.DIRECT_EXCHANGE, MqDemoConstants.UNROUTABLE_ROUTING_KEY, message);
        return message;
    }

    /**
     * 发送“订单超时检查”延迟消息。
     *
     * 这里使用的是消息级 TTL：
     * - 每条消息通过 expiration 设置自己的过期时间。
     * - 过期后，消息从延迟队列转发到死信交换机。
     *
     * 初学时要记住：
     * TTL 队列本身不是最终消费队列，它只是让消息“等一会儿”。
     */
    public MqDemoMessage sendOrderTimeoutMessage(Long orderId, int delaySeconds) {
        MqDemoMessage message = buildMessage(
                "ORDER_TIMEOUT_CHECK",
                orderId,
                "订单 " + orderId + " 将在 " + delaySeconds + " 秒后检查是否超时未支付");

        long delayMillis = TimeUnit.SECONDS.toMillis(delaySeconds);
        MessagePostProcessor ttlProcessor = rabbitMessage -> {
            // expiration 的单位是毫秒，但类型是字符串，这是 RabbitMQ 协议里的要求。
            rabbitMessage.getMessageProperties().setExpiration(String.valueOf(delayMillis));

            // 自定义 header 不是必须的，只是方便在管理页面和日志里观察。
            rabbitMessage.getMessageProperties().setHeader("x-demo-delay-seconds", delaySeconds);
            return rabbitMessage;
        };

        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(
                MqDemoConstants.ORDER_DELAY_EXCHANGE,
                MqDemoConstants.ORDER_DELAY_ROUTING_KEY,
                message,
                ttlProcessor,
                correlationData);

        LOGGER.info("[发送订单延迟消息] exchange={}, routingKey={}, delaySeconds={}, message={}",
                MqDemoConstants.ORDER_DELAY_EXCHANGE, MqDemoConstants.ORDER_DELAY_ROUTING_KEY, delaySeconds, message);
        return message;
    }

    /**
     * 发送一条 Topic 消息。
     *
     * Topic 交换机支持模式匹配：
     * - * 匹配一个单词
     * - # 匹配零个或多个单词
     *
     * 本例演示不同路由键如何被不同的队列接收：
     * - routingKey="topic.user.create" -> 队列 A(topic.#) 和队列 B(topic.user.*) 都会收到
     * - routingKey="topic.order.create" -> 队列 A(topic.#) 和队列 C(topic.order.create) 都会收到
     * - routingKey="topic.any.thing.here" -> 只有队列 A(topic.#) 会收到
     */
    public MqDemoMessage sendTopicMessage(String routingKey, String content) {
        MqDemoMessage message = buildMessage("TOPIC_MESSAGE", null, content);
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        rabbitTemplate.convertAndSend(
                MqDemoConstants.TOPIC_EXCHANGE,
                routingKey,
                message,
                correlationData);
        LOGGER.info("[发送 Topic 消息] exchange={}, routingKey={}, message={}",
                MqDemoConstants.TOPIC_EXCHANGE, routingKey, message);
        return message;
    }

    private MqDemoMessage buildMessage(String messageType, Long businessId, String content) {
        MqDemoMessage message = new MqDemoMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType(messageType);
        message.setBusinessId(businessId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return message;
    }
}
