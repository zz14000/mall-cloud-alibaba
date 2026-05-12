package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台资源实体类
 * <p>
 * 对应数据库表 ums_resource，定义后台管理系统的 API 资源，用于基于 URL 的接口级权限控制。
 * 与 UmsPermission 的按钮级权限不同，UmsResource 直接对应后端 API 接口的 URL 路径，
 * 通过角色与资源的关联关系，在网关或拦截器层面控制接口的访问权限。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>createTime - 创建时间</li>
 *   <li>name - 资源名称</li>
 *   <li>url - 资源URL（如 /admin/product/create）</li>
 *   <li>description - 资源描述</li>
 *   <li>categoryId - 资源分类ID，关联 UmsResourceCategory.id</li>
 * </ul>
 * </p>
 */
public class UmsResource implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "资源名称")
    private String name;
    @ApiModelProperty(value = "资源URL")
    private String url;
    @ApiModelProperty(value = "描述")
    private String description;
    @ApiModelProperty(value = "资源分类ID")
    private Long categoryId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", createTime=").append(createTime);
        sb.append(", name=").append(name);
        sb.append(", url=").append(url);
        sb.append(", description=").append(description);
        sb.append(", categoryId=").append(categoryId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}