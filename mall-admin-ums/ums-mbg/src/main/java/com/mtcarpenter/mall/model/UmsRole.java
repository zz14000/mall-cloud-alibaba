package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台角色实体类
 * <p>
 * 对应数据库表 ums_role，定义后台管理系统的角色信息，是 RBAC 权限模型的核心。
 * 角色作为管理员与权限/菜单/资源之间的桥梁，管理员通过分配角色来获取对应的权限集合，
 * 实现权限的批量管理和灵活配置。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>name - 角色名称</li>
 *   <li>description - 角色描述</li>
 *   <li>adminCount - 拥有该角色的后台用户数量</li>
 *   <li>createTime - 创建时间</li>
 *   <li>status - 启用状态：0->禁用；1->启用</li>
 *   <li>sort - 排序</li>
 * </ul>
 * </p>
 */
public class UmsRole implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "后台用户数量")
    private Integer adminCount;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "启用状态：0->禁用；1->启用")
    private Integer status;
    private Integer sort;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(Integer adminCount) {
        this.adminCount = adminCount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", adminCount=").append(adminCount);
        sb.append(", createTime=").append(createTime);
        sb.append(", status=").append(status);
        sb.append(", sort=").append(sort);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}