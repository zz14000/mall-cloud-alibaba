# mall-demo-mq

这是一个独立的 RabbitMQ 学习 demo，不依赖商城主业务模块。

它演示四件事：

1. 普通消息：生产者 -> direct 交换机 -> 队列 -> 消费者。
2. 生产者 confirm：确认消息是否到达交换机。
3. return 回调：消息到达交换机但无法路由到队列时如何发现。
4. TTL + 死信队列：模拟订单超时未支付自动取消。

## 启动前准备

本地先启动 RabbitMQ，默认连接配置在 `src/main/resources/application.yml`：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

如果你使用 Docker，可以参考：

```bash
docker run -d --name rabbitmq-demo \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management
```

RabbitMQ 管理页面：

```text
http://localhost:15672
账号：guest
密码：guest
```

## 启动 demo

在项目根目录执行：

```bash
mvn -pl mall-demo-mq spring-boot:run
```

启动后访问：

```text
http://localhost:8088/demo-mq
```

这个地址会跳转到一个可点击页面。你也可以直接访问：

```text
http://localhost:8088/demo-mq-page.html
```

接口清单在：

```text
http://localhost:8088/demo-mq/apis
```

## 普通消息流程

调用：

```bash
curl -X POST "http://localhost:8088/demo-mq/messages/direct?content=hello"
```

流程：

```text
RabbitMqDemoService
  -> demo.direct.exchange
  -> routing key demo.direct
  -> demo.direct.queue
  -> RabbitMqDemoListener.handleDirectMessage
  -> basicAck
```

你会在日志里看到：

```text
[发送普通消息]
[生产者确认] 消息已到达交换机
[普通消费者] 收到消息
[普通消费者] 已 ack
```

## 无法路由消息

调用：

```bash
curl -X POST "http://localhost:8088/demo-mq/messages/unroutable?content=no-queue"
```

这条消息会发到存在的交换机 `demo.direct.exchange`，但使用没有绑定队列的 routing key `demo.no.queue`。

你会看到：

```text
[生产者确认] 消息已到达交换机
[消息退回] 交换机收到消息但无法路由到队列
```

这能帮助区分：

- confirm：消息是否到交换机。
- return：交换机是否能把消息路由到队列。

## TTL + 死信模拟订单超时取消

创建一个 10 秒后超时的订单：

```bash
curl -X POST "http://localhost:8088/demo-mq/orders/1001?timeoutSeconds=10"
```

不付款，等待 10 秒后，订单会变为 `CANCELED`：

```bash
curl "http://localhost:8088/demo-mq/orders"
```

完整链路：

```text
创建订单 WAIT_PAY
  -> 发送延迟消息到 demo.order.delay.exchange
  -> 消息进入 demo.order.delay.queue
  -> TTL 到期
  -> RabbitMQ 把消息作为死信转发到 demo.order.dead.exchange
  -> routing key demo.order.cancel
  -> 消息进入 demo.order.cancel.queue
  -> RabbitMqDemoListener.handleOrderTimeoutMessage
  -> 查询订单状态
  -> 仍是 WAIT_PAY 才取消
  -> basicAck
```

## 已支付订单不会被误取消

创建订单：

```bash
curl -X POST "http://localhost:8088/demo-mq/orders/1002?timeoutSeconds=10"
```

立刻付款：

```bash
curl -X POST "http://localhost:8088/demo-mq/orders/1002/pay"
```

等待 10 秒后再查看：

```bash
curl "http://localhost:8088/demo-mq/orders"
```

订单会保持 `PAID`。

这是订单超时取消最重要的点：延迟消息只是提醒系统“该检查了”，消费者不能无脑取消，必须再次查询订单状态。
