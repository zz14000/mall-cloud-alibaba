package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 后台管理员登录日志实体类
 * <p>
 * 对应数据库表 ums_admin_login_log，记录后台管理员每次登录的详细信息，
 * 用于安全审计、登录行为分析和异常登录检测。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>adminId - 管理员ID，关联 UmsAdmin.id</li>
 *   <li>createTime - 登录时间</li>
 *   <li>ip - 登录IP地址</li>
 *   <li>address - 登录地理位置</li>
 *   <li>userAgent - 浏览器登录类型（User-Agent 信息）</li>
 * </ul>
 * </p>
 */
public class UmsAdminLoginLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long adminId;
    private Date createTime;
    private String ip;
    private String address;
    @ApiModelProperty(value = "浏览器登录类型")
    private String userAgent;

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", adminId=").append(adminId);
        sb.append(", createTime=").append(createTime);
        sb.append(", ip=").append(ip);
        sb.append(", address=").append(address);
        sb.append(", userAgent=").append(userAgent);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}