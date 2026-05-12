package com.mtcarpenter.mall.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 会员（前台用户）实体类
 * <p>
 * 对应数据库表 ums_member，存储商城前台注册用户（C端消费者）的完整信息，
 * 包括账号信息、个人资料、积分与成长值等，是会员体系的核心实体。
 * 会员通过积分和成长值参与会员等级体系，享受不同等级的专属权益。
 * </p>
 * <p>
 * 包含字段：
 * <ul>
 *   <li>id - 主键ID</li>
 *   <li>memberLevelId - 会员等级ID，关联 UmsMemberLevel.id</li>
 *   <li>username - 用户名，登录凭证</li>
 *   <li>password - 密码，加密存储</li>
 *   <li>nickname - 昵称</li>
 *   <li>phone - 手机号码</li>
 *   <li>status - 帐号启用状态：0->禁用；1->启用</li>
 *   <li>createTime - 注册时间</li>
 *   <li>icon - 头像URL</li>
 *   <li>gender - 性别：0->未知；1->男；2->女</li>
 *   <li>birthday - 生日</li>
 *   <li>city - 所在城市</li>
 *   <li>job - 职业</li>
 *   <li>personalizedSignature - 个性签名</li>
 *   <li>sourceType - 用户来源</li>
 *   <li>integration - 当前积分</li>
 *   <li>growth - 当前成长值</li>
 *   <li>luckeyCount - 剩余抽奖次数</li>
 *   <li>historyIntegration - 历史积分总量</li>
 * </ul>
 * </p>
 */
public class UmsMember implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long memberLevelId;
    @ApiModelProperty(value = "用户名")
    private String username;
    @ApiModelProperty(value = "密码")
    private String password;
    @ApiModelProperty(value = "昵称")
    private String nickname;
    @ApiModelProperty(value = "手机号码")
    private String phone;
    @ApiModelProperty(value = "帐号启用状态:0->禁用；1->启用")
    private Integer status;
    @ApiModelProperty(value = "注册时间")
    private Date createTime;
    @ApiModelProperty(value = "头像")
    private String icon;
    @ApiModelProperty(value = "性别：0->未知；1->男；2->女")
    private Integer gender;
    @ApiModelProperty(value = "生日")
    private Date birthday;
    @ApiModelProperty(value = "所做城市")
    private String city;
    @ApiModelProperty(value = "职业")
    private String job;
    @ApiModelProperty(value = "个性签名")
    private String personalizedSignature;
    @ApiModelProperty(value = "用户来源")
    private Integer sourceType;
    @ApiModelProperty(value = "积分")
    private Integer integration;
    @ApiModelProperty(value = "成长值")
    private Integer growth;
    @ApiModelProperty(value = "剩余抽奖次数")
    private Integer luckeyCount;
    @ApiModelProperty(value = "历史积分数量")
    private Integer historyIntegration;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberLevelId() {
        return memberLevelId;
    }

    public void setMemberLevelId(Long memberLevelId) {
        this.memberLevelId = memberLevelId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getPersonalizedSignature() {
        return personalizedSignature;
    }

    public void setPersonalizedSignature(String personalizedSignature) {
        this.personalizedSignature = personalizedSignature;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getIntegration() {
        return integration;
    }

    public void setIntegration(Integer integration) {
        this.integration = integration;
    }

    public Integer getGrowth() {
        return growth;
    }

    public void setGrowth(Integer growth) {
        this.growth = growth;
    }

    public Integer getLuckeyCount() {
        return luckeyCount;
    }

    public void setLuckeyCount(Integer luckeyCount) {
        this.luckeyCount = luckeyCount;
    }

    public Integer getHistoryIntegration() {
        return historyIntegration;
    }

    public void setHistoryIntegration(Integer historyIntegration) {
        this.historyIntegration = historyIntegration;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", memberLevelId=").append(memberLevelId);
        sb.append(", username=").append(username);
        sb.append(", password=").append(password);
        sb.append(", nickname=").append(nickname);
        sb.append(", phone=").append(phone);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", icon=").append(icon);
        sb.append(", gender=").append(gender);
        sb.append(", birthday=").append(birthday);
        sb.append(", city=").append(city);
        sb.append(", job=").append(job);
        sb.append(", personalizedSignature=").append(personalizedSignature);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", integration=").append(integration);
        sb.append(", growth=").append(growth);
        sb.append(", luckeyCount=").append(luckeyCount);
        sb.append(", historyIntegration=").append(historyIntegration);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}