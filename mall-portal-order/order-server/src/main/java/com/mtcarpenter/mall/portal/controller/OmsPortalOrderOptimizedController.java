package com.mtcarpenter.mall.portal.controller;

import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.portal.domain.OrderParam;
import com.mtcarpenter.mall.portal.service.OmsPortalOrderOptimizedService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Optimized order APIs.
 */
@Controller
@Api(tags = "OmsPortalOrderOptimizedController", description = "Optimized order APIs")
@RequestMapping("/order/optimized")
public class OmsPortalOrderOptimizedController {

    @Autowired
    private OmsPortalOrderOptimizedService optimizedOrderService;

    @ApiOperation("Generate order with Redis SKU stock reservation")
    @RequestMapping(value = "/generateOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult generateOrder(@RequestBody OrderParam orderParam) {
        Map<String, Object> result = optimizedOrderService.generateOrder(orderParam);
        return CommonResult.success(result, "下单成功");
    }

    @ApiOperation("Optimized order pay success callback")
    @RequestMapping(value = "/paySuccess", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult paySuccess(@RequestParam Long orderId, @RequestParam Integer payType) {
        Integer count = optimizedOrderService.paySuccess(orderId, payType);
        return CommonResult.success(count, "支付成功");
    }

    @ApiOperation("Optimized order cancel")
    @RequestMapping(value = "/cancelOrder", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult cancelOrder(@RequestParam Long orderId) {
        optimizedOrderService.cancelOrder(orderId);
        return CommonResult.success(null);
    }
}
