package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonPage;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.dto.PmsProductAttributeCategoryItem;
import com.mtcarpenter.mall.model.PmsProductAttributeCategory;
import com.mtcarpenter.mall.service.PmsProductAttributeCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品属性分类管理 Controller
 * 
 * 功能说明：
 * 用于管理商品属性的分类体系。商品属性分类是对商品属性进行分组管理的结构，
 * 便于对不同类型的商品定义不同的属性集合。
 * 
 * 属性分类体系说明：
 * 商品属性分类 -> 商品属性/参数
 * 例如：
 *   - 属性分类：手机属性
 *     - 属性：颜色、内存、存储容量
 *     - 参数：CPU 型号、屏幕尺寸、分辨率
 *   - 属性分类：电脑属性
 *     - 属性：颜色、CPU、内存
 *     - 参数：屏幕尺寸、分辨率、显卡型号
 * 
 * 主要功能：
 * 1. 商品属性分类的增删改查
 * 2. 分页查询所有属性分类
 * 3. 获取所有属性分类及其下属的属性列表（用于创建商品时选择属性）
 * 
 * 使用示例：
 * 1. 添加商品属性分类：POST /productAttribute/category/create?name=手机属性
 *    参数：name - 属性分类名称
 *    返回：创建的分类数量
 * 
 * 2. 更新商品属性分类：POST /productAttribute/category/update/{id}?name=智能手机属性
 *    路径参数：id - 属性分类 ID
 *    参数：name - 新的分类名称
 *    返回：更新的数量
 * 
 * 3. 删除商品属性分类：GET /productAttribute/category/delete/{id}
 *    路径参数：id - 属性分类 ID
 *    注意：如果该分类下已有属性，则无法删除
 *    返回：删除的数量
 * 
 * 4. 获取单个属性分类信息：GET /productAttribute/category/{id}
 *    路径参数：id - 属性分类 ID
 *    返回：分类详细信息
 *    示例返回：
 *    {
 *      "id": 1,
 *      "name": "手机属性",
 *      "attributeCount": 10,      // 该分类下的属性数量
 *      "paramCount": 5            // 该分类下的参数数量
 *    }
 * 
 * 5. 分页获取所有属性分类：GET /productAttribute/category/list?pageNum=1&pageSize=5
 *    参数：pageNum - 页码，默认 1
 *          pageSize - 每页数量，默认 5
 *    返回：分页后的属性分类列表
 * 
 * 6. 获取所有属性分类及其下属性：GET /productAttribute/category/list/withAttr
 *    返回：所有属性分类及其下属的属性列表
 *    说明：用于创建商品时，根据选择的属性分类，显示该分类下的所有属性供选择
 *    示例返回：
 *    [
 *      {
 *        "id": 1,
 *        "name": "手机属性",
 *        "productAttributeList": [
 *          {"id": 1, "name": "颜色", "type": 0},
 *          {"id": 2, "name": "内存", "type": 0},
 *          {"id": 3, "name": "CPU 型号", "type": 1},
 *          {"id": 4, "name": "屏幕尺寸", "type": 1}
 *        ]
 *      },
 *      {
 *        "id": 2,
 *        "name": "电脑属性",
 *        "productAttributeList": [
 *          {"id": 5, "name": "颜色", "type": 0},
 *          {"id": 6, "name": "CPU", "type": 0},
 *          {"id": 7, "name": "屏幕尺寸", "type": 1}
 *        ]
 *      }
 *    ]
 * 
 * Created by macro on 2018/4/26.
 */
@Controller
@Api(tags = "PmsProductAttributeCategoryController", description = "商品属性分类管理")
@RequestMapping("/productAttribute/category")
public class PmsProductAttributeCategoryController {
    @Autowired
    private PmsProductAttributeCategoryService productAttributeCategoryService;

    @ApiOperation("添加商品属性分类")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@RequestParam String name) {
        int count = productAttributeCategoryService.create(name);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("修改商品属性分类")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id, @RequestParam String name) {
        int count = productAttributeCategoryService.update(id, name);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("删除单个商品属性分类")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        int count = productAttributeCategoryService.delete(id);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("获取单个商品属性分类信息")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsProductAttributeCategory> getItem(@PathVariable Long id) {
        PmsProductAttributeCategory productAttributeCategory = productAttributeCategoryService.getItem(id);
        return CommonResult.success(productAttributeCategory);
    }

    @ApiOperation("分页获取所有商品属性分类")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PmsProductAttributeCategory>> getList(@RequestParam(defaultValue = "5") Integer pageSize, @RequestParam(defaultValue = "1") Integer pageNum) {
        List<PmsProductAttributeCategory> productAttributeCategoryList = productAttributeCategoryService.getList(pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(productAttributeCategoryList));
    }

    @ApiOperation("获取所有商品属性分类及其下属性")
    @RequestMapping(value = "/list/withAttr", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsProductAttributeCategoryItem>> getListWithAttr() {
        List<PmsProductAttributeCategoryItem> productAttributeCategoryResultList = productAttributeCategoryService.getListWithAttr();
        return CommonResult.success(productAttributeCategoryResultList);
    }
}
