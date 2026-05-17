package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonPage;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.dto.PmsProductAttributeParam;
import com.mtcarpenter.mall.dto.ProductAttrInfo;
import com.mtcarpenter.mall.model.PmsProductAttribute;
import com.mtcarpenter.mall.service.PmsProductAttributeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品属性管理 Controller
 * 
 * 功能说明：
 * 用于管理商城系统中的商品属性信息。商品属性分为两类：
 * 1. 属性（type=0）：用于描述商品的特征，如：颜色、内存、尺寸等，支持自定义输入值
 * 2. 参数（type=1）：商品的规格参数，如：CPU 型号、屏幕尺寸、分辨率等，通常是固定的
 * 
 * 属性分类体系：
 * 商品属性分类 -> 商品属性/参数
 * 例如：
 *   - 属性分类：手机属性
 *     - 属性：颜色（支持多个值：黑色、白色、蓝色）
 *     - 属性：内存（支持多个值：6GB、8GB、12GB）
 *     - 参数：CPU 型号
 *     - 参数：屏幕尺寸
 * 
 * 主要功能：
 * 1. 根据分类查询属性列表或参数列表
 * 2. 添加、更新、删除商品属性
 * 3. 查询单个商品属性详情
 * 4. 根据商品分类获取相关属性及属性分类信息
 * 
 * 使用示例：
 * 1. 根据分类查询属性列表：GET /productAttribute/list/{cid}?type=0&pageNum=1&pageSize=5
 *    路径参数：cid - 商品属性分类 ID
 *    参数：type - 查询类型：0->属性；1->参数
 *          pageNum - 页码
 *          pageSize - 每页数量
 *    示例：
 *      - GET /productAttribute/list/1?type=0 - 查询分类 ID 为 1 的所有属性
 *      - GET /productAttribute/list/1?type=1 - 查询分类 ID 为 1 的所有参数
 *    返回：分页后的属性列表
 * 
 * 2. 添加商品属性：POST /productAttribute/create
 *    请求体：{
 *              "productAttributeCategoryId": 1,  // 所属属性分类 ID
 *              "name": "颜色",                    // 属性名称
 *              "type": 0,                        // 属性类型：0->属性；1->参数
 *              "inputType": 0,                   // 属性值录入方式：0->手工录入；1->从列表中选取
 *              "inputList": "黑色，白色，蓝色",     // 可选值列表（inputType=1 时有效），逗号分隔
 *              "filterType": 0,                  // 分类筛选类型：0->不需要；1->普通筛选；2->颜色筛选
 *              "searchType": 0,                  // 检索类型：0->不需要检索；1->关键字检索；2->精度检索
 *              "relatedStatus": 1,               // 是否与产品关联有关：0->不关联；1->关联
 *              "handAddStatus": 1,               // 是否支持手动新增：0->不支持；1->支持
 *              "sort": 100                       // 排序值
 *            }
 *    返回：创建的属性数量
 * 
 * 3. 更新商品属性：POST /productAttribute/update/{id}
 *    路径参数：id - 属性 ID
 *    请求体：同创建属性格式
 * 
 * 4. 查询单个商品属性：GET /productAttribute/{id}
 *    路径参数：id - 属性 ID
 *    返回：属性详细信息
 * 
 * 5. 批量删除商品属性：POST /productAttribute/delete?ids=1,2,3
 *    参数：ids - 属性 ID 列表，逗号分隔
 *    返回：删除的数量
 * 
 * 6. 根据商品分类获取属性信息：GET /productAttribute/attrInfo/{productCategoryId}
 *    路径参数：productCategoryId - 商品分类 ID
 *    返回：该商品分类下的所有属性及属性分类信息
 *    说明：用于前台商品搜索时，根据分类显示可筛选的属性列表
 *    示例返回：
 *    [
 *      {
 *        "attributeId": 1,
 *        "attributeName": "颜色",
 *        "attributeCategoryId": 1,
 *        "attributeCategoryName": "手机属性"
 *      },
 *      {
 *        "attributeId": 2,
 *        "attributeName": "内存",
 *        "attributeCategoryId": 1,
 *        "attributeCategoryName": "手机属性"
 *      }
 *    ]
 * 
 * Created by macro on 2018/4/26.
 */
@Controller
@Api(tags = "PmsProductAttributeController", description = "商品属性管理")
@RequestMapping("/productAttribute")
public class PmsProductAttributeController {
    @Autowired
    private PmsProductAttributeService productAttributeService;

    @ApiOperation("根据分类查询属性列表或参数列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "type", value = "0表示属性，1表示参数", required = true, paramType = "query", dataType = "integer")})
    @RequestMapping(value = "/list/{cid}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PmsProductAttribute>> getList(@PathVariable Long cid,
                                                                 @RequestParam(value = "type") Integer type,
                                                                 @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                                                 @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        List<PmsProductAttribute> productAttributeList = productAttributeService.getList(cid, type, pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(productAttributeList));
    }

    @ApiOperation("添加商品属性信息")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestBody PmsProductAttributeParam productAttributeParam, BindingResult bindingResult) {
        int count = productAttributeService.create(productAttributeParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("修改商品属性信息")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestBody PmsProductAttributeParam productAttributeParam, BindingResult bindingResult) {
        int count = productAttributeService.update(id, productAttributeParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("查询单个商品属性")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsProductAttribute> getItem(@PathVariable Long id) {
        PmsProductAttribute productAttribute = productAttributeService.getItem(id);
        return CommonResult.success(productAttribute);
    }

    @ApiOperation("批量删除商品属性")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@RequestParam("ids") List<Long> ids) {
        int count = productAttributeService.delete(ids);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("根据商品分类的id获取商品属性及属性分类")
    @RequestMapping(value = "/attrInfo/{productCategoryId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<ProductAttrInfo>> getAttrInfo(@PathVariable Long productCategoryId) {
        List<ProductAttrInfo> productAttrInfoList = productAttributeService.getProductAttrInfo(productCategoryId);
        return CommonResult.success(productAttrInfoList);
    }
}
