package com.mtcarpenter.mall.model;

import java.io.Serializable;

/**
 * 会员与标签关联实体类
 * <p>
 * 对应数据库表 ums_member_member_tag_relation，建立会员与标签的多对多关联关系。
 * 会员标签用于用户画像和精准营销，系统可根据会员的消费行为自动打标签，
 * 也可由管理员手动为会员分配标签。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>memberId - 会员ID，关联 UmsMember.id</li>
 *   <li>tagId - 标签ID，关联 UmsMemberTag.id</li>
 * </ul>
 * </p>
 */
public class UmsMemberMemberTagRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long memberId;
    private Long tagId;

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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", memberId=").append(memberId);
        sb.append(", tagId=").append(tagId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}