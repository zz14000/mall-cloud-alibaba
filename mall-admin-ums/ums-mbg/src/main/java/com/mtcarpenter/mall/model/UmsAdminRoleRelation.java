package com.mtcarpenter.mall.model;

import java.io.Serializable;

/**
 * 后台管理员与角色关联实体类
 * <p>
 * 对应数据库表 ums_admin_role_relation，建立管理员与角色的多对多关联关系。
 * 一个管理员可以拥有多个角色，一个角色也可以分配给多个管理员，
 * 是 RBAC 权限模型中连接用户和角色的桥梁表。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>adminId - 管理员ID，关联 UmsAdmin.id</li>
 *   <li>roleId - 角色ID，关联 UmsRole.id</li>
 * </ul>
 * </p>
 */
public class UmsAdminRoleRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long adminId;
    private Long roleId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", adminId=").append(adminId);
        sb.append(", roleId=").append(roleId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}