package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台权限实体类
 * <p>
 * 对应数据库表 ums_permission，定义后台管理系统的权限资源，采用树形层级组织。
 * 权限分为三种类型：目录、菜单、按钮（接口绑定权限），覆盖从前端页面展示到后端接口调用的
 * 全链路权限控制。按钮级权限绑定到具体 API 接口，实现最细粒度的操作权限控制。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>pid - 父级权限ID，顶级权限为0</li>
 *   <li>name - 权限名称</li>
 *   <li>value - 权限值（如商品添加、商品删除等标识）</li>
 *   <li>icon - 图标</li>
 *   <li>type - 权限类型：0->目录；1->菜单；2->按钮（接口绑定权限）</li>
 *   <li>uri - 前端资源路径</li>
 *   <li>status - 启用状态：0->禁用；1->启用</li>
 *   <li>createTime - 创建时间</li>
 *   <li>sort - 排序</li>
 * </ul>
 * </p>
 */
public class UmsPermission implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "父级权限id")
    private Long pid;
    @ApiModelProperty(value = "名称")
    private String name;
    @ApiModelProperty(value = "权限值")
    private String value;
    @ApiModelProperty(value = "图标")
    private String icon;
    @ApiModelProperty(value = "权限类型：0->目录；1->菜单；2->按钮（接口绑定权限）")
    private Integer type;
    @ApiModelProperty(value = "前端资源路径")
    private String uri;
    @ApiModelProperty(value = "启用状态；0->禁用；1->启用")
    private Integer status;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "排序")
    private Integer sort;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPid() {
        return pid;
    }

    public void setPid(Long pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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
        sb.append(", pid=").append(pid);
        sb.append(", name=").append(name);
        sb.append(", value=").append(value);
        sb.append(", icon=").append(icon);
        sb.append(", type=").append(type);
        sb.append(", uri=").append(uri);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", sort=").append(sort);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}