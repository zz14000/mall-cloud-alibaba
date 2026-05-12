package com.mtcarpenter.mall.model;

import java.io.Serializable;

/**
 * 会员与商品分类关联实体类
 * <p>
 * 对应数据库表 ums_member_product_category_relation，记录会员关注的商品分类，
 * 用于个性化推荐和内容推送。系统可根据会员关注的分类，优先展示相关商品和活动信息。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>memberId - 会员ID，关联 UmsMember.id</li>
 *   <li>productCategoryId - 商品分类ID</li>
 * </ul>
 * </p>
 */
public class UmsMemberProductCategoryRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long memberId;
    private Long productCategoryId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getProductCategoryId() {
        return productCategoryId;
    }

    public void setProductCategoryId(Long productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", memberId=").append(memberId);
        sb.append(", productCategoryId=").append(productCategoryId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}