package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员规则设置实体类
 * <p>
 * 对应数据库表 ums_member_rule_setting，配置会员积分和成长值的获取规则，
 * 包括连续签到奖励规则、消费获取积分规则等，控制会员体系中积分和成长值的发放策略。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>continueSignDay - 连续签到天数</li>
 *   <li>continueSignPoint - 连续签到赠送数量</li>
 *   <li>consumePerPoint - 每消费多少元获取1个点</li>
 *   <li>lowOrderAmount - 最低获取点数的订单金额</li>
 *   <li>maxPointPerOrder - 每笔订单最高获取点数</li>
 *   <li>type - 类型：0->积分规则；1->成长值规则</li>
 * </ul>
 * </p>
 */
public class UmsMemberRuleSetting implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "连续签到天数")
    private Integer continueSignDay;
    @ApiModelProperty(value = "连续签到赠送数量")
    private Integer continueSignPoint;
    @ApiModelProperty(value = "每消费多少元获取1个点")
    private BigDecimal consumePerPoint;
    @ApiModelProperty(value = "最低获取点数的订单金额")
    private BigDecimal lowOrderAmount;
    @ApiModelProperty(value = "每笔订单最高获取点数")
    private Integer maxPointPerOrder;
    @ApiModelProperty(value = "类型：0->积分规则；1->成长值规则")
    private Integer type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getContinueSignDay() {
        return continueSignDay;
    }

    public void setContinueSignDay(Integer continueSignDay) {
        this.continueSignDay = continueSignDay;
    }

    public Integer getContinueSignPoint() {
        return continueSignPoint;
    }

    public void setContinueSignPoint(Integer continueSignPoint) {
        this.continueSignPoint = continueSignPoint;
    }

    public BigDecimal getConsumePerPoint() {
        return consumePerPoint;
    }

    public void setConsumePerPoint(BigDecimal consumePerPoint) {
        this.consumePerPoint = consumePerPoint;
    }

    public BigDecimal getLowOrderAmount() {
        return lowOrderAmount;
    }

    public void setLowOrderAmount(BigDecimal lowOrderAmount) {
        this.lowOrderAmount = lowOrderAmount;
    }

    public Integer getMaxPointPerOrder() {
        return maxPointPerOrder;
    }

    public void setMaxPointPerOrder(Integer maxPointPerOrder) {
        this.maxPointPerOrder = maxPointPerOrder;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", continueSignDay=").append(continueSignDay);
        sb.append(", continueSignPoint=").append(continueSignPoint);
        sb.append(", consumePerPoint=").append(consumePerPoint);
        sb.append(", lowOrderAmount=").append(lowOrderAmount);
        sb.append(", maxPointPerOrder=").append(maxPointPerOrder);
        sb.append(", type=").append(type);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}