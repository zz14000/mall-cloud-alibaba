package com.mtcarpenter.mall.demo.mq.domain;

/**
 * 模拟订单状态。
 *
 * 这个 demo 不连接数据库，只把订单放在内存 Map 里。
 * 状态设计得很小，是为了突出 MQ 超时取消的核心判断：
 * 延迟消息到期时，只有 WAIT_PAY 的订单才允许被关闭。
 */
public enum DemoOrderStatus {

    /**
     * 待支付：创建订单后还没有付款，超时取消消息只能取消这种状态。
     */
    WAIT_PAY,

    /**
     * 已支付：延迟消息到期后应该跳过，不能误取消。
     */
    PAID,

    /**
     * 已取消：超时取消成功后的状态。
     */
    CANCELED
}
