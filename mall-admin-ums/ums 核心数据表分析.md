# UMS 核心数据表分析

## 一、表结构总览

UMS（User Management System）用户管理系统包含**后台管理员体系**和**前台会员体系**两大模块，共 **25 张核心数据表**。

### 表分类

| 分类 | 表数量 | 说明 |
|------|--------|------|
| 后台管理员相关 | 10 张 | 管理员账号、角色、权限、菜单、资源等 |
| 前台会员相关 | 15 张 | 会员账号、等级、积分、成长值、标签等 |

---

## 二、后台管理员体系

### 2.1 核心实体表

#### 1. 管理员表 - `ums_admin`

**用途**：存储后台管理系统的用户账号信息，是 RBAC 权限模型的核心主体

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| username | VARCHAR | 用户名，登录凭证 |
| password | VARCHAR | 密码，加密存储 |
| icon | VARCHAR | 头像 URL |
| email | VARCHAR | 邮箱地址 |
| nickName | VARCHAR | 昵称 |
| note | VARCHAR | 备注信息 |
| createTime | DATETIME | 账号创建时间 |
| loginTime | DATETIME | 最后登录时间 |
| status | INT | 帐号启用状态：0->禁用；1->启用 |

#### 2. 角色表 - `ums_role`

**用途**：定义后台管理系统的角色信息，是 RBAC 权限模型的核心

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| name | VARCHAR | 角色名称 |
| description | VARCHAR | 角色描述 |
| adminCount | INT | 拥有该角色的后台用户数量 |
| createTime | DATETIME | 创建时间 |
| status | INT | 启用状态：0->禁用；1->启用 |
| sort | INT | 排序 |

#### 3. 菜单表 - `ums_menu`

**用途**：定义后台管理系统的菜单结构，采用树形层级组织

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| parentId | BIGINT | 父级菜单 ID，顶级菜单为 0 |
| createTime | DATETIME | 创建时间 |
| title | VARCHAR | 菜单显示名称 |
| level | INT | 菜单级数 |
| sort | INT | 菜单排序（数值越小越靠前） |
| name | VARCHAR | 前端路由名称 |
| icon | VARCHAR | 前端图标 |
| hidden | INT | 前端是否隐藏：0->显示；1->隐藏 |

#### 4. 权限表 - `ums_permission`

**用途**：定义后台管理系统的权限资源，采用树形层级组织

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| pid | BIGINT | 父级权限 ID，顶级权限为 0 |
| name | VARCHAR | 权限名称 |
| value | VARCHAR | 权限值（如商品添加、商品删除等标识） |
| icon | VARCHAR | 图标 |
| type | INT | 权限类型：0->目录；1->菜单；2->按钮（接口绑定权限） |
| uri | VARCHAR | 前端资源路径 |
| status | INT | 启用状态：0->禁用；1->启用 |
| createTime | DATETIME | 创建时间 |
| sort | INT | 排序 |

#### 5. 资源表 - `ums_resource`

**用途**：定义后台管理系统的 API 资源，用于基于 URL 的接口级权限控制

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| createTime | DATETIME | 创建时间 |
| name | VARCHAR | 资源名称 |
| url | VARCHAR | 资源 URL（如 /admin/product/create） |
| description | VARCHAR | 资源描述 |
| categoryId | BIGINT | 资源分类 ID，关联 UmsResourceCategory.id |

#### 6. 资源分类表 - `ums_resource_category`

**用途**：对后台 API 资源进行分类管理

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| createTime | DATETIME | 创建时间 |
| name | VARCHAR | 分类名称 |
| sort | INT | 排序 |

#### 7. 管理员登录日志表 - `ums_admin_login_log`

**用途**：记录后台管理员每次登录的详细信息，用于安全审计

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| adminId | BIGINT | 管理员 ID，关联 UmsAdmin.id |
| createTime | DATETIME | 登录时间 |
| ip | VARCHAR | 登录 IP 地址 |
| address | VARCHAR | 登录地理位置 |
| userAgent | VARCHAR | 浏览器登录类型（User-Agent 信息） |

### 2.2 关联关系表

#### 1. 管理员与角色关联表 - `ums_admin_role_relation`

**用途**：建立管理员与角色的多对多关联关系

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| adminId | BIGINT | 管理员 ID，关联 UmsAdmin.id |
| roleId | BIGINT | 角色 ID，关联 UmsRole.id |

#### 2. 角色与菜单关联表 - `ums_role_menu_relation`

**用途**：建立角色与菜单的多对多关联关系，控制前端菜单可见性

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| roleId | BIGINT | 角色 ID，关联 UmsRole.id |
| menuId | BIGINT | 菜单 ID，关联 UmsMenu.id |

#### 3. 角色与权限关联表 - `ums_role_permission_relation`

**用途**：建立角色与权限的多对多关联关系

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| roleId | BIGINT | 角色 ID，关联 UmsRole.id |
| permissionId | BIGINT | 权限 ID，关联 UmsPermission.id |

#### 4. 角色与资源关联表 - `ums_role_resource_relation`

**用途**：建立角色与 API 资源的多对多关联关系，控制后端接口访问权限

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| roleId | BIGINT | 角色 ID，关联 UmsRole.id |
| resourceId | BIGINT | 资源 ID，关联 UmsResource.id |

#### 5. 管理员与权限关联表 - `ums_admin_permission_relation`

**用途**：为管理员单独分配"+权限"或"-权限"，实现基于个人的细粒度权限控制

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| adminId | BIGINT | 管理员 ID，关联 UmsAdmin.id |
| permissionId | BIGINT | 权限 ID，关联 UmsPermission.id |
| type | INT | 权限类型：0->+权限（额外增加）；1->-权限（额外移除） |

### 2.3 后台权限体系 ER 图

```
┌─────────────┐       ┌──────────────────────┐       ┌─────────────┐
│ ums_admin   │       │ ums_admin_role_      │       │ ums_role    │
│─────────────│       │ relation             │       │─────────────│
│ id          │◄─────►│ adminId (FK)         │       │ id          │
│ username    │       │ roleId (FK)          │◄─────►│ name        │
│ password    │       └──────────────────────┘       │ description │
│ status      │                                      └─────────────┘
└─────────────┘                                             │
         │                                                  │
         │              ┌──────────────────────┐            │
         │              │ ums_role_menu_       │            │
         │              │ relation             │            │
         │              │─────────────────────│            │
         └─────────────►│ roleId (FK)          │            │
                        │ menuId (FK)          │◄───────────┘
                        └──────────────────────┘
                                   │
                                   ▼
                        ┌──────────────────────┐
                        │ ums_menu             │
                        │─────────────────────│
                        │ id                   │
                        │ parent_id            │
                        │ title                │
                        │ level                │
                        │ name                 │
                        │ icon                 │
                        │ hidden               │
                        └──────────────────────┘

         ┌──────────────────────┐       ┌─────────────┐
         │ ums_role_permission_ │       │ums_permission│
         │ relation             │       │─────────────│
         │─────────────────────│       │ id          │
         │ roleId (FK)         │──────►│ pid         │
         │ permissionId (FK)   │       │ name        │
         └──────────────────────┘       │ value       │
                                        │ type        │
         ┌──────────────────────┐       │ uri         │
         │ ums_role_resource_   │       └─────────────┘
         │ relation             │
         │─────────────────────│       ┌──────────────────────┐
         │ roleId (FK)         │──────►│ ums_resource         │
         │ resourceId (FK)     │       │─────────────────────│
         └──────────────────────┘       │ id                   │
                                        │ url                  │
                                        │ name                 │
                                        │ category_id (FK)     │
                                        └──────────────────────┘
                                                   │
                                                   ▼
                                        ┌──────────────────────┐
                                        │ums_resource_category │
                                        │─────────────────────│
                                        │ id                   │
                                        │ name                 │
                                        └──────────────────────┘
```

---

## 三、前台会员体系

### 3.1 核心实体表

#### 1. 会员表 - `ums_member`

**用途**：存储商城前台注册用户（C 端消费者）的完整信息，是会员体系的核心实体

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberLevelId | BIGINT | 会员等级 ID，关联 UmsMemberLevel.id |
| username | VARCHAR | 用户名，登录凭证 |
| password | VARCHAR | 密码，加密存储 |
| nickname | VARCHAR | 昵称 |
| phone | VARCHAR | 手机号码 |
| status | INT | 帐号启用状态：0->禁用；1->启用 |
| createTime | DATETIME | 注册时间 |
| icon | VARCHAR | 头像 URL |
| gender | INT | 性别：0->未知；1->男；2->女 |
| birthday | DATETIME | 生日 |
| city | VARCHAR | 所在城市 |
| job | VARCHAR | 职业 |
| personalizedSignature | VARCHAR | 个性签名 |
| sourceType | VARCHAR | 用户来源 |
| integration | INT | 当前积分 |
| growth | INT | 当前成长值 |
| luckeyCount | INT | 剩余抽奖次数 |
| historyIntegration | INT | 历史积分总量 |

#### 2. 会员等级表 - `ums_member_level`

**用途**：定义会员等级体系中的各个等级及其对应权益

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| name | VARCHAR | 等级名称 |
| growthPoint | INT | 达到该等级所需成长值 |
| defaultStatus | INT | 是否为默认等级：0->不是；1->是 |
| freeFreightPoint | DECIMAL | 免运费标准 |
| commentGrowthPoint | INT | 每次评价获取的成长值 |
| priviledgeFreeFreight | INT | 是否有免邮特权 |
| priviledgeSignIn | INT | 是否有签到特权 |
| priviledgeComment | INT | 是否有评论获奖励特权 |
| priviledgePromotion | INT | 是否有专享活动特权 |
| priviledgeMemberPrice | INT | 是否有会员价格特权 |
| priviledgeBirthday | INT | 是否有生日特权 |
| note | VARCHAR | 备注信息 |

#### 3. 会员统计信息表 - `ums_member_statistics_info`

**用途**：汇总统计会员的各项业务数据，用于会员画像构建和数据分析

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| consumeAmount | DECIMAL | 累计消费金额 |
| orderCount | INT | 订单数量 |
| couponCount | INT | 优惠券数量 |
| commentCount | INT | 评价数 |
| returnOrderCount | INT | 退货数量 |
| loginCount | INT | 登录次数 |
| attendCount | INT | 关注数量 |
| fansCount | INT | 粉丝数量 |
| collectProductCount | INT | 收藏商品数量 |
| collectSubjectCount | INT | 收藏专题数量 |
| collectTopicCount | INT | 收藏话题数量 |
| collectCommentCount | INT | 收藏评论数量 |
| inviteFriendCount | INT | 邀请好友数量 |
| recentOrderTime | DATETIME | 最后一次下订单时间 |

#### 4. 会员标签表 - `ums_member_tag`

**用途**：定义会员标签及其自动打标签规则

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| name | VARCHAR | 标签名称 |
| finishOrderCount | INT | 自动打标签完成订单数量阈值 |
| finishOrderAmount | DECIMAL | 自动打标签完成订单金额阈值 |

#### 5. 会员收货地址表 - `ums_member_receive_address`

**用途**：存储会员的收货地址信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| name | VARCHAR | 收货人名称 |
| phoneNumber | VARCHAR | 收货人手机号 |
| defaultStatus | INT | 是否为默认地址 |
| postCode | VARCHAR | 邮政编码 |
| province | VARCHAR | 省份/直辖市 |
| city | VARCHAR | 城市 |
| region | VARCHAR | 区 |
| detailAddress | VARCHAR | 详细地址（街道） |

#### 6. 会员登录日志表 - `ums_member_login_log`

**用途**：记录前台会员每次登录的详细信息

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| createTime | DATETIME | 登录时间 |
| ip | VARCHAR | 登录 IP 地址 |
| city | VARCHAR | 登录城市 |
| loginType | INT | 登录类型：0->PC；1->android；2->ios；3->小程序 |
| province | VARCHAR | 登录省份 |

### 3.2 积分与成长值体系

#### 1. 积分变更历史表 - `ums_integration_change_history`

**用途**：记录会员积分的每次变动详情，用于积分流水追踪和对账

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| createTime | DATETIME | 变更时间 |
| changeType | INT | 变更类型：0->增加；1->减少 |
| changeCount | INT | 积分变更数量 |
| operateMan | VARCHAR | 操作人员 |
| operateNote | VARCHAR | 操作备注 |
| sourceType | INT | 积分来源：0->购物；1->管理员修改 |

#### 2. 成长值变更历史表 - `ums_growth_change_history`

**用途**：记录会员成长值的每次变动详情，用于成长值流水追踪和等级升降级计算

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| createTime | DATETIME | 变更时间 |
| changeType | INT | 变更类型：0->增加；1->减少 |
| changeCount | INT | 成长值变更数量 |
| operateMan | VARCHAR | 操作人员 |
| operateNote | VARCHAR | 操作备注 |
| sourceType | INT | 成长值来源：0->购物；1->管理员修改 |

#### 3. 会员规则设置表 - `ums_member_rule_setting`

**用途**：配置会员积分和成长值的获取规则

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| continueSignDay | INT | 连续签到天数 |
| continueSignPoint | INT | 连续签到赠送数量 |
| consumePerPoint | DECIMAL | 每消费多少元获取 1 个点 |
| lowOrderAmount | DECIMAL | 最低获取点数的订单金额 |
| maxPointPerOrder | INT | 每笔订单最高获取点数 |
| type | INT | 类型：0->积分规则；1->成长值规则 |

#### 4. 积分消费设置表 - `ums_integration_consume_setting`

**用途**：配置会员积分在订单消费时的抵扣规则

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| deductionPerAmount | INT | 每一元需要抵扣的积分数量 |
| maxPercentPerOrder | INT | 每笔订单最高抵用百分比 |
| useUnit | INT | 每次使用积分最小单位（如 100 积分起用） |
| couponStatus | INT | 是否可以和优惠券同用：0->不可以；1->可以 |

### 3.3 关联关系表

#### 1. 会员与标签关联表 - `ums_member_member_tag_relation`

**用途**：建立会员与标签的多对多关联关系

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键 ID |
| memberId | BIGINT | 会员 ID，关联 UmsMember.id |
| tagId | BIGINT | 标签 ID，关联 UmsMemberTag.id |

### 3.4 会员体系 ER 图

```
┌─────────────────┐       ┌──────────────────────┐       ┌─────────────────┐
│ ums_member      │       │ ums_member_level     │       │ums_member_tag   │
│─────────────────│       │─────────────────────│       │─────────────────│
│ id              │◄─────►│ memberLevelId (FK)  │       │ id              │
│ username        │       │ name                │       │ name            │
│ password        │       │ growthPoint         │       │ finishOrderCount│
│ nickname        │       │ priviledge*         │       │ finishOrderAmt  │
│ phone           │       └──────────────────────┘       └─────────────────┘
│ status          │                  ▲                            │
│ integration     │                  │                            │
│ growth          │                  │                            │
│ historyInteg    │                  │                            │
└─────────────────┘                  │                            │
         │                           │                            │
         │              ┌──────────────────────┐                  │
         │              │ums_member_member_tag_│                  │
         │              │ relation             │◄─────────────────┘
         │              │─────────────────────│
         └─────────────►│ memberId (FK)        │
                        │ tagId (FK)           │
                        └──────────────────────┘

         ┌──────────────────────┐       ┌──────────────────────┐
         │ums_integration_change│       │ums_growth_change_    │
         │ _history             │       │ history              │
         │─────────────────────│       │─────────────────────│
         │ id                  │       │ id                  │
         │ memberId (FK)       │       │ memberId (FK)       │
         │ changeType          │       │ changeType          │
         │ changeCount         │       │ changeCount         │
         │ operateMan          │       │ operateMan          │
         │ operateNote         │       │ operateNote         │
         │ sourceType          │       │ sourceType          │
         └──────────────────────┘       └──────────────────────┘

         ┌──────────────────────┐       ┌──────────────────────┐
         │ums_member_rule_setting│      │ums_integration_consume│
         │─────────────────────│       │ _setting              │
         │ id                  │       │─────────────────────│
         │ continueSignDay     │       │ deductionPerAmount  │
         │ continueSignPoint   │       │ maxPercentPerOrder  │
         │ consumePerPoint     │       │ useUnit             │
         │ lowOrderAmount      │       │ couponStatus        │
         │ maxPointPerOrder    │       └──────────────────────┘
         │ type                │
         └──────────────────────┘

         ┌──────────────────────┐       ┌──────────────────────┐
         │ums_member_statistics │       │ums_member_receive_   │
         │ _info                │       │ address              │
         │─────────────────────│       │─────────────────────│
         │ id                  │       │ id                  │
         │ memberId (FK)       │       │ memberId (FK)       │
         │ consumeAmount       │       │ name                │
         │ orderCount          │       │ phoneNumber         │
         │ couponCount         │       │ defaultStatus       │
         │ commentCount        │       │ province            │
         │ loginCount          │       │ city                │
         │ fansCount           │       │ region              │
         │ collect*            │       │ detailAddress       │
         │ recentOrderTime     │       └──────────────────────┘
         └──────────────────────┘
```

---

## 四、核心表关联关系总结

### 4.1 后台管理员权限链路

```
管理员 (ums_admin)
    ↓ (通过 ums_admin_role_relation)
角色 (ums_role)
    ├─→ 菜单 (ums_menu) [通过 ums_role_menu_relation]
    ├─→ 权限 (ums_permission) [通过 ums_role_permission_relation]
    └─→ 资源 (ums_resource) [通过 ums_role_resource_relation]
```

**权限控制流程**：
1. 管理员登录 → 查询其关联的所有角色
2. 聚合角色对应的菜单权限 → 控制前端菜单显示
3. 聚合角色对应的权限（按钮级）→ 控制前端操作按钮
4. 聚合角色对应的资源（API 接口）→ 控制后端接口访问

### 4.2 前台会员体系链路

```
会员 (ums_member)
    ├─→ 会员等级 (ums_member_level) [通过 memberLevelId]
    ├─→ 会员标签 (ums_member_tag) [通过 ums_member_member_tag_relation]
    ├─→ 会员统计 (ums_member_statistics_info) [通过 memberId]
    ├─→ 收货地址 (ums_member_receive_address) [通过 memberId]
    ├─→ 登录日志 (ums_member_login_log) [通过 memberId]
    ├─→ 积分历史 (ums_integration_change_history) [通过 memberId]
    └─→ 成长值历史 (ums_growth_change_history) [通过 memberId]
```

**会员运营流程**：
1. 会员注册 → 自动分配默认等级
2. 会员消费/签到 → 增加积分和成长值 → 记录变更历史
3. 成长值达到阈值 → 自动升级会员等级
4. 消费行为分析 → 自动打标签 → 精准营销

---

## 五、关键字段说明

### 5.1 状态字段规范

| 字段名 | 取值 | 说明 |
|--------|------|------|
| status | 0 | 禁用 |
| status | 1 | 启用 |
| hidden | 0 | 显示 |
| hidden | 1 | 隐藏 |
| defaultStatus | 0 | 否 |
| defaultStatus | 1 | 是 |
| gender | 0 | 未知 |
| gender | 1 | 男 |
| gender | 2 | 女 |

### 5.2 类型字段规范

| 表名 | 字段名 | 取值 | 说明 |
|------|--------|------|------|
| ums_permission | type | 0 | 目录 |
| ums_permission | type | 1 | 菜单 |
| ums_permission | type | 2 | 按钮（接口绑定权限） |
| ums_member_rule_setting | type | 0 | 积分规则 |
| ums_member_rule_setting | type | 1 | 成长值规则 |
| ums_integration_change_history | changeType/sourceType | 0 | 增加/购物 |
| ums_integration_change_history | changeType/sourceType | 1 | 减少/管理员修改 |
| ums_growth_change_history | changeType/sourceType | 0 | 增加/购物 |
| ums_growth_change_history | changeType/sourceType | 1 | 减少/管理员修改 |
| ums_member_login_log | loginType | 0 | PC |
| ums_member_login_log | loginType | 1 | android |
| ums_member_login_log | loginType | 2 | ios |
| ums_member_login_log | loginType | 3 | 小程序 |

---

## 六、数据表清单

| 序号 | 表名 | 中文名称 | 所属分类 |
|------|------|----------|----------|
| 1 | ums_admin | 后台管理员表 | 后台管理 |
| 2 | ums_admin_login_log | 后台管理员登录日志表 | 后台管理 |
| 3 | ums_admin_role_relation | 后台管理员与角色关联表 | 后台管理 |
| 4 | ums_admin_permission_relation | 后台管理员与权限关联表 | 后台管理 |
| 5 | ums_role | 后台角色表 | 后台管理 |
| 6 | ums_menu | 后台菜单表 | 后台管理 |
| 7 | ums_role_menu_relation | 角色与菜单关联表 | 后台管理 |
| 8 | ums_permission | 后台权限表 | 后台管理 |
| 9 | ums_role_permission_relation | 角色与权限关联表 | 后台管理 |
| 10 | ums_resource | 后台资源表 | 后台管理 |
| 11 | ums_resource_category | 后台资源分类表 | 后台管理 |
| 12 | ums_role_resource_relation | 角色与资源关联表 | 后台管理 |
| 13 | ums_member | 会员表 | 会员体系 |
| 14 | ums_member_level | 会员等级表 | 会员体系 |
| 15 | ums_member_statistics_info | 会员统计信息表 | 会员体系 |
| 16 | ums_member_tag | 会员标签表 | 会员体系 |
| 17 | ums_member_member_tag_relation | 会员与标签关联表 | 会员体系 |
| 18 | ums_member_receive_address | 会员收货地址表 | 会员体系 |
| 19 | ums_member_login_log | 会员登录日志表 | 会员体系 |
| 20 | ums_integration_change_history | 积分变更历史表 | 会员体系 |
| 21 | ums_growth_change_history | 成长值变更历史表 | 会员体系 |
| 22 | ums_member_rule_setting | 会员规则设置表 | 会员体系 |
| 23 | ums_integration_consume_setting | 积分消费设置表 | 会员体系 |
