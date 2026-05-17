package com.mtcarpenter.mall.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.reflect.TypeToken;
import com.mtcarpenter.mall.common.CmsPrefrenceAreaProductRelationInput;
import com.mtcarpenter.mall.common.CmsSubjectProductRelationInput;
import com.mtcarpenter.mall.common.PmsProductOutput;
import com.mtcarpenter.mall.common.api.CommonResult;
import com.mtcarpenter.mall.common.api.ResultCode;
import com.mtcarpenter.mall.dao.*;
import com.mtcarpenter.mall.dto.PmsProductParam;
import com.mtcarpenter.mall.dto.PmsProductQueryParam;
import com.mtcarpenter.mall.dto.PmsProductResult;
import com.mtcarpenter.mall.mapper.*;
import com.mtcarpenter.mall.model.*;
import com.mtcarpenter.mall.service.PmsProductService;
import com.mtcarpenter.mall.client.CmsPrefrenceAreaProductRelationClient;
import com.mtcarpenter.mall.client.CmsSubjectProductRelationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品管理Service实现类
 * Created by macro on 2018/4/26.
 */
@Service
public class PmsProductServiceImpl implements PmsProductService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PmsProductServiceImpl.class);
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsMemberPriceDao memberPriceDao;
    @Autowired
    private PmsMemberPriceMapper memberPriceMapper;
    @Autowired
    private PmsProductLadderDao productLadderDao;
    @Autowired
    private PmsProductLadderMapper productLadderMapper;
    @Autowired
    private PmsProductFullReductionDao productFullReductionDao;
    @Autowired
    private PmsProductFullReductionMapper productFullReductionMapper;
    @Autowired
    private PmsSkuStockDao skuStockDao;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private PmsProductAttributeValueDao productAttributeValueDao;
    @Autowired
    private PmsProductAttributeValueMapper productAttributeValueMapper;

    @Autowired
    private CmsSubjectProductRelationClient cmsSubjectProductRelationClient;

    @Autowired
    private CmsPrefrenceAreaProductRelationClient cmsPrefrenceAreaProductRelationClient;

    @Autowired
    private PmsProductDao productDao;
    @Autowired
    private PmsProductVertifyRecordDao productVertifyRecordDao;

    @Override
    public int create(PmsProductParam productParam) {
        int count;
        //创建商品
        PmsProduct product = productParam;
        product.setId(null);
        productMapper.insertSelective(product);
        //根据促销类型设置价格：会员价格、阶梯价格、满减价格
        Long productId = product.getId();
        //会员价格
        relateAndInsertList(memberPriceDao, productParam.getMemberPriceList(), productId);
        //阶梯价格
        relateAndInsertList(productLadderDao, productParam.getProductLadderList(), productId);
        //满减价格
        relateAndInsertList(productFullReductionDao, productParam.getProductFullReductionList(), productId);
        //处理sku的编码
        handleSkuStockCode(productParam.getSkuStockList(), productId);
        //添加sku库存信息
        relateAndInsertList(skuStockDao, productParam.getSkuStockList(), productId);
        //添加商品参数,添加自定义商品规格
        relateAndInsertList(productAttributeValueDao, productParam.getProductAttributeValueList(), productId);
        //关联专题
        cmsSubjectProductRelationClient.relateAndInsertList(productParam.getSubjectProductRelationList(), productId);
        //关联优选
        cmsPrefrenceAreaProductRelationClient.relateAndInsertList(productParam.getPrefrenceAreaProductRelationList(), productId);
        count = 1;
        return count;
    }

    /**
     * 处理sku的编码，保证每个sku的编码都是唯一的，格式为：yyyyMMdd商品id索引id
     * @param skuStockList
     * @param productId
     */
    private void handleSkuStockCode(List<PmsSkuStock> skuStockList, Long productId) {
        if (CollectionUtils.isEmpty(skuStockList)) return;
        for (int i = 0; i < skuStockList.size(); i++) {
            PmsSkuStock skuStock = skuStockList.get(i);
            if (StringUtils.isEmpty(skuStock.getSkuCode())) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                StringBuilder sb = new StringBuilder();
                //日期
                sb.append(sdf.format(new Date()));
                //四位商品id
                sb.append(String.format("%04d", productId));
                //三位索引id
                sb.append(String.format("%03d", i + 1));
                skuStock.setSkuCode(sb.toString());
            }
        }
    }

    
    /**
     * 获取商品更新信息（用于编辑页面回显）
     * 
     * 方法功能：
     * 1. 查询商品基本信息（包含 SKU、属性、会员价格、阶梯价格、满减价格等）
     * 2. 通过 Feign 调用 CMS 服务，获取商品关联的专题信息
     * 3. 通过 Feign 调用 CMS 服务，获取商品关联的优选专区信息
     * 4. 将所有信息整合到 PmsProductResult 对象中返回
     * 
     * 调用场景：
     * - 后台管理系统点击"编辑商品"时，前端调用此接口获取商品完整信息
     * - API: GET /product/updateInfo/{id}
     * 
     * 数据组成：
     * - 商品主表信息（pms_product）：名称、价格、库存、详情等
     * - 商品 SKU 信息（pms_sku_stock）：多个规格组合的价格和库存
     * - 商品属性值（pms_product_attribute_value）：商品的规格参数值
     * - 会员价格（pms_member_price）：不同会员等级的价格
     * - 阶梯价格（pms_product_ladder）：购买数量达到一定值的优惠价格
     * - 满减价格（pms_product_full_reduction）：满多少减多少的促销价格
     * - 专题关联（cms_subject_product_relation）：商品所属专题（来自 CMS 服务）
     * - 优选关联（cms_prefrence_area_product_relation）：商品所属优选专区（来自 CMS 服务）
     * 
     * @param id 商品 ID
     * @return PmsProductResult 包含商品完整信息的对象，用于编辑表单回显
     */
    @Override
    public PmsProductResult getUpdateInfo(Long id) {
        // 步骤 1: 查询商品基本信息及其关联数据（SKU、属性、价格策略等）
        // 调用自定义 DAO，执行多表联查（见 PmsProductDao.xml 中的 getUpdateInfo 映射）
        //PmsProductResult继承PmsProductParam，PmsProductParam继承PmsProduct（商品主表）
        PmsProductResult updateInfo = productDao.getUpdateInfo(id);

        // 步骤 2: 通过 Feign 客户端调用 CMS 服务，获取商品关联的专题信息
        // 这是跨微服务调用，PMS 服务调用 CMS 服务获取内容管理相关数据
        CommonResult<List<CmsSubjectProductRelationInput>> listCommonResult = 
            cmsSubjectProductRelationClient.relationByProductId(id);
        
        // 创建 Gson 对象用于 JSON 反序列化
        Gson gson = new Gson();
        
        // 步骤 3: 处理专题关联数据
        // 判断 CMS 服务调用是否成功
        if (listCommonResult.getCode() == ResultCode.SUCCESS.getCode()) {
            // 将 CMS 服务返回的数据转换为 List<CmsSubjectProductRelationInput>
            // 转换过程：先将 Data 转为 JSON 字符串，再反序列化为目标类型
            // 使用 TypeToken 是为了保留泛型类型信息（Gson 的类型擦除问题）
            List<CmsSubjectProductRelationInput> relationInputList = gson.fromJson(
                JSON.toJSONString(listCommonResult.getData()),
                new TypeToken<List<CmsSubjectProductRelationInput>>() {
                }.getType()
            );
            // 将专题关联信息设置到结果对象中
            updateInfo.setSubjectProductRelationList(relationInputList);
        }
        
        // 步骤 4: 通过 Feign 客户端调用 CMS 服务，获取商品关联的优选专区信息
        CommonResult<List<CmsPrefrenceAreaProductRelationInput>> commonResult = 
            cmsPrefrenceAreaProductRelationClient.relationByProductId(id);
        
        // 步骤 5: 处理优选专区关联数据
        if (commonResult.getCode() == ResultCode.SUCCESS.getCode()) {
            // 同样使用 Gson 进行 JSON 反序列化
            List<CmsPrefrenceAreaProductRelationInput> areaProductRelationInputs = gson.fromJson(
                JSON.toJSONString(commonResult.getData()),
                new TypeToken<List<CmsPrefrenceAreaProductRelationInput>>() {
                }.getType()
            );
            // 将优选专区关联信息设置到结果对象中
            updateInfo.setPrefrenceAreaProductRelationList(areaProductRelationInputs);
        }
        
        // 步骤 6: 返回完整的商品信息（包含所有关联数据）
        return updateInfo;
    }

    @Override
    public int update(Long id, PmsProductParam productParam) {
        int count;
        //更新商品信息
        PmsProduct product = productParam;
        product.setId(id);
        productMapper.updateByPrimaryKeySelective(product);
        //更新商品参数，更改逻辑：先删除再插入
        //会员价格
        PmsMemberPriceExample pmsMemberPriceExample = new PmsMemberPriceExample();
        pmsMemberPriceExample.createCriteria().andProductIdEqualTo(id);
        memberPriceMapper.deleteByExample(pmsMemberPriceExample);
        relateAndInsertList(memberPriceDao, productParam.getMemberPriceList(), id);
        //阶梯价格
        PmsProductLadderExample ladderExample = new PmsProductLadderExample();
        ladderExample.createCriteria().andProductIdEqualTo(id);
        productLadderMapper.deleteByExample(ladderExample);
        relateAndInsertList(productLadderDao, productParam.getProductLadderList(), id);
        //满减价格
        PmsProductFullReductionExample fullReductionExample = new PmsProductFullReductionExample();
        fullReductionExample.createCriteria().andProductIdEqualTo(id);
        productFullReductionMapper.deleteByExample(fullReductionExample);
        relateAndInsertList(productFullReductionDao, productParam.getProductFullReductionList(), id);
        //修改sku库存信息
        handleUpdateSkuStockList(id, productParam);
        //修改商品参数,添加自定义商品规格
        PmsProductAttributeValueExample productAttributeValueExample = new PmsProductAttributeValueExample();
        productAttributeValueExample.createCriteria().andProductIdEqualTo(id);
        productAttributeValueMapper.deleteByExample(productAttributeValueExample);
        relateAndInsertList(productAttributeValueDao, productParam.getProductAttributeValueList(), id);
        //关联专题
        cmsSubjectProductRelationClient.relateAndUpdateList(productParam.getSubjectProductRelationList(), id);
        //关联优选
        cmsPrefrenceAreaProductRelationClient.relateAndUpdateList(productParam.getPrefrenceAreaProductRelationList(), id);
        count = 1;
        return count;
    }

    /**
     * 修改逻辑
     * @param id
     * @param productParam
     */
    private void handleUpdateSkuStockList(Long id, PmsProductParam productParam) {
        //当前的sku信息
        List<PmsSkuStock> currSkuList = productParam.getSkuStockList();
        //当前没有sku直接删除原来数据
        if (CollUtil.isEmpty(currSkuList)) {
            PmsSkuStockExample skuStockExample = new PmsSkuStockExample();
            skuStockExample.createCriteria().andProductIdEqualTo(id);
            skuStockMapper.deleteByExample(skuStockExample);
            return;
        }
        //获取初始sku信息
        PmsSkuStockExample skuStockExample = new PmsSkuStockExample();
        skuStockExample.createCriteria().andProductIdEqualTo(id);
        List<PmsSkuStock> oriStuList = skuStockMapper.selectByExample(skuStockExample);
        //获取新增sku信息，新增id为空
        List<PmsSkuStock> insertSkuList = currSkuList.stream().filter(item -> item.getId() == null).collect(Collectors.toList());
        //获取需要更新的sku信息
        List<PmsSkuStock> updateSkuList = currSkuList.stream().filter(item -> item.getId() != null).collect(Collectors.toList());
        List<Long> updateSkuIds = updateSkuList.stream().map(PmsSkuStock::getId).collect(Collectors.toList());
        //获取需要删除的sku信息
        List<PmsSkuStock> removeSkuList = oriStuList.stream().filter(item -> !updateSkuIds.contains(item.getId())).collect(Collectors.toList());
        handleSkuStockCode(insertSkuList, id);
        handleSkuStockCode(updateSkuList, id);
        //新增sku
        if (CollUtil.isNotEmpty(insertSkuList)) {
            relateAndInsertList(skuStockDao, insertSkuList, id);
        }
        //删除sku
        if (CollUtil.isNotEmpty(removeSkuList)) {
            List<Long> removeSkuIds = removeSkuList.stream().map(PmsSkuStock::getId).collect(Collectors.toList());
            PmsSkuStockExample removeExample = new PmsSkuStockExample();
            removeExample.createCriteria().andIdIn(removeSkuIds);
            skuStockMapper.deleteByExample(removeExample);
        }
        //修改sku
        if (CollUtil.isNotEmpty(updateSkuList)) {
            for (PmsSkuStock pmsSkuStock : updateSkuList) {
                skuStockMapper.updateByPrimaryKeySelective(pmsSkuStock);
            }
        }

    }

    @Override
    public List<PmsProduct> list(PmsProductQueryParam productQueryParam, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        PmsProductExample productExample = new PmsProductExample();
        PmsProductExample.Criteria criteria = productExample.createCriteria();
        //查询未删除的商品
        criteria.andDeleteStatusEqualTo(0);
        //组装查询条件，根据商品状态、审核状态、关键词、商品编号、品牌id、商品分类id查询
        if (productQueryParam.getPublishStatus() != null) {
            criteria.andPublishStatusEqualTo(productQueryParam.getPublishStatus());
        }
        if (productQueryParam.getVerifyStatus() != null) {
            criteria.andVerifyStatusEqualTo(productQueryParam.getVerifyStatus());
        }
        if (!StringUtils.isEmpty(productQueryParam.getKeyword())) {
            criteria.andNameLike("%" + productQueryParam.getKeyword() + "%");
        }
        if (!StringUtils.isEmpty(productQueryParam.getProductSn())) {
            criteria.andProductSnEqualTo(productQueryParam.getProductSn());
        }
        if (productQueryParam.getBrandId() != null) {
            criteria.andBrandIdEqualTo(productQueryParam.getBrandId());
        }
        if (productQueryParam.getProductCategoryId() != null) {
            criteria.andProductCategoryIdEqualTo(productQueryParam.getProductCategoryId());
        }
        return productMapper.selectByExample(productExample);
    }

    @Override
    public int updateVerifyStatus(List<Long> ids, Integer verifyStatus, String detail) {
        PmsProduct product = new PmsProduct();
        product.setVerifyStatus(verifyStatus);
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andIdIn(ids);
        List<PmsProductVertifyRecord> list = new ArrayList<>();
        int count = productMapper.updateByExampleSelective(product, example);
        //修改完审核状态后插入审核记录表
        for (Long id : ids) {
            PmsProductVertifyRecord record = new PmsProductVertifyRecord();
            record.setProductId(id);
            record.setCreateTime(new Date());
            record.setDetail(detail);
            record.setStatus(verifyStatus);
            //从SecurityContextHolder.getContext() 中获取当前登录用户
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            record.setVertifyMan(username);
            list.add(record);
        }
        productVertifyRecordDao.insertList(list);
        return count;
    }

    @Override
    public int updatePublishStatus(List<Long> ids, Integer publishStatus) {
        PmsProduct record = new PmsProduct();
        record.setPublishStatus(publishStatus);
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andIdIn(ids);
        return productMapper.updateByExampleSelective(record, example);
    }

    @Override
    public int updateRecommendStatus(List<Long> ids, Integer recommendStatus) {
        PmsProduct record = new PmsProduct();
        record.setRecommandStatus(recommendStatus);
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andIdIn(ids);
        return productMapper.updateByExampleSelective(record, example);
    }

    @Override
    public int updateNewStatus(List<Long> ids, Integer newStatus) {
        PmsProduct record = new PmsProduct();
        record.setNewStatus(newStatus);
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andIdIn(ids);
        return productMapper.updateByExampleSelective(record, example);
    }

    @Override
    public int updateDeleteStatus(List<Long> ids, Integer deleteStatus) {
        PmsProduct record = new PmsProduct();
        record.setDeleteStatus(deleteStatus);
        PmsProductExample example = new PmsProductExample();
        example.createCriteria().andIdIn(ids);
        return productMapper.updateByExampleSelective(record, example);
    }

    @Override
    public List<PmsProduct> list(String keyword) {
        PmsProductExample productExample = new PmsProductExample();
        PmsProductExample.Criteria criteria = productExample.createCriteria();
        criteria.andDeleteStatusEqualTo(0);
        if (!StringUtils.isEmpty(keyword)) {
            criteria.andNameLike("%" + keyword + "%");
            productExample.or().andDeleteStatusEqualTo(0).andProductSnLike("%" + keyword + "%");
        }
        return productMapper.selectByExample(productExample);
    }

    /**
     * 根据 商品 id获取商品信息
     *
     * @param productId
     * @return
     */
    @Override
    public PmsProductOutput getProductByProductId(Long productId) {
        PmsProduct pmsProduct = productMapper.selectByPrimaryKey(productId);
        PmsProductOutput pmsProductOutput = new PmsProductOutput();
        BeanUtils.copyProperties(pmsProduct, pmsProductOutput);
        return pmsProductOutput;
    }

    /**
     * 建立和插入关系表操作
     *
     * @param dao       可以操作的dao
     * @param dataList  要插入的数据
     * @param productId 建立关系的id
     */
    private void relateAndInsertList(Object dao, List dataList, Long productId) {
        try {
            if (CollectionUtils.isEmpty(dataList)) return;  
            for (Object item : dataList) {
                // 2.1 通过反射获取 setId 方法
                Method setId = item.getClass().getMethod("setId", Long.class);
                // 2.2 调用 setId 方法，将 ID 设为 null（新增数据不需要 ID）
                setId.invoke(item, (Long) null);
                // 2.3 通过反射获取 setProductId 方法
                Method setProductId = item.getClass().getMethod("setProductId", Long.class);
                // 2.4 调用 setProductId 方法，设置关联的商品 ID
                setProductId.invoke(item, productId);
            }
            // 步骤 3: 通过反射获取 DAO 的 insertList 方法
            Method insertList = dao.getClass().getMethod("insertList", List.class);
            // 步骤 4: 调用 insertList 方法，批量插入数据
            insertList.invoke(dao, dataList);
        } catch (Exception e) {
            LOGGER.warn("创建产品出错:{}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

}
