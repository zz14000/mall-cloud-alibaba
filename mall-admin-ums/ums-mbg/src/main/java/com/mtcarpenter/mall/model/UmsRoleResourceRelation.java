package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 角色与资源关联实体类
 * <p>
 * 对应数据库表 ums_role_resource_relation，建立角色与 API 资源的多对多关联关系。
 * 通过此关联，控制拥有该角色的管理员可访问的后端 API 接口，
 * 在网关或拦截器层面实现接口级别的动态权限控制。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>roleId - 角色ID，关联 UmsRole.id</li>
 *   <li>resourceId - 资源ID，关联 UmsResource.id</li>
 * </ul>
 * </p>
 */
public class UmsRoleResourceRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "角色ID")
    private Long roleId;
    @ApiModelProperty(value = "资源ID")
    private Long resourceId;

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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", roleId=").append(roleId);
        sb.append(", resourceId=").append(resourceId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}