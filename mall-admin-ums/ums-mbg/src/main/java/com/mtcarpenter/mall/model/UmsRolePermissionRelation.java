package com.mtcarpenter.mall.model;

import java.io.Serializable;

/**
 * 角色与权限关联实体类
 * <p>
 * 对应数据库表 ums_role_permission_relation，建立角色与权限的多对多关联关系。
 * 通过此关联，为角色分配目录、菜单、按钮等不同层级的权限，
 * 拥有该角色的管理员自动继承这些权限。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>roleId - 角色ID，关联 UmsRole.id</li>
 *   <li>permissionId - 权限ID，关联 UmsPermission.id</li>
 * </ul>
 * </p>
 */
public class UmsRolePermissionRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long roleId;
    private Long permissionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", roleId=").append(roleId);
        sb.append(", permissionId=").append(permissionId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}