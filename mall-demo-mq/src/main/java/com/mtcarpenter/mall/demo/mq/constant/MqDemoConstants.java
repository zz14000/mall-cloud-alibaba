package com.mtcarpenter.mall.demo.mq.constant;

/**
 * 集中保存交换机、队列、路由键名称。
 *
 * RabbitMQ 里最容易混淆的是这三个概念：
 *
 * 1. Exchange：交换机，生产者把消息发给它。
 * 2. Queue：队列，消费者从它里面取消息。
 * 3. Routing key：路由键，交换机根据它决定把消息投递到哪个队列。
 *
 * 把名称集中到一个类里，学习时可以顺着名称看完整链路。
 */
public final class MqDemoConstants {

    private MqDemoConstants() {
    }

    /**
     * 普通 direct 交换机：演示“生产者 -> 交换机 -> 队列 -> 消费者”的最基础流程。
     */
    public static final String DIRECT_EXCHANGE = "demo.direct.exchange";

    /**
     * 普通队列：DirectMessageListener 会监听这个队列。
     */
    public static final String DIRECT_QUEUE = "demo.direct.queue";

    /**
     * 普通消息路由键：direct 交换机会用它匹配绑定关系。
     */
    public static final String DIRECT_ROUTING_KEY = "demo.direct";

    /**
     * 故意不存在绑定关系的路由键。
     *
     * 用它发送消息时，交换机能收到消息，但找不到队列，ReturnCallback 会被触发。
     */
    public static final String UNROUTABLE_ROUTING_KEY = "demo.no.queue";

    /**
     * 延迟交换机。
     *
     * 注意：这里没有使用 RabbitMQ delayed-message 插件。
     * 本 demo 用“消息 TTL + 死信交换机”模拟延迟队列，更贴近 mall-portal-order 的订单超时取消方案。
     */
    public static final String ORDER_DELAY_EXCHANGE = "demo.order.delay.exchange";

    /**
     * 延迟队列。
     *
     * 生产者把“订单超时检查消息”先发到这个队列。
     * 消费者不监听它，它只是负责让消息在里面等待一段时间。
     */
    public static final String ORDER_DELAY_QUEUE = "demo.order.delay.queue";

    /**
     * 投递到延迟队列使用的路由键。
     */
    public static final String ORDER_DELAY_ROUTING_KEY = "demo.order.delay";

    /**
     * 死信交换机。
     *
     * 延迟队列里的消息 TTL 到期后，会被 RabbitMQ 重新投递到这个交换机。
     */
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "demo.order.dead.exchange";

    /**
     * 真正被消费者监听的订单取消队列。
     *
     * TTL 到期后的消息最终会来到这里，OrderTimeoutListener 会消费它。
     */
    public static final String ORDER_CANCEL_QUEUE = "demo.order.cancel.queue";

    /**
     * 死信消息从死信交换机路由到订单取消队列时使用的路由键。
     */
    public static final String ORDER_CANCEL_ROUTING_KEY = "demo.order.cancel";

    // ==================== Topic 交换机演示 ====================

    /**
     * Topic 交换机：演示路由键的模式匹配。
     */
    public static final String TOPIC_EXCHANGE = "demo.topic.exchange";

    /**
     * 队列 A：绑定 routing key = "topic.#"，匹配所有以 topic. 开头的路由键。
     */
    public static final String TOPIC_QUEUE_A = "demo.topic.queue.a";

    /**
     * 队列 B：绑定 routing key = "topic.user.*"，只匹配 topic.user. 后面跟一个单词的路由键。
     */
    public static final String TOPIC_QUEUE_B = "demo.topic.queue.b";

    /**
     * 队列 C：绑定 routing key = "topic.order.create"，精确匹配。
     */
    public static final String TOPIC_QUEUE_C = "demo.topic.queue.c";
}
