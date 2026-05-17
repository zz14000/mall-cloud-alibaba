package com.mtcarpenter.mall.controller;

import com.mtcarpenter.mall.common.api.CommonPage;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.dto.PmsBrandParam;
import com.mtcarpenter.mall.model.PmsBrand;
import com.mtcarpenter.mall.service.PmsBrandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品品牌管理 Controller
 * 
 * 功能说明：
 * 用于管理商城系统中的商品品牌信息，包括品牌的增删改查、批量操作等功能。
 * 品牌是商品的重要属性，如：华为、苹果、小米、耐克等。
 * 
 * 主要功能：
 * 1. 品牌列表查询（支持分页和按名称搜索）
 * 2. 品牌详情查询
 * 3. 品牌新增、更新、删除
 * 4. 批量操作：批量删除、批量更新显示状态、批量更新厂家制造商状态
 * 
 * 使用示例：
 * 1. 获取所有品牌列表：GET /brand/listAll
 *    返回：所有品牌信息列表
 * 
 * 2. 分页查询品牌：GET /brand/list?keyword=华为&pageNum=1&pageSize=5
 *    参数：keyword - 品牌名称关键词（可选）
 *          pageNum - 页码，默认 1
 *          pageSize - 每页数量，默认 5
 *    返回：分页后的品牌列表
 * 
 * 3. 添加品牌：POST /brand/create
 *    请求体：{
 *              "name": "华为",
 *              "firstLetter": "H",
 *              "sort": 100,
 *              "factoryStatus": 1,      // 是否为品牌制造商：0->不是；1->是
 *              "showStatus": 1,         // 是否显示：0-> 不显示；1-> 显示
 *              "productCount": 0,
 *              "productCommentCount": 0,
 *              "logo": "http://example.com/logo.png",
 *              "subTitle": "华为 - 构建万物互联的智能世界",
 *              "description": "华为技术有限公司"
 *            }
 *    返回：操作结果
 * 
 * 4. 更新品牌：POST /brand/update/{id}
 *    路径参数：id - 品牌 ID
 *    请求体：同创建品牌的请求体格式
 * 
 * 5. 删除品牌：GET /brand/delete/{id}
 *    路径参数：id - 品牌 ID
 * 
 * 6. 批量删除品牌：POST /brand/delete/batch?ids=1,2,3
 *    参数：ids - 品牌 ID 列表，逗号分隔
 * 
 * 7. 批量更新显示状态：POST /brand/update/showStatus?ids=1,2,3&showStatus=1
 *    参数：ids - 品牌 ID 列表
 *          showStatus - 显示状态：0->不显示；1->显示
 * 
 * 8. 批量更新厂家制造商状态：POST /brand/update/factoryStatus?ids=1,2,3&factoryStatus=1
 *    参数：ids - 品牌 ID 列表
 *          factoryStatus - 是否为品牌制造商：0->不是；1->是
 * 
 * Created by macro on 2018/4/26.
 */
@Controller
@Api(tags = "PmsBrandController", description = "商品品牌管理")
@RequestMapping("/brand")
public class PmsBrandController {
    @Autowired
    private PmsBrandService brandService;

    @ApiOperation(value = "获取全部品牌列表")
    @RequestMapping(value = "/listAll", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<PmsBrand>> getList() {
        return CommonResult.success(brandService.listAllBrand());
    }

    @ApiOperation(value = "添加品牌")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult create(@Validated @RequestBody PmsBrandParam pmsBrand, BindingResult result) {
        CommonResult commonResult;
        int count = brandService.createBrand(pmsBrand);
        if (count == 1) {
            commonResult = CommonResult.success(count);
        } else {
            commonResult = CommonResult.failed();
        }
        return commonResult;
    }

    @ApiOperation(value = "更新品牌")
    @RequestMapping(value = "/update/{id}", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult update(@PathVariable("id") Long id,
                               @Validated @RequestBody PmsBrandParam pmsBrandParam,
                               BindingResult result) {
        CommonResult commonResult;
        int count = brandService.updateBrand(id, pmsBrandParam);
        if (count == 1) {
            commonResult = CommonResult.success(count);
        } else {
            commonResult = CommonResult.failed();
        }
        return commonResult;
    }

    @ApiOperation(value = "删除品牌")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult delete(@PathVariable("id") Long id) {
        int count = brandService.deleteBrand(id);
        if (count == 1) {
            return CommonResult.success(null);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "根据品牌名称分页获取品牌列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<CommonPage<PmsBrand>> getList(@RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize) {
        List<PmsBrand> brandList = brandService.listBrand(keyword, pageNum, pageSize);
        return CommonResult.success(CommonPage.restPage(brandList));
    }

    @ApiOperation(value = "根据编号查询品牌信息")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<PmsBrand> getItem(@PathVariable("id") Long id) {
        return CommonResult.success(brandService.getBrand(id));
    }

    @ApiOperation(value = "批量删除品牌")
    @RequestMapping(value = "/delete/batch", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult deleteBatch(@RequestParam("ids") List<Long> ids) {
        int count = brandService.deleteBrand(ids);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "批量更新显示状态")
    @RequestMapping(value = "/update/showStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateShowStatus(@RequestParam("ids") List<Long> ids,
                                   @RequestParam("showStatus") Integer showStatus) {
        int count = brandService.updateShowStatus(ids, showStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }

    @ApiOperation(value = "批量更新厂家制造商状态")
    @RequestMapping(value = "/update/factoryStatus", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateFactoryStatus(@RequestParam("ids") List<Long> ids,
                                      @RequestParam("factoryStatus") Integer factoryStatus) {
        int count = brandService.updateFactoryStatus(ids, factoryStatus);
        if (count > 0) {
            return CommonResult.success(count);
        } else {
            return CommonResult.failed();
        }
    }
}
