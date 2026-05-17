package com.mtcarpenter.mall.controller;


import com.mtcarpenter.mall.common.PmsProductOutput;
import com.mtcarpenter.mall.common.api.CommonPage;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.dto.PmsProductParam;
import com.mtcarpenter.mall.dto.PmsProductQueryParam;
import com.mtcarpenter.mall.dto.PmsProductResult;
import com.mtcarpenter.mall.model.PmsProduct;
import com.mtcarpenter.mall.service.PmsProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理 Controller
 * 
 * 功能说明：
 * 用于管理商城系统中的商品信息，是 PMS 模块最核心的 Controller。
 * 支持商品的创建、查询、更新、审核、上下架、推荐等各种操作。
 * 
 * 主要功能：
 * 1. 商品创建和更新（包含商品详情、属性、规格、库存等完整信息）
 * 2. 商品查询（支持分页、按名称/货号模糊查询）
 * 3. 商品审核管理
 * 4. 商品上下架管理
 * 5. 商品推荐管理
 * 6. 商品新品标记
 * 7. 商品删除状态管理
 * 8. 通过 ID 查询商品完整信息（用于商品详情展示）
 * 
 * 使用示例：
 * 1. 创建商品：POST /product/create
 *    请求体：{
 *              "brandId": 1,
 *              "productCategoryId": 10,
 *              "name": "华为 Mate 60 Pro",
 *              "subTitle": "卫星通话，昆仑玻璃",
 *              "price": 6999.00,
 *              "originalPrice": 7999.00,
 *              "stock": 1000,
 *              "unit": "台",
 *              "weight": 225,
 *              "sort": 100,
 *              "albumPics": "url1,url2,url3",  // 商品相册图片
 *              "detail": "商品详情 HTML 内容",
 *              "detailTitle": "商品详情标题",
 *              "detailDesc": "商品详情描述",
 *              "keywords": "华为，Mate60，5G",  // 关键词
 *              "note": "备注信息",
 *              "publishStatus": 0,           // 上架状态：0-> 下架；1-> 上架
 *              "verifyStatus": 0,            // 审核状态：0-> 未审核；1-> 已审核
 *              "sort": 100,                  // 排序值
 *              "newStatus": 1,               // 是否新品：0-> 不是；1-> 是
 *              "recommendStatus": 1,         // 是否推荐：0-> 不推荐；1-> 推荐
 *              "productAttributeValueList": [  // 商品属性值列表
 *                  {"productAttributeId": 1, "value": "5G"}
 *              ],
 *              "skuStockList": [  // SKU 库存列表
 *                  {
 *                      "spData": "黑色;256GB",
 *                      "price": 6999.00,
 *                      "stock": 100,
 *                      "skuCode": "HW-M60-B-256"
 *                  }
 *              ]
 *            }
 *    返回：创建的商品数量
 * 
 * 2. 获取商品更新信息：GET /product/updateInfo/{id}
 *    路径参数：id - 商品 ID
 *    返回：商品完整信息（包含 SKU、属性等），用于编辑页面回显
 * 
 * 3. 更新商品：POST /product/update/{id}
 *    路径参数：id - 商品 ID
 *    请求体：同创建商品格式
 * 
 * 4. 查询商品列表：GET /product/list?pageNum=1&pageSize=5&publishStatus=1&verifyStatus=1&keyword=华为
 *    参数：pageNum - 页码
 *          pageSize - 每页数量
 *          publishStatus - 上架状态（可选）
 *          verifyStatus - 审核状态（可选）
 *          keyword - 商品名称关键词（可选）
 *    返回：分页商品列表
 * 
 * 5. 模糊查询商品：GET /product/simpleList?keyword=华为
 *    参数：keyword - 商品名称或货号
 *    返回：匹配的商品列表（简化版）
 * 
 * 6. 批量审核商品：POST /product/update/verifyStatus?ids=1,2,3&verifyStatus=1&detail=审核通过
 *    参数：ids - 商品 ID 列表
 *          verifyStatus - 审核状态：0->未审核；1->已审核
 *          detail - 审核详情说明
 * 
 * 7. 批量上下架：POST /product/update/publishStatus?ids=1,2,3&publishStatus=1
 *    参数：ids - 商品 ID 列表
 *          publishStatus - 上架状态：0->下架；1->上架
 * 
 * 8. 批量推荐商品：POST /product/update/recommendStatus?ids=1,2,3&recommendStatus=1
 *    参数：ids - 商品 ID 列表
 *          recommendStatus - 推荐状态：0->不推荐；1->推荐
 * 
 * 9. 批量设为新品：POST /product/update/newStatus?ids=1,2,3&newStatus=1
 *    参数：ids - 商品 ID 列表
 *          newStatus - 新品状态：0->不是新品；1->是新品
 * 
 * 10. 批量修改删除状态：POST /product/update/deleteStatus?ids=1,2,3&deleteStatus=1
 *     参数：ids - 商品 ID 列表
 *           deleteStatus - 删除状态：0->未删除；1->已删除（逻辑删除）
 * 
 * 11. 通过 ID 查询商品：GET /product/getProductByProductId?productId=1
 *     参数：productId - 商品 ID
 *     返回：商品完整信息（用于前台商品详情展示）
 * 
 * Created by macro on 2018/4/26.
 */
@Controller
@Api(tags = "PmsProductController", description = "商品管理")
@RequestMapping("/product")
public class PmsProductController {
    @Autowired
    private PmsProductService productService;

    @ApiOperation("创建商品")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody PmsProductParam productParam, BindingResult bindingResult) {
        int count = productService.create(productParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("根据商品id获取商品编辑信息")
    @RequestMapping(value = "/updateInfo/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsProductResult> getUpdateInfo(@PathVariable Long id) {
        PmsProductResult productResult = productService.getUpdateInfo(id);
        return CommonResult.success(productResult);
    }

    @ApiOperation("更新商品")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody PmsProductParam productParam, BindingResult bindingResult) {
        int count = productService.update(id, productParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("查询商品")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PmsProduct>> getList(PmsProductQueryParam productQueryParam,
                                                        @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        List<PmsProduct> productList = productService.list(productQueryParam, pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(productList));
    }

    @ApiOperation("根据商品名称或货号模糊查询")
    @RequestMapping(value = "/simpleList", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsProduct>> getList(String keyword) {
        List<PmsProduct> productList = productService.list(keyword);
        return CommonResult.success(productList);
    }

    @ApiOperation("批量修改审核状态")
    @RequestMapping(value = "/update/verifyStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateVerifyStatus(@RequestParam("ids") List<Long> ids,
                                           @RequestParam("verifyStatus") Integer verifyStatus,
                                           @RequestParam("detail") String detail) {
        int count = productService.updateVerifyStatus(ids, verifyStatus, detail);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("批量上下架")
    @RequestMapping(value = "/update/publishStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updatePublishStatus(@RequestParam("ids") List<Long> ids,
                                            @RequestParam("publishStatus") Integer publishStatus) {
        int count = productService.updatePublishStatus(ids, publishStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("批量推荐商品")
    @RequestMapping(value = "/update/recommendStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateRecommendStatus(@RequestParam("ids") List<Long> ids,
                                              @RequestParam("recommendStatus") Integer recommendStatus) {
        int count = productService.updateRecommendStatus(ids, recommendStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("批量设为新品")
    @RequestMapping(value = "/update/newStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateNewStatus(@RequestParam("ids") List<Long> ids,
                                        @RequestParam("newStatus") Integer newStatus) {
        int count = productService.updateNewStatus(ids, newStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("批量修改删除状态")
    @RequestMapping(value = "/update/deleteStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateDeleteStatus(@RequestParam("ids") List<Long> ids,
                                           @RequestParam("deleteStatus") Integer deleteStatus) {
        int count = productService.updateDeleteStatus(ids, deleteStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }


    @ApiOperation("通过id查询商品信息")
    @RequestMapping(value = "/getProductByProductId", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsProductOutput> getProductByProductId(@RequestParam("productId") Long productId ) {
        PmsProductOutput pmsProduct = productService.getProductByProductId(productId);
        return CommonResult.success(pmsProduct);
    }


}
