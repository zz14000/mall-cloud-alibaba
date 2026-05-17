package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.model.PmsSkuStock;
import com.mtcarpenter.mall.service.PmsSkuStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SKU 商品库存管理 Controller
 * 
 * 功能说明：
 * 用于管理商品的 SKU（Stock Keeping Unit，库存量单位）库存信息。
 * SKU 是商品的最小库存单位，通常由商品的不同规格组合而成。
 * 
 * SKU 概念说明：
 * - 商品：华为 Mate 60 Pro
 * - SKU1：华为 Mate 60 Pro 黑色 256GB，价格 6999 元，库存 100 件
 * - SKU2：华为 Mate 60 Pro 白色 256GB，价格 6999 元，库存 50 件
 * - SKU3：华为 Mate 60 Pro 黑色 512GB，价格 7999 元，库存 80 件
 * 
 * 主要功能：
 * 1. 根据商品 ID 查询 SKU 库存列表（支持按 SKU 编码模糊搜索）
 * 2. 批量更新商品的 SKU 库存信息
 * 
 * 使用示例：
 * 1. 根据商品编号查询 SKU 库存：GET /sku/{pid}?keyword=黑色
 *    路径参数：pid - 商品 ID
 *    参数：keyword - SKU 编码或规格的模糊搜索关键词（可选）
 *    示例：
 *      - GET /sku/1 - 查询商品 ID 为 1 的所有 SKU 库存
 *      - GET /sku/1?keyword=黑色 - 查询商品 ID 为 1 且规格包含"黑色"的 SKU
 *    返回：SKU 库存列表
 *    示例返回：
 *    [
 *      {
 *        "id": 1,
 *        "productId": 1,
 *        "spData": "黑色;256GB",      // 规格值，多个规格用分号分隔
 *        "price": 6999.00,            // 销售价格
 *        "stock": 100,                // 库存数量
 *        "lowStock": 10,              // 库存预警值
 *        "skuCode": "HW-M60-B-256",   // SKU 编码
 *        "lockStock": 0               // 锁定库存（下单未支付时锁定）
 *      },
 *      {
 *        "id": 2,
 *        "productId": 1,
 *        "spData": "白色;256GB",
 *        "price": 6999.00,
 *        "stock": 50,
 *        "lowStock": 10,
 *        "skuCode": "HW-M60-W-256",
 *        "lockStock": 0
 *      }
 *    ]
 * 
 * 2. 批量更新 SKU 库存：POST /sku/update/{pid}
 *    路径参数：pid - 商品 ID
 *    请求体：[
 *              {
 *                "id": 1,                    // SKU ID（新增时不需要）
 *                "productId": 1,
 *                "spData": "黑色;256GB",
 *                "price": 6999.00,
 *                "stock": 150,               // 更新后的库存
 *                "lowStock": 15,
 *                "skuCode": "HW-M60-B-256",
 *                "lockStock": 0
 *              },
 *              {
 *                "id": 2,
 *                "productId": 1,
 *                "spData": "白色;256GB",
 *                "price": 6999.00,
 *                "stock": 80,
 *                "lowStock": 10,
 *                "skuCode": "HW-M60-W-256",
 *                "lockStock": 0
 *              }
 *            ]
 *    说明：
 *      - 如果 SKU 的 id 不为空，则更新该 SKU 信息
 *      - 如果 SKU 的 id 为空或为 0，则新增 SKU
 *      - 如果数据库中某个 SKU 在请求体中不存在，则会被删除
 *    返回：更新的数量
 * 
 * 注意事项：
 * 1. SKU 库存的更新是覆盖式的，会同步处理新增、更新、删除操作
 * 2. spData（规格数据）的格式需要与商品属性保持一致
 * 3. 库存更新时需要注意并发问题，避免超卖
 * 
 * Created by macro on 2018/4/27.
 */
@Controller
@Api(tags = "PmsSkuStockController", description = "sku 商品库存管理")
@RequestMapping("/sku")
public class PmsSkuStockController {
    @Autowired
    private PmsSkuStockService skuStockService;

    @ApiOperation("根据商品编号及编号模糊搜索sku库存")
    @RequestMapping(value = "/{pid}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsSkuStock>> getList(@PathVariable Long pid, @RequestParam(value = "keyword",required = false) String keyword) {
        List<PmsSkuStock> skuStockList = skuStockService.getList(pid, keyword);
        return CommonResult.success(skuStockList);
    }
    @ApiOperation("批量更新库存信息")
    @RequestMapping(value ="/update/{pid}",method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long pid, @RequestBody List<PmsSkuStock> skuStockList){
        int count = skuStockService.update(pid,skuStockList);
        if(count>0){
            return CommonResult.success(count);
        }else{
            return CommonResult.failed();
        }
    }
}
