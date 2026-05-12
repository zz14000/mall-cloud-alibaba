package com.mtcarpenter.mall.model;


import java.io.Serializable;

/**
 * 后台管理员与权限关联实体类
 * <p>
 * 对应数据库表 ums_admin_permission_relation，用于为管理员单独分配"+权限"或"-权限"，
 * 实现基于个人的细粒度权限控制。当角色权限不足以满足需求时，可通过此表对特定管理员
 * 增加或移除个别权限，而不影响角色下其他管理员。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>adminId - 管理员ID，关联 UmsAdmin.id</li>
 *   <li>permissionId - 权限ID，关联 UmsPermission.id</li>
 *   <li>type - 权限类型：0->+权限（额外增加）；1->-权限（额外移除）</li>
 * </ul>
 * </p>
 */
public class UmsAdminPermissionRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long adminId;
    private Long permissionId;
    private Integer type;

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

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
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
        sb.append(", adminId=").append(adminId);
        sb.append(", permissionId=").append(permissionId);
        sb.append(", type=").append(type);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}