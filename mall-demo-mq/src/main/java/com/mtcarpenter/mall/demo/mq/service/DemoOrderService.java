package com.mtcarpenter.mall.demo.mq.service;

import com.mtcarpenter.mall.demo.mq.domain.DemoOrder;
import com.mtcarpenter.mall.demo.mq.domain.DemoOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟订单服务。
 *
 * 真实项目会把订单写入数据库，这里为了让 demo 足够轻，只用内存 Map 保存订单。
 * 这也能说明一个重要思想：RabbitMQ 消息只是“触发动作”，最终要不要取消订单，必须查业务状态。
 */
@Service
public class DemoOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoOrderService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 内存订单表。
     *
     * key 是 orderId，value 是订单快照。
     * 项目重启后数据会清空，这符合 demo 的定位。
     */
    private final Map<Long, DemoOrder> orderStore = new ConcurrentHashMap<>();

    private final RabbitMqDemoService rabbitMqDemoService;

    public DemoOrderService(RabbitMqDemoService rabbitMqDemoService) {
        this.rabbitMqDemoService = rabbitMqDemoService;
    }

    /**
     * 创建模拟订单，并发送延迟取消消息。
     *
     * 这个方法对应真实电商里的“下单成功”：
     * 1. 订单状态先是 WAIT_PAY。
     * 2. 发送一条延迟消息。
     * 3. 如果用户一直不付款，延迟消息到期后消费者会关闭订单。
     */
    public synchronized DemoOrder createOrder(Long orderId, int timeoutSeconds) {
        DemoOrder order = new DemoOrder();
        order.setOrderId(orderId);
        order.setStatus(DemoOrderStatus.WAIT_PAY);
        order.setTimeoutSeconds(timeoutSeconds);
        order.setCreateTime(now());
        orderStore.put(orderId, order);

        rabbitMqDemoService.sendOrderTimeoutMessage(orderId, timeoutSeconds);
        LOGGER.info("[创建模拟订单] orderId={}, status={}, timeoutSeconds={}", orderId, order.getStatus(), timeoutSeconds);
        return order;
    }

    /**
     * 模拟支付。
     *
     * 延迟消息并不会因为订单已支付而自动消失。
     * 所以消息到期后，消费者必须查状态：如果已经 PAID，就跳过取消。
     */
    public synchronized DemoOrder payOrder(Long orderId) {
        DemoOrder order = orderStore.get(orderId);
        if (order == null) {
            LOGGER.warn("[模拟支付] 订单不存在, orderId={}", orderId);
            return null;
        }
        if (DemoOrderStatus.WAIT_PAY.equals(order.getStatus())) {
            order.setStatus(DemoOrderStatus.PAID);
            order.setPayTime(now());
            LOGGER.info("[模拟支付] 支付成功, orderId={}", orderId);
        } else {
            LOGGER.info("[模拟支付] 当前状态不允许支付, orderId={}, status={}", orderId, order.getStatus());
        }
        return order;
    }

    /**
     * 延迟消息到期后执行的取消逻辑。
     *
     * 这里必须做状态判断，这是 MQ 消费者最常见的“幂等保护”：
     * - WAIT_PAY：说明还没支付，可以取消。
     * - PAID：说明用户已经付款，不能取消。
     * - CANCELED：说明已经取消过，再收到消息也不能重复扣减库存或重复返券。
     */
    public synchronized DemoOrder cancelOrderIfStillWaitingPay(Long orderId) {
        DemoOrder order = orderStore.get(orderId);
        if (order == null) {
            LOGGER.warn("[超时取消] 订单不存在，直接忽略, orderId={}", orderId);
            return null;
        }
        if (!DemoOrderStatus.WAIT_PAY.equals(order.getStatus())) {
            LOGGER.info("[超时取消] 订单不是待支付状态，跳过取消, orderId={}, status={}", orderId, order.getStatus());
            return order;
        }

        order.setStatus(DemoOrderStatus.CANCELED);
        order.setCancelTime(now());
        LOGGER.info("[超时取消] 订单已取消, orderId={}", orderId);
        return order;
    }

    public List<DemoOrder> listOrders() {
        List<DemoOrder> orders = new ArrayList<>(orderStore.values());
        orders.sort(Comparator.comparing(DemoOrder::getOrderId));
        return orders;
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
