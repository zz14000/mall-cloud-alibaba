package com.mtcarpenter.mall.demo.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 学习 demo 启动类。
 *
 * 这个模块只演示 MQ，不依赖订单、商品、会员等主项目业务模块。
 * 启动后可以通过 DemoMqController 里的 REST 接口发送消息、创建模拟订单、模拟付款。
 */
@SpringBootApplication
public class MallDemoMqApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallDemoMqApplication.class, args);
    }
}
