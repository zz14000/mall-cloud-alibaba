# SMS 核心数据表分析

## 一、数据表总览

SMS 模块主要包含以下核心数据表，分为三大业务板块：

### 1. 优惠券管理板块
- `sms_coupon` - 优惠券主表
- `sms_coupon_product_relation` - 优惠券与商品关系表
- `sms_coupon_product_category_relation` - 优惠券与商品分类关系表
- `sms_coupon_history` - 优惠券领取/使用历史记录表

### 2. 限时抢购（秒杀）板块
- `sms_flash_promotion` - 限时购活动表

- `sms_flash_promotion_session` - 限时购场次表

- `sms_flash_promotion_product_relation` - 限时购与商品关系表

- `sms_flash_promotion_log` - 限时购会员订阅日志表

  **限时购活动表与限时购场次表之间没有关联关系，相互独立，限时购与商品关系表中包含活动id和场次id，来进行关联**

### 3. 首页推荐板块
- `sms_home_advertise` - 首页广告轮播表
- `sms_home_brand` - 首页品牌推荐表
- `sms_home_new_product` - 首页新品推荐表
- `sms_home_recommend_product` - 首页人气推荐商品表
- `sms_home_recommend_subject` - 首页推荐专题表

---

## 二、数据表详细结构

### 2.1 优惠券管理板块

#### 2.1.1 sms_coupon - 优惠券主表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| type | INTEGER | 优惠券类型 | 0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券 |
| name | VARCHAR | 优惠券名称 | - |
| platform | INTEGER | 使用平台 | 0->全部；1->移动；2->PC |
| count | INTEGER | 数量 | 当前可用数量 |
| amount | DECIMAL | 优惠金额 | 减免金额 |
| per_limit | INTEGER | 每人限领张数 | 限制每个用户领取数量 |
| min_point | DECIMAL | 使用门槛 | 0 表示无门槛，订单金额达到此值可用 |
| start_time | TIMESTAMP | 有效期开始时间 | 优惠券可使用开始时间 |
| end_time | TIMESTAMP | 有效期结束时间 | 优惠券可使用结束时间 |
| use_type | INTEGER | 使用类型 | 0->全场通用；1->指定分类；2->指定商品 |
| note | VARCHAR | 备注 | 备注说明 |
| publish_count | INTEGER | 发行数量 | 计划发放总数量 |
| use_count | INTEGER | 已使用数量 | 已被使用的数量 |
| receive_count | INTEGER | 领取数量 | 已被领取的数量 |
| enable_time | TIMESTAMP | 可领取日期 | 优惠券开始发放时间 |
| code | VARCHAR | 优惠码 | 优惠券编码 |
| member_level | INTEGER | 可领取的会员类型 | 0->不限制；其他->具体会员等级 |

**业务逻辑：**
- `count` = `publish_count` - 初始状态，随着领取和使用递减
- `use_type` 决定关联表：
  - use_type=0：无需关联表，全场通用
  - use_type=1：关联 `sms_coupon_product_category_relation`
  - use_type=2：关联 `sms_coupon_product_relation`

---

#### 2.1.2 sms_coupon_product_relation - 优惠券与商品关系表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| coupon_id | BIGINT | 优惠券 ID | 外键，关联 sms_coupon.id |
| product_id | BIGINT | 商品 ID | 关联商品表主键 |
| product_name | VARCHAR | 商品名称 | 冗余字段，方便查询 |
| product_sn | VARCHAR | 商品编码 | 冗余字段，商品编号 |

**关联关系：**
- `coupon_id` → `sms_coupon.id` (多对一)
- `product_id` → 商品表主键 (多对一)

**业务场景：**
当优惠券的 `use_type=2`（指定商品）时，通过此表建立优惠券与商品的关联关系。

---

#### 2.1.3 sms_coupon_product_category_relation - 优惠券与商品分类关系表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| coupon_id | BIGINT | 优惠券 ID | 外键，关联 sms_coupon.id |
| product_category_id | BIGINT | 商品分类 ID | 关联商品分类表主键 |
| product_category_name | VARCHAR | 商品分类名称 | 冗余字段 |
| parent_category_name | VARCHAR | 父分类名称 | 冗余字段，一级分类名称 |

**关联关系：**
- `coupon_id` → `sms_coupon.id` (多对一)
- `product_category_id` → 商品分类表主键 (多对一)

**业务场景：**
当优惠券的 `use_type=1`（指定分类）时，通过此表建立优惠券与商品分类的关联关系。

---

#### 2.1.4 sms_coupon_history - 优惠券领取/使用历史记录表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| coupon_id | BIGINT | 优惠券 ID | 外键，关联 sms_coupon.id |
| member_id | BIGINT | 会员 ID | 关联会员表主键 |
| coupon_code | VARCHAR | 优惠码 | 优惠券的唯一码，核销用 |
| member_nickname | VARCHAR | 领取人昵称 | 冗余字段 |
| get_type | INTEGER | 获取类型 | 0->后台赠送；1->主动获取 |
| create_time | TIMESTAMP | 领取时间 | 用户领取时间 |
| use_status | INTEGER | 使用状态 | 0->未使用；1->已使用；2->已过期 |
| use_time | TIMESTAMP | 使用时间 | 核销时间 |
| order_id | BIGINT | 订单 ID | 关联订单表，使用优惠券的订单 |
| order_sn | VARCHAR | 订单号码 | 冗余字段，订单编号 |

**关联关系：**
- `coupon_id` → `sms_coupon.id` (多对一)
- `member_id` → 会员表主键 (多对一)
- `order_id` → 订单表主键 (多对一，可选)

**业务逻辑：**
- 用户领取优惠券后生成一条记录，`use_status=0`
- 使用优惠券后更新 `use_status=1`，并记录 `use_time` 和 `order_id`
- 优惠券过期后更新 `use_status=2`

---

### 2.2 限时抢购（秒杀）板块

#### 2.2.1 sms_flash_promotion - 限时购活动表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| title | VARCHAR | 活动标题 | 如"双 11 秒杀"、"618 大促" |
| start_date | DATE | 开始日期 | 活动整体开始日期 |
| end_date | DATE | 结束日期 | 活动整体结束日期 |
| status | INTEGER | 上下线状态 | 0->下线；1->上线 |
| create_time | TIMESTAMP | 创建时间 | 活动创建时间 |

**业务逻辑：**
- 一个限时购活动包含多个场次（`sms_flash_promotion_session`）
- 通过 `status` 控制活动是否可用

---

#### 2.2.2 sms_flash_promotion_session - 限时购场次表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| name | VARCHAR | 场次名称 | 如"上午场"、"下午场"、"晚间场" |
| start_time | TIME | 每日开始时间 | 场次开始时间点 |
| end_time | TIME | 每日结束时间 | 场次结束时间点 |
| status | INTEGER | 启用状态 | 0->不启用；1->启用 |
| create_time | TIMESTAMP | 创建时间 | 场次创建时间 |

**业务逻辑：**
- 场次是每天重复的时间段
- 一个活动（`sms_flash_promotion`）可以包含多个场次
- 通过 `status` 控制场次是否启用

---

#### 2.2.3 sms_flash_promotion_product_relation - 限时购与商品关系表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| flash_promotion_id | BIGINT | 限时购活动 ID | 外键，关联 sms_flash_promotion.id |
| flash_promotion_session_id | BIGINT | 限时购场次 ID | 外键，关联 sms_flash_promotion_session.id |
| product_id | BIGINT | 商品 ID | 关联商品表主键 |
| flash_promotion_price | DECIMAL | 限时购价格 | 秒杀价格 |
| flash_promotion_count | INTEGER | 限时购数量 | 秒杀库存数量 |
| flash_promotion_limit | INTEGER | 每人限购数量 | 每个用户限购数量 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**关联关系：**
- `flash_promotion_id` → `sms_flash_promotion.id` (多对一)
- `flash_promotion_session_id` → `sms_flash_promotion_session.id` (多对一)
- `product_id` → 商品表主键 (多对一)

**业务逻辑：**
- 建立活动、场次、商品三者之间的关系
- 通过 `flash_promotion_price` 设置秒杀价
- 通过 `flash_promotion_count` 控制秒杀库存
- 通过 `flash_promotion_limit` 防止恶意刷单

---

#### 2.2.4 sms_flash_promotion_log - 限时购会员订阅日志表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | INTEGER | 主键 ID | 自增 |
| member_id | INTEGER | 会员 ID | 关联会员表主键 |
| product_id | BIGINT | 商品 ID | 关联商品表主键 |
| member_phone | VARCHAR | 会员手机号 | 用于发送通知 |
| product_name | VARCHAR | 商品名称 | 冗余字段 |
| subscribe_time | TIMESTAMP | 会员订阅时间 | 用户订阅提醒时间 |
| send_time | TIMESTAMP | 发送时间 | 发送通知时间 |

**关联关系：**
- `member_id` → 会员表主键 (多对一)
- `product_id` → 商品表主键 (多对一)

**业务场景：**
- 用户订阅秒杀提醒，开售前发送短信/消息通知
- 记录用户感兴趣的秒杀商品

---

### 2.3 首页推荐板块

#### 2.3.1 sms_home_advertise - 首页广告轮播表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| name | VARCHAR | 广告名称 | 内部管理用名称 |
| type | INTEGER | 轮播位置 | 0->PC 首页轮播；1->app 首页轮播 |
| pic | VARCHAR | 图片地址 | 广告图 URL |
| start_time | TIMESTAMP | 开始时间 | 广告上线时间 |
| end_time | TIMESTAMP | 结束时间 | 广告下线时间 |
| status | INTEGER | 上下线状态 | 0->下线；1->上线 |
| click_count | INTEGER | 点击数 | 广告被点击次数 |
| order_count | INTEGER | 下单数 | 通过广告产生的订单数 |
| url | VARCHAR | 链接地址 | 点击广告跳转的 URL |
| note | VARCHAR | 备注 | 备注说明 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**业务逻辑：**
- 通过 `type` 区分不同终端的广告
- 通过 `start_time` 和 `end_time` 控制广告展示时间
- `click_count` 和 `order_count` 用于统计广告效果

---

#### 2.3.2 sms_home_brand - 首页品牌推荐表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| brand_id | BIGINT | 品牌 ID | 关联品牌表主键 |
| brand_name | VARCHAR | 品牌名称 | 冗余字段 |
| recommend_status | INTEGER | 推荐状态 | 0->不推荐；1->推荐 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**关联关系：**
- `brand_id` → 品牌表主键 (多对一)

**业务场景：**
- 首页品牌推荐区域展示
- 通过 `recommend_status` 控制是否显示

---

#### 2.3.3 sms_home_new_product - 首页新品推荐表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| product_id | BIGINT | 商品 ID | 关联商品表主键 |
| product_name | VARCHAR | 商品名称 | 冗余字段 |
| recommend_status | INTEGER | 推荐状态 | 0->不推荐；1->推荐 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**关联关系：**
- `product_id` → 商品表主键 (多对一)

**业务场景：**
- 首页新品推荐区域展示

---

#### 2.3.4 sms_home_recommend_product - 首页人气推荐商品表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| product_id | BIGINT | 商品 ID | 关联商品表主键 |
| product_name | VARCHAR | 商品名称 | 冗余字段 |
| recommend_status | INTEGER | 推荐状态 | 0->不推荐；1->推荐 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**关联关系：**
- `product_id` → 商品表主键 (多对一)

**业务场景：**
- 首页人气商品推荐区域展示
- 与 `sms_home_new_product` 结构相同，但业务含义不同（新品 vs 人气）

---

#### 2.3.5 sms_home_recommend_subject - 首页推荐专题表

**核心字段：**

| 字段名 | 类型 | 说明 | 枚举值/备注 |
|--------|------|------|-------------|
| id | BIGINT | 主键 ID | 自增 |
| subject_id | BIGINT | 专题 ID | 关联专题表主键 |
| subject_name | VARCHAR | 专题名称 | 冗余字段 |
| recommend_status | INTEGER | 推荐状态 | 0->不推荐；1->推荐 |
| sort | INTEGER | 排序 | 展示顺序，值越小越靠前 |

**关联关系：**
- `subject_id` → 专题表主键 (多对一)

**业务场景：**
- 首页专题推荐区域展示
- 专题通常是商品合集，如"夏季穿搭指南"、"数码好物推荐"

---

## 三、数据表关联关系图

### 3.1 优惠券板块关联关系

```
┌─────────────────────────┐
│   sms_coupon            │
│   (优惠券主表)          │
│                         │
│   - id (PK)             │
│   - type                │
│   - use_type            │
│   - publish_count       │
│   - use_count           │
│   - receive_count       │
└───────────┬─────────────┘
            │
            │ use_type=1 (指定分类)
            │ use_type=2 (指定商品)
            ├──────────────────────────┬──────────────────────────┐
            │                          │                            │
            ▼                          ▼                            ▼
┌─────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────────┐
│ sms_coupon_history      │ │sms_coupon_product_      │ │sms_coupon_product_      │
│ (领取使用记录)          │ │category_relation        │ │relation                 │
│                         │ │(优惠券 - 商品分类)       │ │(优惠券 - 商品)          │
│ - id (PK)               │ │                         │ │                         │
│ - coupon_id (FK) ───────┼─┤- coupon_id (FK)         │ │- coupon_id (FK)         │
│ - member_id             │ │- product_category_id    │ │- product_id             │
│ - use_status            │ │- product_category_name  │ │- product_name           │
│ - order_id              │ │- parent_category_name   │ │- product_sn             │
└─────────────────────────┘ └─────────────────────────┘ └─────────────────────────┘
```

### 3.2 限时购板块关联关系

```
┌─────────────────────────┐
│ sms_flash_promotion     │
│ (限时购活动)            │
│                         │
│ - id (PK)               │
│ - title                 │
│ - start_date            │
│ - end_date              │
│ - status                │
└───────────┬─────────────┘
            │
            │ 包含多个场次
            │
            ▼
┌─────────────────────────┐
│ sms_flash_promotion_    │
│ session                 │
│ (限时购场次)            │
│                         │
│ - id (PK)               │
│ - name                  │
│ - start_time            │
│ - end_time              │
│ - status                │
└───────────┬─────────────┘
            │
            │ 每个场次包含多个商品
            │
            ▼
┌─────────────────────────┐
│ sms_flash_promotion_    │
│ product_relation        │
│ (限时购 - 商品关系)      │
│                         │
│ - id (PK)               │
│ - flash_promotion_id(FK)│
│ - flash_promotion_      │
│   session_id (FK)       │
│ - product_id            │
│ - flash_promotion_price │
│ - flash_promotion_count │
│ - flash_promotion_limit │
│ - sort                  │
└─────────────────────────┘

┌─────────────────────────┐
│ sms_flash_promotion_log │
│ (会员订阅日志)          │
│                         │
│ - id (PK)               │
│ - member_id             │
│ - product_id            │
│ - member_phone          │
│ - subscribe_time        │
│ - send_time             │
└─────────────────────────┘
```

### 3.3 首页推荐板块关联关系

```
首页推荐板块各表相对独立，都通过外键关联到对应的主表：

┌─────────────────────────┐
│ sms_home_advertise      │
│ (首页广告)              │
│ - 独立表，无外键关联     │
└─────────────────────────┘

┌─────────────────────────┐
│ sms_home_brand          │
│ (首页品牌推荐)          │
│ - brand_id → 品牌表     │
└─────────────────────────┘

┌─────────────────────────┐
│ sms_home_new_product    │
│ (首页新品推荐)          │
│ - product_id → 商品表   │
└─────────────────────────┘

┌─────────────────────────┐
│ sms_home_recommend_     │
│ product                 │
│ (首页人气推荐商品)      │
│ - product_id → 商品表   │
└─────────────────────────┘

┌─────────────────────────┐
│ sms_home_recommend_     │
│ subject                 │
│ (首页推荐专题)          │
│ - subject_id → 专题表   │
└─────────────────────────┘
```

---

## 四、核心业务流程

### 4.1 优惠券发放流程

```
1. 创建优惠券 (sms_coupon)
   - 设置 publish_count（发行数量）
   - 设置 count（可用数量）= publish_count
   - 设置 use_count = 0
   - 设置 receive_count = 0

2. 根据 use_type 设置关联关系：
   - use_type=0：全场通用，无需关联
   - use_type=1：插入 sms_coupon_product_category_relation
   - use_type=2：插入 sms_coupon_product_relation

3. 用户领取优惠券
   - 插入 sms_coupon_history 记录
   - 更新 sms_coupon：
     * count = count - 1
     * receive_count = receive_count + 1

4. 用户使用优惠券
   - 更新 sms_coupon_history：
     * use_status = 1
     * use_time = 当前时间
     * order_id = 订单 ID
   - 更新 sms_coupon：
     * use_count = use_count + 1
```

### 4.2 限时购活动流程

```
1. 创建限时购活动 (sms_flash_promotion)
   - 设置活动标题、日期范围

2. 创建活动场次 (sms_flash_promotion_session)
   - 设置场次名称、时间段
   - 关联到活动

3. 添加秒杀商品 (sms_flash_promotion_product_relation)
   - 设置秒杀价格、数量、限购数量
   - 关联活动和场次

4. 用户订阅提醒 (sms_flash_promotion_log)
   - 记录用户、商品、手机号
   - 设置 subscribe_time

5. 开售时发送通知
   - 更新 sms_flash_promotion_log.send_time
```

### 4.3 首页推荐配置流程

```
1. 添加广告 (sms_home_advertise)
   - 设置广告图、链接、时间范围
   - 设置 type（PC/APP）

2. 推荐品牌/商品/专题
   - 插入对应推荐表
   - 设置 recommend_status=1
   - 设置 sort 排序

3. 前端展示
   - 根据 type、status、时间范围筛选
   - 按 sort 排序展示
```

---

## 五、关键字段枚举值总结

### 5.1 优惠券相关枚举

**sms_coupon.type（优惠券类型）**
- 0：全场赠券
- 1：会员赠券
- 2：购物赠券
- 3：注册赠券

**sms_coupon.platform（使用平台）**
- 0：全部
- 1：移动
- 2：PC

**sms_coupon.use_type（使用类型）**
- 0：全场通用
- 1：指定分类
- 2：指定商品

**sms_coupon_history.get_type（获取类型）**
- 0：后台赠送
- 1：主动获取

**sms_coupon_history.use_status（使用状态）**
- 0：未使用
- 1：已使用
- 2：已过期

**sms_coupon.member_level（会员类型）**
- 0：不限制
- 其他：具体会员等级

### 5.2 限时购相关枚举

**sms_flash_promotion.status（上下线状态）**
- 0：下线
- 1：上线

**sms_flash_promotion_session.status（启用状态）**
- 0：不启用
- 1：启用

### 5.3 首页推荐相关枚举

**sms_home_advertise.type（轮播位置）**
- 0：PC 首页轮播
- 1：app 首页轮播

**sms_home_advertise.status（上下线状态）**
- 0：下线
- 1：上线

**sms_home_brand.recommend_status（推荐状态）**
- 0：不推荐
- 1：推荐

**sms_home_new_product.recommend_status（推荐状态）**
- 0：不推荐
- 1：推荐

**sms_home_recommend_product.recommend_status（推荐状态）**
- 0：不推荐
- 1：推荐

**sms_home_recommend_subject.recommend_status（推荐状态）**
- 0：不推荐
- 1：推荐

---

## 六、数据统计字段说明

### 6.1 优惠券统计

**sms_coupon 表统计字段：**
- `publish_count`：计划发行总量（创建时设定）
- `receive_count`：已领取数量（用户领取时 +1）
- `use_count`：已使用数量（用户使用时 +1）
- `count`：剩余可用数量 = publish_count - receive_count

### 6.2 广告统计

**sms_home_advertise 表统计字段：**
- `click_count`：广告点击次数
- `order_count`：通过广告产生的订单数

---

## 七、表命名规范

所有表名遵循 `sms_` 前缀，表名结构清晰：

- `sms_coupon*`：优惠券相关表
- `sms_flash_promotion*`：限时购相关表
- `sms_home_*`：首页推荐相关表

关系表命名规范：
- `sms_coupon_product_relation`：优惠券 - 商品关系
- `sms_coupon_product_category_relation`：优惠券 - 商品分类关系
- `sms_flash_promotion_product_relation`：限时购 - 商品关系

---

## 八、总结

SMS 模块作为营销管理系统，核心功能包括：

1. **优惠券管理**：支持多种类型的优惠券，可灵活配置使用范围（全场/指定分类/指定商品），完整的领取 - 使用生命周期管理

2. **限时抢购**：支持多场次秒杀活动，可设置秒杀价格、库存、限购数量，支持会员订阅提醒

3. **首页推荐**：提供广告轮播、品牌推荐、新品推荐、人气推荐、专题推荐等多种首页展示内容

数据表设计特点：
- 主表 + 关系表的设计模式，灵活扩展
- 冗余关键字段（如商品名称、分类名称），减少关联查询
- 完善的统计字段，支持业务数据分析
- 统一的枚举值定义，便于系统维护
