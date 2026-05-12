package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 会员任务实体类
 * <p>
 * 对应数据库表 ums_member_task，定义会员可完成的任务及其奖励规则，
 * 包括新手任务和日常任务。会员完成任务后可获得成长值和积分奖励，
 * 用于提升会员活跃度和留存率。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>name - 任务名称</li>
 *   <li>growth - 完成任务赠送的成长值</li>
 *   <li>intergration - 完成任务赠送的积分</li>
 *   <li>type - 任务类型：0->新手任务；1->日常任务</li>
 * </ul>
 * </p>
 */
public class UmsMemberTask implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    @ApiModelProperty(value = "赠送成长值")
    private Integer growth;
    @ApiModelProperty(value = "赠送积分")
    private Integer intergration;
    @ApiModelProperty(value = "任务类型：0->新手任务；1->日常任务")
    private Integer type;

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

    public Integer getGrowth() {
        return growth;
    }

    public void setGrowth(Integer growth) {
        this.growth = growth;
    }

    public Integer getIntergration() {
        return intergration;
    }

    public void setIntergration(Integer intergration) {
        this.intergration = intergration;
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
        sb.append(", name=").append(name);
        sb.append(", growth=").append(growth);
        sb.append(", intergration=").append(intergration);
        sb.append(", type=").append(type);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}