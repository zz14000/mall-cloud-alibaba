package com.mtcarpenter.mall.demo.mq.domain;

import java.io.Serializable;

/**
 * Demo 使用的消息体。
 *
 * 真实项目里，不同业务通常会设计不同的消息 DTO。
 * 这里为了学习方便，用一个通用消息对象承载普通消息和订单超时消息。
 */
public class MqDemoMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每条消息的唯一 ID。
     *
     * 它也会作为 CorrelationData 的 ID，用来把“发送动作”和“confirm 回调”关联起来。
     */
    private String messageId;

    /**
     * 业务类型，例如 DIRECT_MESSAGE、ORDER_TIMEOUT_CHECK。
     */
    private String messageType;

    /**
     * 业务 ID。
     *
     * 普通消息可以为空；订单超时消息里放 orderId。
     */
    private Long businessId;

    /**
     * 消息内容，学习时主要用来观察日志和 RabbitMQ Management 页面。
     */
    private String content;

    /**
     * 创建时间字符串。
     *
     * 用字符串是为了让 JSON 展示更直观，也避免初学时被日期序列化细节干扰。
     */
    private String createdAt;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "MqDemoMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", businessId=" + businessId +
                ", content='" + content + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
