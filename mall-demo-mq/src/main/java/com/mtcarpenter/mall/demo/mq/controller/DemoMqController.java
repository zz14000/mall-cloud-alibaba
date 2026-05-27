package com.mtcarpenter.mall.demo.mq.controller;

import com.mtcarpenter.mall.demo.mq.constant.MqDemoConstants;
import com.mtcarpenter.mall.demo.mq.domain.DemoOrder;
import com.mtcarpenter.mall.demo.mq.domain.MqDemoMessage;
import com.mtcarpenter.mall.demo.mq.service.DemoOrderService;
import com.mtcarpenter.mall.demo.mq.service.RabbitMqDemoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RabbitMQ demo 入口。
 *
 * 这些接口不是业务接口，只是为了方便你手动触发不同 MQ 场景。
 * 启动项目后，可以用浏览器、Postman 或 curl 调用它们，然后观察控制台日志。
 */
@Controller
@RequestMapping("/demo-mq")
public class DemoMqController {

    private final RabbitMqDemoService rabbitMqDemoService;

    private final DemoOrderService demoOrderService;

    public DemoMqController(RabbitMqDemoService rabbitMqDemoService, DemoOrderService demoOrderService) {
        this.rabbitMqDemoService = rabbitMqDemoService;
        this.demoOrderService = demoOrderService;
    }

    /**
     * 浏览器访问 /demo-mq 时，直接跳转到可点击的学习页面。
     *
     * 如果返回 JSON，初学时会不知道下一步该点哪里。
     * 所以页面负责交互，/apis 负责查看接口清单。
     */
    @GetMapping
    public String index() {
        return "redirect:/demo-mq-page.html";
    }

    /**
     * 查看 demo 提供的接口和核心 RabbitMQ 资源名称。
     */
    @GetMapping("/apis")
    @ResponseBody
    public Map<String, Object> apis() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("description", "RabbitMQ learning demo");
        result.put("tryEndpoints", Arrays.asList(
                "GET  /demo-mq-page.html",
                "POST /demo-mq/messages/direct?content=hello",
                "POST /demo-mq/messages/unroutable?content=hello",
                "POST /demo-mq/orders/1001?timeoutSeconds=10",
                "POST /demo-mq/orders/1001/pay",
                "GET  /demo-mq/orders"
        ));
        result.put("howToUse", "This URL only shows the API list. Open /demo-mq-page.html or use Postman/curl to call POST endpoints.");
        result.put("directFlow", MqDemoConstants.DIRECT_EXCHANGE + " --" + MqDemoConstants.DIRECT_ROUTING_KEY + "--> " + MqDemoConstants.DIRECT_QUEUE);
        result.put("ttlDeadLetterFlow",
                MqDemoConstants.ORDER_DELAY_EXCHANGE + " --" + MqDemoConstants.ORDER_DELAY_ROUTING_KEY + "--> " +
                        MqDemoConstants.ORDER_DELAY_QUEUE + " --TTL expired/dead letter--> " +
                        MqDemoConstants.ORDER_DEAD_LETTER_EXCHANGE + " --" + MqDemoConstants.ORDER_CANCEL_ROUTING_KEY + "--> " +
                        MqDemoConstants.ORDER_CANCEL_QUEUE);
        return result;
    }

    /**
     * 发送普通消息。
     *
     * 看点：
     * 1. 控制台会打印“发送普通消息”。
     * 2. ConfirmCallback 会打印“消息已到达交换机”。
     * 3. 普通消费者会打印“收到消息”和“已 ack”。
     */
    @PostMapping("/messages/direct")
    @ResponseBody
    public MqDemoMessage sendDirectMessage(@RequestParam(defaultValue = "hello rabbitmq") String content) {
        return rabbitMqDemoService.sendDirectMessage(content);
    }

    /**
     * 发送无法路由的消息。
     *
     * 看点：
     * 1. ConfirmCallback 通常仍然 ack=true，因为交换机存在。
     * 2. ReturnCallback 会打印“无法路由到队列”。
     * 3. 消费者不会收到消息，因为它没有进入任何队列。
     */
    @PostMapping("/messages/unroutable")
    @ResponseBody
    public MqDemoMessage sendUnroutableMessage(@RequestParam(defaultValue = "this message has no queue") String content) {
        return rabbitMqDemoService.sendUnroutableMessage(content);
    }

    /**
     * 创建模拟订单，并发送 TTL + 死信延迟消息。
     *
     * 例子：
     * POST /demo-mq/orders/1001?timeoutSeconds=10
     *
     * 10 秒内不调用付款接口，订单会被超时消费者改成 CANCELED。
     */
    @PostMapping("/orders/{orderId}")
    @ResponseBody
    public DemoOrder createOrder(@PathVariable Long orderId,
                                 @RequestParam(defaultValue = "10") Integer timeoutSeconds) {
        return demoOrderService.createOrder(orderId, timeoutSeconds);
    }

    /**
     * 模拟付款。
     *
     * 例子：
     * 先创建订单：POST /demo-mq/orders/1002?timeoutSeconds=10
     * 再立刻付款：POST /demo-mq/orders/1002/pay
     *
     * 等 10 秒后，延迟消息仍然会到达消费者。
     * 但消费者查到订单已经 PAID，会跳过取消。
     */
    @PostMapping("/orders/{orderId}/pay")
    @ResponseBody
    public DemoOrder payOrder(@PathVariable Long orderId) {
        return demoOrderService.payOrder(orderId);
    }

    /**
     * 查看当前内存订单。
     */
    @GetMapping("/orders")
    @ResponseBody
    public Object listOrders() {
        return demoOrderService.listOrders();
    }

    // ==================== Topic 交换机演示接口 ====================

    /**
     * 发送 Topic 消息。
     *
     * 例子：
     * POST /demo-mq/messages/topic?routingKey=topic.user.create&content=用户注册
     * POST /demo-mq/messages/topic?routingKey=topic.order.create&content=订单创建
     * POST /demo-mq/messages/topic?routingKey=topic.any.thing.here&content=任意消息
     *
     * 观察：
     * - routingKey=topic.user.create -> 队列 A 和队列 B 都会收到
     * - routingKey=topic.order.create -> 队列 A 和队列 C 都会收到
     * - routingKey=topic.any.thing.here -> 只有队列 A 会收到
     */
    @PostMapping("/messages/topic")
    @ResponseBody
    public MqDemoMessage sendTopicMessage(
            @RequestParam(defaultValue = "topic.user.create") String routingKey,
            @RequestParam(defaultValue = "topic message") String content) {
        return rabbitMqDemoService.sendTopicMessage(routingKey, content);
    }
}
