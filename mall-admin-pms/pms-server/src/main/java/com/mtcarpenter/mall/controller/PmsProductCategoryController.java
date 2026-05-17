package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonPage;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.dto.PmsProductCategoryParam;
import com.mtcarpenter.mall.dto.PmsProductCategoryWithChildrenItem;
import com.mtcarpenter.mall.model.PmsProductCategory;
import com.mtcarpenter.mall.service.PmsProductCategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类管理 Controller
 * 
 * 功能说明：
 * 用于管理商城系统中的商品分类体系，支持多级分类结构（树形结构）。
 * 商品分类是商品组织的核心结构，如：手机 -> 智能手机 -> 华为手机。
 * 
 * 主要功能：
 * 1. 商品分类的增删改查
 * 2. 分页查询指定父分类下的子分类
 * 3. 批量修改分类的显示状态和导航栏显示状态
 * 4. 获取所有一级分类及其子分类（树形结构）
 * 
 * 分类层级说明：
 * - 一级分类：parentId = 0，如：手机、电脑、服装
 * - 二级分类：parentId = 一级分类 ID，如：智能手机、笔记本电脑
 * - 三级分类：parentId = 二级分类 ID，如：华为手机、游戏本
 * 
 * 使用示例：
 * 1. 添加商品分类：POST /productCategory/create
 *    请求体：{
 *              "parentId": 0,              // 父分类 ID，0 表示一级分类
 *              "name": "手机",
 *              "level": 1,                 // 分类级别：1->1 级；2->2 级；3->3 级
 *              "sort": 100,                // 排序值，数值越小越靠前
 *              "showStatus": 1,            // 是否显示：0->不显示；1->显示
 *              "navStatus": 1,             // 是否显示在导航栏：0->不显示；1->显示
 *              "icon": "http://example.com/icon.png",  // 分类图标
 *              "productUnit": "台",        // 计量单位
 *              "description": "手机类产品"  // 分类描述
 *            }
 *    返回：创建的分类数量
 * 
 * 2. 更新商品分类：POST /productCategory/update/{id}
 *    路径参数：id - 分类 ID
 *    请求体：同创建分类格式
 * 
 * 3. 分页查询商品分类：GET /productCategory/list/{parentId}?pageNum=1&pageSize=5
 *    路径参数：parentId - 父分类 ID（查询该父分类下的所有子分类）
 *    参数：pageNum - 页码
 *          pageSize - 每页数量
 *    示例：
 *      - GET /productCategory/list/0 - 查询所有一级分类
 *      - GET /productCategory/list/1 - 查询父分类 ID 为 1 的所有子分类
 *    返回：分页后的分类列表
 * 
 * 4. 根据 ID 获取分类：GET /productCategory/{id}
 *    路径参数：id - 分类 ID
 *    返回：分类详细信息
 * 
 * 5. 删除商品分类：POST /productCategory/delete/{id}
 *    路径参数：id - 分类 ID
 *    注意：如果该分类下有子分类或商品，则无法删除
 * 
 * 6. 批量修改导航栏显示状态：POST /productCategory/update/navStatus?ids=1,2,3&navStatus=1
 *    参数：ids - 分类 ID 列表
 *          navStatus - 导航栏显示状态：0->不显示；1->显示
 *    说明：用于控制分类是否在首页导航栏中展示
 * 
 * 7. 批量修改显示状态：POST /productCategory/update/showStatus?ids=1,2,3&showStatus=1
 *    参数：ids - 分类 ID 列表
 *          showStatus - 显示状态：0->不显示；1->显示
 *    说明：用于控制分类是否在前台显示
 * 
 * 8. 查询所有一级分类及子分类：GET /productCategory/list/withChildren
 *    返回：树形结构的分类列表，包含所有一级分类及其下属的子分类
 *    示例返回：
 *    [
 *      {
 *        "id": 1,
 *        "name": "手机",
 *        "children": [
 *          {"id": 10, "name": "智能手机"},
 *          {"id": 11, "name": "老人手机"}
 *        ]
 *      }
 *    ]
 * 
 * Created by macro on 2018/4/26.
 */
@Controller
@Api(tags = "PmsProductCategoryController", description = "商品分类管理")
@RequestMapping("/productCategory")
public class PmsProductCategoryController {
    @Autowired
    private PmsProductCategoryService productCategoryService;

    @ApiOperation("添加产品分类")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@Validated @RequestBody PmsProductCategoryParam productCategoryParam,
                         BindingResult result) {
        int count = productCategoryService.create(productCategoryParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("修改商品分类")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable Long id,
                         @Validated
                         @RequestBody PmsProductCategoryParam productCategoryParam,
                         BindingResult result) {
        int count = productCategoryService.update(id, productCategoryParam);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("分页查询商品分类")
    @RequestMapping(value = "/list/{parentId}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PmsProductCategory>> getList(@PathVariable Long parentId,
                                                                @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
                                                                @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum) {
        List<PmsProductCategory> productCategoryList = productCategoryService.getList(parentId, pageSize, pageNum);
        return CommonResult.success(CommonPage.restPage(productCategoryList));
    }

    @ApiOperation("根据id获取商品分类")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsProductCategory> getItem(@PathVariable Long id) {
        PmsProductCategory productCategory = productCategoryService.getItem(id);
        return CommonResult.success(productCategory);
    }

    @ApiOperation("删除商品分类")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@PathVariable Long id) {
        int count = productCategoryService.delete(id);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("修改导航栏显示状态")
    @RequestMapping(value = "/update/navStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateNavStatus(@RequestParam("ids") List<Long> ids, @RequestParam("navStatus") Integer navStatus) {
        int count = productCategoryService.updateNavStatus(ids, navStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("修改显示状态")
    @RequestMapping(value = "/update/showStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateShowStatus(@RequestParam("ids") List<Long> ids, @RequestParam("showStatus") Integer showStatus) {
        int count = productCategoryService.updateShowStatus(ids, showStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation("查询所有一级分类及子分类")
    @RequestMapping(value = "/list/withChildren", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsProductCategoryWithChildrenItem>> listWithChildren() {
        List<PmsProductCategoryWithChildrenItem> list = productCategoryService.listWithChildren();
        return CommonResult.success(list);
    }
}
