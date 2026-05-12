package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 会员成长值变更历史实体类
 * <p>
 * 对应数据库表 ums_growth_change_history，记录会员成长值的每次变动详情，
 * 包括购物获取成长值、管理员手动调整等场景，用于成长值流水追踪和会员等级升降级计算。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>memberId - 会员ID，关联 UmsMember.id</li>
 *   <li>createTime - 变更时间</li>
 *   <li>changeType - 变更类型：0->增加；1->减少</li>
 *   <li>changeCount - 成长值变更数量</li>
 *   <li>operateMan - 操作人员</li>
 *   <li>operateNote - 操作备注</li>
 *   <li>sourceType - 成长值来源：0->购物；1->管理员修改</li>
 * </ul>
 * </p>
 */
public class UmsGrowthChangeHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long memberId;
    private Date createTime;
    @ApiModelProperty(value = "改变类型：0->增加；1->减少")
    private Integer changeType;
    @ApiModelProperty(value = "积分改变数量")
    private Integer changeCount;
    @ApiModelProperty(value = "操作人员")
    private String operateMan;
    @ApiModelProperty(value = "操作备注")
    private String operateNote;
    @ApiModelProperty(value = "积分来源：0->购物；1->管理员修改")
    private Integer sourceType;

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getChangeType() {
        return changeType;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public Integer getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(Integer changeCount) {
        this.changeCount = changeCount;
    }

    public String getOperateMan() {
        return operateMan;
    }

    public void setOperateMan(String operateMan) {
        this.operateMan = operateMan;
    }

    public String getOperateNote() {
        return operateNote;
    }

    public void setOperateNote(String operateNote) {
        this.operateNote = operateNote;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", memberId=").append(memberId);
        sb.append(", createTime=").append(createTime);
        sb.append(", changeType=").append(changeType);
        sb.append(", changeCount=").append(changeCount);
        sb.append(", operateMan=").append(operateMan);
        sb.append(", operateNote=").append(operateNote);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}