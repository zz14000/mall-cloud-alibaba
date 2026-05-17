package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nacos 配置管理测试 Controller
 * 
 * 功能说明：
 * 用于测试和验证 Nacos 配置中心的动态刷新功能。
 * 通过该 Controller 可以验证 Nacos 配置是否成功注入到应用中，
 * 以及配置文件的动态刷新是否生效。
 * 
 * 技术说明：
 * - @RefreshScope：Spring Cloud 的注解，用于支持配置的动态刷新
 *   当 Nacos 配置中心的配置发生变化时，被该注解标记的 Bean 会自动刷新配置值
 * - @Value：Spring 的注解，用于注入配置文件中的值
 * - Nacos：阿里巴巴开源的分布式配置中心和服务发现框架
 * 
 * 主要功能：
 * 1. 验证 Nacos 配置是否成功注入
 * 2. 测试配置的动态刷新功能
 * 
 * 使用示例：
 * 1. 获取当前应用的配置信息：GET /config/get
 *    返回：包含应用名称的字符串，验证配置是否成功注入
 *    示例返回：
 *    {
 *      "code": 200,
 *      "message": "操作成功",
 *      "data": "nacos config RefreshScope :mall-admin-pms"
 *    }
 *    说明：返回结果中的 "mall-admin-pms" 是从 Nacos 配置中心注入的应用名称
 *         （来自 spring.application.name 配置项）
 * 
 * 2. 测试配置动态刷新：
 *    步骤 1：访问 GET /config/get，记录返回的应用名称
 *    步骤 2：在 Nacos 配置中心修改 spring.application.name 的值
 *    步骤 3：再次访问 GET /config/get，验证返回的应用名称是否已更新
 *    预期结果：第二次访问返回的应用名称与 Nacos 配置中心的最新值一致
 * 
 * Nacos 配置文件说明：
 * 在 Nacos 配置中心中，每个微服务都有对应的配置文件，例如：
 * - mall-admin-pms-prod.yaml：pms 服务的生产环境配置
 * - mall-admin-pms-dev.yaml：pms 服务的开发环境配置
 * 
 * 配置文件内容示例（mall-admin-pms-prod.yaml）：
 * spring:
 *   application:
 *     name: mall-admin-pms
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/mall_pms?useUnicode=true&characterEncoding=utf-8
 *     username: root
 *     password: root
 * 
 * 注意事项：
 * 1. 该 Controller 仅用于测试和演示，生产环境中建议移除或添加权限控制
 * 2. @RefreshScope 会影响 Bean 的性能，因为它需要使用代理对象
 * 3. 只有被 @RefreshScope 标记的 Bean 中的 @Value 注解才会动态刷新
 * 
 * @author mtcarpenter
 * @github https://github.com/mtcarpenter/mall-cloud-alibaba
 * @desc 微信公众号：山间木匠
 */
@RestController
@RequestMapping("/config")
@RefreshScope
public class ConfigController {

    @Value("${spring.application.name}")
    private String applicationName;

    @RequestMapping("/get")
    public CommonResult get() {
        return CommonResult.success("nacos config RefreshScope :" + applicationName);
    }


}
