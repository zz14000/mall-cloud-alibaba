package com.mtcarpenter.mall.service.impl;

import com.github.pagehelper.PageHelper;
import com.mtcarpenter.mall.dao.PmsProductAttributeCategoryDao;
import com.mtcarpenter.mall.dto.PmsProductAttributeCategoryItem;
import com.mtcarpenter.mall.mapper.PmsProductAttributeCategoryMapper;
import com.mtcarpenter.mall.model.PmsProductAttributeCategory;
import com.mtcarpenter.mall.model.PmsProductAttributeCategoryExample;
import com.mtcarpenter.mall.service.PmsProductAttributeCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PmsProductAttributeCategoryService实现类
 * Created by macro on 2018/4/26.
 */
@Service
public class PmsProductAttributeCategoryServiceImpl implements PmsProductAttributeCategoryService {
    @Autowired
    private PmsProductAttributeCategoryMapper productAttributeCategoryMapper;
    @Autowired
    private PmsProductAttributeCategoryDao productAttributeCategoryDao;

    /**
     * 只创建了类名
     * @param name
     * @return
     */
    @Override
    public int create(String name) {
        PmsProductAttributeCategory productAttributeCategory = new PmsProductAttributeCategory();
        productAttributeCategory.setName(name);
        return productAttributeCategoryMapper.insertSelective(productAttributeCategory);
    }

    /**
     * 只更新类名
     * @param id
     * @param name
     * @return
     */
    @Override
    public int update(Long id, String name) {
        PmsProductAttributeCategory productAttributeCategory = new PmsProductAttributeCategory();
        productAttributeCategory.setName(name);
        productAttributeCategory.setId(id);
        return productAttributeCategoryMapper.updateByPrimaryKeySelective(productAttributeCategory);
    }

    @Override
    public int delete(Long id) {
        return productAttributeCategoryMapper.deleteByPrimaryKey(id);
    }

    /**
     * 获取单个AttributeCategory，只有属性数量和参数数量，没他们具体是啥
     * @param id
     * @return
     */
    @Override
    public PmsProductAttributeCategory getItem(Long id) {
        return productAttributeCategoryMapper.selectByPrimaryKey(id);
    }

    /**
     * 获取AttributeCategory列表
     * @param pageSize
     * @param pageNum
     * @return
     */
    @Override
    public List<PmsProductAttributeCategory> getList(Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum,pageSize);
        return productAttributeCategoryMapper.selectByExample(new PmsProductAttributeCategoryExample());
    }

    /**
     * 获取所有分类以及它们分类下的属性
     * 分类 1 → [属性 1, 属性 2, 属性 3]
     * 分类 2 → [属性 4, 属性 5]
     * 分类 3 → []  (没有属性)
     * ...
     * @return
     */
    @Override
    public List<PmsProductAttributeCategoryItem> getListWithAttr() {
        return productAttributeCategoryDao.getListWithAttr();
    }
}
