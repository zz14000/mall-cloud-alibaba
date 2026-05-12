package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台菜单实体类
 * <p>
 * 对应数据库表 ums_menu，定义后台管理系统的菜单结构，采用树形层级组织。
 * 菜单与角色关联，控制不同角色在后台管理界面中可见的菜单项，
 * 实现前端页面的权限控制（菜单级权限）。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>parentId - 父级菜单ID，顶级菜单为0</li>
 *   <li>createTime - 创建时间</li>
 *   <li>title - 菜单显示名称</li>
 *   <li>level - 菜单级数</li>
 *   <li>sort - 菜单排序（数值越小越靠前）</li>
 *   <li>name - 前端路由名称</li>
 *   <li>icon - 前端图标</li>
 *   <li>hidden - 前端是否隐藏：0->显示；1->隐藏</li>
 * </ul>
 * </p>
 */
public class UmsMenu implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    @ApiModelProperty(value = "父级ID")
    private Long parentId;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "菜单名称")
    private String title;
    @ApiModelProperty(value = "菜单级数")
    private Integer level;
    @ApiModelProperty(value = "菜单排序")
    private Integer sort;
    @ApiModelProperty(value = "前端名称")
    private String name;
    @ApiModelProperty(value = "前端图标")
    private String icon;
    @ApiModelProperty(value = "前端隐藏")
    private Integer hidden;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getHidden() {
        return hidden;
    }

    public void setHidden(Integer hidden) {
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", parentId=").append(parentId);
        sb.append(", createTime=").append(createTime);
        sb.append(", title=").append(title);
        sb.append(", level=").append(level);
        sb.append(", sort=").append(sort);
        sb.append(", name=").append(name);
        sb.append(", icon=").append(icon);
        sb.append(", hidden=").append(hidden);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}