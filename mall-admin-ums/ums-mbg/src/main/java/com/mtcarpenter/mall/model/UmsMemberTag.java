package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员标签实体类
 * <p>
 * 对应数据库表 ums_member_tag，定义会员标签及其自动打标签规则。
 * 系统可根据会员的订单完成数量和金额自动为会员打上对应标签，
 * 用于用户分群、精准营销和个性化推荐。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>name - 标签名称</li>
 *   <li>finishOrderCount - 自动打标签完成订单数量阈值</li>
 *   <li>finishOrderAmount - 自动打标签完成订单金额阈值</li>
 * </ul>
 * </p>
 */
public class UmsMemberTag implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    @ApiModelProperty(value = "自动打标签完成订单数量")
    private Integer finishOrderCount;
    @ApiModelProperty(value = "自动打标签完成订单金额")
    private BigDecimal finishOrderAmount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getFinishOrderCount() {
        return finishOrderCount;
    }

    public void setFinishOrderCount(Integer finishOrderCount) {
        this.finishOrderCount = finishOrderCount;
    }

    public BigDecimal getFinishOrderAmount() {
        return finishOrderAmount;
    }

    public void setFinishOrderAmount(BigDecimal finishOrderAmount) {
        this.finishOrderAmount = finishOrderAmount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", finishOrderCount=").append(finishOrderCount);
        sb.append(", finishOrderAmount=").append(finishOrderAmount);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}