package com.mtcarpenter.mall.demo.mq.component;

import com.mtcarpenter.mall.demo.mq.constant.MqDemoConstants;
import com.mtcarpenter.mall.demo.mq.domain.MqDemoMessage;
import com.mtcarpenter.mall.demo.mq.service.DemoOrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费者监听器。
 *
 * @RabbitListener 会启动一个后台消费者线程，不需要我们手动写 while 循环去拉消息。
 *
 * 本 demo 在 application.yml 里配置了手动 ack，所以每个监听方法都要在处理完成后调用：
 * - basicAck：处理成功，RabbitMQ 可以删除消息。
 * - basicNack/basicReject：处理失败，可以选择重新入队，或者丢弃/进入死信队列。
 */
@Component
public class RabbitMqDemoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDemoListener.class);

    private final DemoOrderService demoOrderService;

    public RabbitMqDemoListener(DemoOrderService demoOrderService) {
        this.demoOrderService = demoOrderService;
    }

    /**
     * 普通消息消费者。
     *
     * 它监听 demo.direct.queue。
     * 只要生产者发送到 demo.direct.exchange，并且 routing key 是 demo.direct，消息就会到这个方法。
     */
    @RabbitListener(queues = MqDemoConstants.DIRECT_QUEUE)
    public void handleDirectMessage(MqDemoMessage payload, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            LOGGER.info("[普通消费者] 收到消息 payload={}, deliveryTag={}", payload, deliveryTag);

            // 这里可以写真实业务逻辑，例如保存数据库、调用接口、刷新缓存等。
            // demo 只打印日志，然后确认消息处理成功。

            // multiple=false 表示只确认当前这一条消息。
            channel.basicAck(deliveryTag, false);
            LOGGER.info("[普通消费者] 已 ack，RabbitMQ 会从队列删除这条消息, messageId={}", payload.getMessageId());
        } catch (Exception ex) {
            LOGGER.error("[普通消费者] 处理失败，拒绝消息且不重新入队, payload={}", payload, ex);

            // requeue=false 表示不重新放回当前队列。
            // 如果当前队列配置了死信交换机，消息会进入死信交换机；本普通队列没有配置，所以会被丢弃。
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 订单超时消费者。
     *
     * 它监听的是 demo.order.cancel.queue，而不是 demo.order.delay.queue。
     * 因为消息先在 delay 队列里等待 TTL，到期后才会通过死信交换机转到 cancel 队列。
     */
    @RabbitListener(queues = MqDemoConstants.ORDER_CANCEL_QUEUE)
    public void handleOrderTimeoutMessage(MqDemoMessage payload, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            LOGGER.info("[订单超时消费者] 收到死信转发后的消息 payload={}, deliveryTag={}", payload, deliveryTag);

            // 延迟消息只是提醒“该检查订单了”，不能无脑取消。
            // 因为用户可能已经付款，所以必须再次查询业务状态。
            demoOrderService.cancelOrderIfStillWaitingPay(payload.getBusinessId());

            channel.basicAck(deliveryTag, false);
            LOGGER.info("[订单超时消费者] 已 ack，超时检查流程结束, orderId={}", payload.getBusinessId());
        } catch (Exception ex) {
            LOGGER.error("[订单超时消费者] 处理失败，拒绝消息且不重新入队, payload={}", payload, ex);

            // 学习 demo 中选择不重新入队，避免异常消息无限循环。
            // 生产环境通常会给最终消费队列再配置一个错误死信队列，方便人工排查。
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Topic 队列 A 消费者。
     *
     * 监听 demo.topic.queue.a，绑定模式为 "topic.#"。
     * 所有以 "topic." 开头的路由键都会被投递到这个队列。
     */
    @RabbitListener(queues = MqDemoConstants.TOPIC_QUEUE_A)
    public void handleTopicMessageA(MqDemoMessage payload, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            String routingKey = rawMessage.getMessageProperties().getReceivedRoutingKey();
            LOGGER.info("[Topic 队列 A 消费者] 收到消息 routingKey={}, payload={}, deliveryTag={}",
                    routingKey, payload, deliveryTag);

            channel.basicAck(deliveryTag, false);
            LOGGER.info("[Topic 队列 A 消费者] 已 ack, messageId={}", payload.getMessageId());
        } catch (Exception ex) {
            LOGGER.error("[Topic 队列 A 消费者] 处理失败，拒绝消息, payload={}", payload, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Topic 队列 B 消费者。
     *
     * 监听 demo.topic.queue.b，绑定模式为 "topic.user.*"。
     * 只匹配 "topic.user." 后面跟一个单词的路由键。
     */
    @RabbitListener(queues = MqDemoConstants.TOPIC_QUEUE_B)
    public void handleTopicMessageB(MqDemoMessage payload, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            String routingKey = rawMessage.getMessageProperties().getReceivedRoutingKey();
            LOGGER.info("[Topic 队列 B 消费者] 收到消息 routingKey={}, payload={}, deliveryTag={}",
                    routingKey, payload, deliveryTag);

            channel.basicAck(deliveryTag, false);
            LOGGER.info("[Topic 队列 B 消费者] 已 ack, messageId={}", payload.getMessageId());
        } catch (Exception ex) {
            LOGGER.error("[Topic 队列 B 消费者] 处理失败，拒绝消息, payload={}", payload, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Topic 队列 C 消费者。
     *
     * 监听 demo.topic.queue.c，绑定模式为 "topic.order.create"（精确匹配）。
     */
    @RabbitListener(queues = MqDemoConstants.TOPIC_QUEUE_C)
    public void handleTopicMessageC(MqDemoMessage payload, Message rawMessage, Channel channel) throws IOException {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            String routingKey = rawMessage.getMessageProperties().getReceivedRoutingKey();
            LOGGER.info("[Topic 队列 C 消费者] 收到消息 routingKey={}, payload={}, deliveryTag={}",
                    routingKey, payload, deliveryTag);

            channel.basicAck(deliveryTag, false);
            LOGGER.info("[Topic 队列 C 消费者] 已 ack, messageId={}", payload.getMessageId());
        } catch (Exception ex) {
            LOGGER.error("[Topic 队列 C 消费者] 处理失败，拒绝消息, payload={}", payload, ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
