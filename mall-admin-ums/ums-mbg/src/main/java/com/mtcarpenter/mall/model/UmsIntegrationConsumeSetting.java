package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 积分消费设置实体类
 * <p>
 * 对应数据库表 ums_integration_consume_setting，配置会员积分在订单消费时的抵扣规则，
 * 包括每元抵扣积分数量、每笔订单最高抵扣比例、积分使用最小单位等，
 * 控制积分作为支付手段的使用策略。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>deductionPerAmount - 每一元需要抵扣的积分数量</li>
 *   <li>maxPercentPerOrder - 每笔订单最高抵用百分比</li>
 *   <li>useUnit - 每次使用积分最小单位（如100积分起用）</li>
 *   <li>couponStatus - 是否可以和优惠券同用：0->不可以；1->可以</li>
 * </ul>
 * </p>
 */
public class UmsIntegrationConsumeSetting implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "每一元需要抵扣的积分数量")
    private Integer deductionPerAmount;
    @ApiModelProperty(value = "每笔订单最高抵用百分比")
    private Integer maxPercentPerOrder;
    @ApiModelProperty(value = "每次使用积分最小单位100")
    private Integer useUnit;
    @ApiModelProperty(value = "是否可以和优惠券同用；0->不可以；1->可以")
    private Integer couponStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDeductionPerAmount() {
        return deductionPerAmount;
    }

    public void setDeductionPerAmount(Integer deductionPerAmount) {
        this.deductionPerAmount = deductionPerAmount;
    }

    public Integer getMaxPercentPerOrder() {
        return maxPercentPerOrder;
    }

    public void setMaxPercentPerOrder(Integer maxPercentPerOrder) {
        this.maxPercentPerOrder = maxPercentPerOrder;
    }

    public Integer getUseUnit() {
        return useUnit;
    }

    public void setUseUnit(Integer useUnit) {
        this.useUnit = useUnit;
    }

    public Integer getCouponStatus() {
        return couponStatus;
    }

    public void setCouponStatus(Integer couponStatus) {
        this.couponStatus = couponStatus;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", deductionPerAmount=").append(deductionPerAmount);
        sb.append(", maxPercentPerOrder=").append(maxPercentPerOrder);
        sb.append(", useUnit=").append(useUnit);
        sb.append(", couponStatus=").append(couponStatus);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}