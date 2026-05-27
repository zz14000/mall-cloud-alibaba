package com.mtcarpenter.mall.demo.mq.domain;

/**
 * 内存里的模拟订单。
 *
 * 它不是主项目订单表，只是为了演示：
 * 1. 创建订单后发送一条延迟消息。
 * 2. 用户可能在延迟消息到期前付款。
 * 3. 延迟消息到期后，消费者必须再次检查订单状态。
 */
public class DemoOrder {

    private Long orderId;

    private DemoOrderStatus status;

    private Integer timeoutSeconds;

    private String createTime;

    private String payTime;

    private String cancelTime;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public DemoOrderStatus getStatus() {
        return status;
    }

    public void setStatus(DemoOrderStatus status) {
        this.status = status;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(String cancelTime) {
        this.cancelTime = cancelTime;
    }
}
