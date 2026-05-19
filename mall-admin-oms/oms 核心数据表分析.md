# OMS 模块核心数据表关联关系梳理

## 一、数据表概览

OMS（Order Management System）订单管理模块共包含 **8 张核心数据表**，涵盖了订单管理、订单项、订单操作历史、订单退货、退货原因、订单设置、公司地址、购物车等业务场景。

### 数据表清单

| 序号 | 表名 | 中文名称 | 主要用途 |
|------|------|----------|----------|
| 1 | `oms_order` | 订单表 | 存储订单主表信息 |
| 2 | `oms_order_item` | 订单商品表 | 存储订单中的商品明细 |
| 3 | `oms_order_operate_history` | 订单操作历史记录表 | 记录订单状态变更历史 |
| 4 | `oms_order_return_apply` | 订单退货申请表 | 存储用户退货申请信息 |
| 5 | `oms_order_return_reason` | 退货原因表 | 配置退货原因选项 |
| 6 | `oms_order_setting` | 订单配置表 | 配置订单超时时间等参数 |
| 7 | `oms_company_address` | 公司地址表 | 配置收发货地址 |
| 8 | `oms_cart_item` | 购物车表 | 存储用户购物车数据 |

---

## 二、核心数据表详细设计

### 2.1 oms_order（订单主表）⭐

**表说明**：订单系统的核心表，存储订单的基础信息、金额信息、收货信息、状态信息等。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 订单 ID | 主键 |
| `order_sn` | VARCHAR | 订单编号 | 业务唯一标识 |
| `member_id` | BIGINT | 会员 ID | 关联用户表 |
| `member_username` | VARCHAR | 用户帐号 | 冗余字段 |
| `coupon_id` | BIGINT | 优惠券 ID | 关联优惠券表 |
| `create_time` | DATETIME | 提交时间 | 下单时间 |
| `status` | INTEGER | 订单状态 | **核心字段**：0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单 |
| `order_type` | INTEGER | 订单类型 | 0->正常订单；1->秒杀订单 |
| `source_type` | INTEGER | 订单来源 | 0->PC 订单；1->app 订单 |
| `pay_type` | INTEGER | 支付方式 | 0->未支付；1->支付宝；2->微信 |
| `total_amount` | DECIMAL | 订单总金额 | 商品原价总和 |
| `pay_amount` | DECIMAL | 应付金额（实际支付金额） | 优惠后实际支付 |
| `freight_amount` | DECIMAL | 运费金额 | |
| `promotion_amount` | DECIMAL | 促销优化金额 | 促销价、满减、阶梯价优惠 |
| `integration_amount` | DECIMAL | 积分抵扣金额 | |
| `coupon_amount` | DECIMAL | 优惠券抵扣金额 | |
| `discount_amount` | DECIMAL | 管理员后台调整订单使用的折扣金额 | |
| `delivery_company` | VARCHAR | 物流公司 (配送方式) | |
| `delivery_sn` | VARCHAR | 物流单号 | |
| `auto_confirm_day` | INTEGER | 自动确认时间（天） | |
| `integration` | INTEGER | 可以获得的积分 | |
| `growth` | INTEGER | 可以获得的成长值 | |
| `promotion_info` | VARCHAR | 活动信息 | 记录参与的促销活动 |
| `bill_type` | INTEGER | 发票类型 | 0->不开发票；1->电子发票；2->纸质发票 |
| `bill_header` | VARCHAR | 发票抬头 | |
| `bill_content` | VARCHAR | 发票内容 | |
| `bill_receiver_phone` | VARCHAR | 收票人电话 | |
| `bill_receiver_email` | VARCHAR | 收票人邮箱 | |
| `receiver_name` | VARCHAR | 收货人姓名 | |
| `receiver_phone` | VARCHAR | 收货人电话 | |
| `receiver_post_code` | VARCHAR | 收货人邮编 | |
| `receiver_province` | VARCHAR | 省份/直辖市 | |
| `receiver_city` | VARCHAR | 城市 | |
| `receiver_region` | VARCHAR | 区 | |
| `receiver_detail_address` | VARCHAR | 详细地址 | |
| `note` | VARCHAR | 订单备注 | 用户留言 |
| `confirm_status` | INTEGER | 确认收货状态 | 0->未确认；1->已确认 |
| `delete_status` | INTEGER | 删除状态 | 0->未删除；1->已删除（逻辑删除） |
| `use_integration` | INTEGER | 下单时使用的积分 | |
| `payment_time` | DATETIME | 支付时间 | |
| `delivery_time` | DATETIME | 发货时间 | |
| `receive_time` | DATETIME | 确认收货时间 | |
| `comment_time` | DATETIME | 评价时间 | |
| `modify_time` | DATETIME | 修改时间 | |

#### 核心状态流转

```
待付款 (0) → 待发货 (1) → 已发货 (2) → 已完成 (3)
    ↓           ↓
  已关闭 (4)  已关闭 (4)
    ↓
  无效订单 (5)
```

---

### 2.2 oms_order_item（订单商品表）⭐

**表说明**：存储订单中每个商品的详细信息，与订单主表是 **多对一** 关系。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 订单项 ID | 主键 |
| `order_id` | BIGINT | 订单 ID | **外键**，关联 oms_order.id |
| `order_sn` | VARCHAR | 订单编号 | 冗余字段 |
| `product_id` | BIGINT | 商品 ID | 关联商品表 |
| `product_name` | VARCHAR | 商品名称 | 快照信息 |
| `product_pic` | VARCHAR | 商品图片 | 快照信息 |
| `product_brand` | VARCHAR | 商品品牌 | |
| `product_sn` | VARCHAR | 商品编码 | |
| `product_price` | DECIMAL | 销售价格 | 下单时的单价 |
| `product_quantity` | INTEGER | 购买数量 | |
| `product_sku_id` | BIGINT | 商品 SKU 编号 | |
| `product_sku_code` | VARCHAR | 商品 SKU 条码 | |
| `product_category_id` | BIGINT | 商品分类 ID | |
| `product_attr` | VARCHAR | 商品销售属性 | JSON 格式：[{'key':'颜色','value':'红色'},{'key':'容量','value':'4G'}] |
| `promotion_name` | VARCHAR | 商品促销名称 | |
| `promotion_amount` | DECIMAL | 商品促销分解金额 | |
| `coupon_amount` | DECIMAL | 优惠券优惠分解金额 | 分摊到该商品的优惠 |
| `integration_amount` | DECIMAL | 积分优惠分解金额 | |
| `real_amount` | DECIMAL | 该商品经过优惠后的分解金额 | 实际支付金额（单品） |
| `gift_integration` | INTEGER | 赠送积分 | |
| `gift_growth` | INTEGER | 赠送成长值 | |

#### 与订单主表的关系

- **关联关系**：`oms_order_item.order_id` → `oms_order.id`
- **关系类型**：多对一（一个订单包含多个订单项）
- **业务说明**：订单金额 = Σ订单项 real_amount + 运费

---

### 2.3 oms_order_operate_history（订单操作历史记录表）⭐

**表说明**：记录订单从创建到完成/关闭的完整生命周期，每次状态变更都会在此表留下记录。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 记录 ID | 主键 |
| `order_id` | BIGINT | 订单 ID | **外键**，关联 oms_order.id |
| `operate_man` | VARCHAR | 操作人 | 用户；系统；后台管理员 |
| `create_time` | DATETIME | 操作时间 | |
| `order_status` | INTEGER | 订单状态 | 同 oms_order.status |
| `note` | VARCHAR | 备注 | 操作说明 |

#### 与订单主表的关系

- **关联关系**：`oms_order_operate_history.order_id` → `oms_order.id`
- **关系类型**：多对一（一个订单有多条操作记录）
- **业务场景**：
  - 用户下单
  - 用户支付
  - 后台发货
  - 用户确认收货
  - 修改收货人信息
  - 修改费用信息
  - 订单关闭等

---

### 2.4 oms_order_return_apply（订单退货申请表）⭐

**表说明**：存储用户的退货/退款申请信息，记录退货流程的各个状态。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 退货申请 ID | 主键 |
| `order_id` | BIGINT | 订单 ID | **外键**，关联 oms_order.id |
| `order_sn` | VARCHAR | 订单编号 | 冗余字段 |
| `product_id` | BIGINT | 退货商品 ID | 关联商品表 |
| `company_address_id` | BIGINT | 收货地址表 ID | **外键**，关联 oms_company_address.id |
| `create_time` | DATETIME | 申请时间 | |
| `member_username` | VARCHAR | 会员用户名 | |
| `return_amount` | DECIMAL | 退款金额 | |
| `return_name` | VARCHAR | 退货人姓名 | |
| `return_phone` | VARCHAR | 退货人电话 | |
| `status` | INTEGER | 申请状态 | **核心字段**：0->待处理；1->退货中；2->已完成；3->已拒绝 |
| `handle_time` | DATETIME | 处理时间 | 商家审核时间 |
| `product_pic` | VARCHAR | 商品图片 | |
| `product_name` | VARCHAR | 商品名称 | |
| `product_brand` | VARCHAR | 商品品牌 | |
| `product_attr` | VARCHAR | 商品销售属性 | 颜色：红色；尺码：xl; |
| `product_count` | INTEGER | 退货数量 | |
| `product_price` | DECIMAL | 商品单价 | |
| `product_real_price` | DECIMAL | 商品实际支付单价 | |
| `reason` | VARCHAR | 原因 | 退货原因 |
| `description` | VARCHAR | 描述 | 详细说明 |
| `proof_pics` | VARCHAR | 凭证图片 | 以逗号隔开的图片 URL |
| `handle_note` | VARCHAR | 处理备注 | 商家审核意见 |
| `handle_man` | VARCHAR | 处理人员 | 商家处理人 |
| `receive_man` | VARCHAR | 收货人 | 仓库收货人 |
| `receive_time` | DATETIME | 收货时间 | 仓库收到退货时间 |
| `receive_note` | VARCHAR | 收货备注 | 仓库收货备注 |

#### 状态流转

```
待处理 (0) → 退货中 (1) → 已完成 (2)
    ↓
  已拒绝 (3)
```

#### 与其他表的关系

- `order_id` → `oms_order.id`：关联订单主表
- `company_address_id` → `oms_company_address.id`：关联退货收货地址
- `product_id` → 商品表：关联商品信息

---

### 2.5 oms_order_return_reason（退货原因表）

**表说明**：配置用户可申请退货的原因选项，供前端下拉选择。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 原因 ID | 主键 |
| `name` | VARCHAR | 退货类型 | 如：质量问题、七天无理由、拍错等 |
| `sort` | INTEGER | 排序 | 数值越小越靠前 |
| `status` | INTEGER | 状态 | 0->不启用；1->启用 |
| `create_time` | DATETIME | 添加时间 | |

---

### 2.6 oms_order_setting（订单配置表）

**表说明**：存储订单系统的超时时间配置，用于订单自动状态流转。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 配置 ID | 主键 |
| `flash_order_overtime` | INTEGER | 秒杀订单超时关闭时间 (分) | |
| `normal_order_overtime` | INTEGER | 正常订单超时时间 (分) | 未支付自动关闭 |
| `confirm_overtime` | INTEGER | 发货后自动确认收货时间（天） | |
| `finish_overtime` | INTEGER | 自动完成交易时间，不能申请售后（天） | |
| `comment_overtime` | INTEGER | 订单完成后自动好评时间（天） | |

---

### 2.7 oms_company_address（公司地址表）

**表说明**：配置商家的收发货地址，用于订单发货和退货收货。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 地址 ID | 主键 |
| `address_name` | VARCHAR | 地址名称 | 如：北京仓、上海仓 |
| `send_status` | INTEGER | 默认发货地址 | 0->否；1->是 |
| `receive_status` | INTEGER | 是否默认收货地址 | 0->否；1->是 |
| `name` | VARCHAR | 收发货人姓名 | |
| `phone` | VARCHAR | 收货人电话 | |
| `province` | VARCHAR | 省/直辖市 | |
| `city` | VARCHAR | 市 | |
| `region` | VARCHAR | 区 | |
| `detail_address` | VARCHAR | 详细地址 | |

#### 业务关联

- 订单发货时使用默认发货地址
- 退货申请时选择退货收货地址（`oms_order_return_apply.company_address_id`）

---

### 2.8 oms_cart_item（购物车表）

**表说明**：存储用户购物车中的商品信息，下单后通常会清空对应商品。

#### 主要字段说明

| 字段名 | 类型 | 说明 | 备注 |
|--------|------|------|------|
| `id` | BIGINT | 购物车项 ID | 主键 |
| `product_id` | BIGINT | 商品 ID | |
| `product_sku_id` | BIGINT | 商品 SKU 编号 | |
| `member_id` | BIGINT | 会员 ID | |
| `quantity` | INTEGER | 购买数量 | |
| `price` | DECIMAL | 添加到购物车的价格 | |
| `product_pic` | VARCHAR | 商品主图 | |
| `product_name` | VARCHAR | 商品名称 | |
| `product_sub_title` | VARCHAR | 商品副标题（卖点） | |
| `product_sku_code` | VARCHAR | 商品 sku 条码 | |
| `member_nickname` | VARCHAR | 会员昵称 | |
| `create_date` | DATETIME | 创建时间 | |
| `modify_date` | DATETIME | 修改时间 | |
| `delete_status` | INTEGER | 是否删除 | 0->未删除；1->已删除 |
| `product_category_id` | BIGINT | 商品分类 | |
| `product_brand` | VARCHAR | 商品品牌 | |
| `product_sn` | VARCHAR | 商品编码 | |
| `product_attr` | VARCHAR | 商品销售属性 | JSON 格式 |

---

## 三、数据表关联关系图

### 3.1 核心关联关系

```
┌─────────────────────────────────────────────────────────────────┐
│                        OMS 核心数据表关联关系                      │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│   oms_order      │  订单主表
│  (订单核心信息)   │
└────────┬─────────┘
         │
         │ 1 对 多
         │ (order_id)
         ▼
┌──────────────────┐
│ oms_order_item   │  订单商品明细
│  (订单商品快照)   │
└──────────────────┘

┌──────────────────┐
│   oms_order      │
└────────┬─────────┘
         │ 1 对 多
         │ (order_id)
         ▼
┌──────────────────────────┐
│ oms_order_operate_history│  订单操作历史
│   (订单状态变更记录)      │
└──────────────────────────┘

┌──────────────────┐
│   oms_order      │
└────────┬─────────┘
         │ 1 对 多
         │ (order_id)
         ▼
┌──────────────────────────┐
│ oms_order_return_apply   │  退货申请
│   (退货/退款流程)         │
└───────────┬──────────────┘
            │
            │ 多对 1
            │ (company_address_id)
            ▼
┌──────────────────────────┐
│ oms_company_address      │  公司地址
│   (收发货地址配置)        │
└──────────────────────────┘

┌──────────────────────────┐
│ oms_order_return_apply   │
└───────────┬──────────────┘
            │ 关联退货原因（文本）
            ▼
┌──────────────────────────┐
│ oms_order_return_reason  │  退货原因
│   (退货原因选项配置)      │
└──────────────────────────┘

┌──────────────────────────┐
│    oms_order_setting     │  订单配置
│   (超时时间等系统参数)    │
└──────────────────────────┘

┌──────────────────┐
│  oms_cart_item   │  购物车
│ (下单前临时存储)  │
└──────────────────┘
```

### 3.2 订单查询关联关系

查询订单详情时的典型关联查询：

```sql
SELECT 
    o.*,                                    -- 订单主表信息
    oi.id AS item_id,
    oi.product_id AS item_product_id,       -- 订单商品明细
    oi.product_name AS item_product_name,
    oi.product_quantity AS item_product_quantity,
    oh.id AS history_id,
    oh.operate_man AS history_operate_man,  -- 订单操作历史
    oh.order_status AS history_order_status,
    oh.create_time AS history_create_time
FROM oms_order o
LEFT JOIN oms_order_item oi ON o.id = oi.order_id
LEFT JOIN oms_order_operate_history oh ON o.id = oh.order_id
WHERE o.id = #{orderId}
ORDER BY oi.id ASC, oh.create_time DESC
```

---

## 四、核心业务流程的数据表操作

### 4.1 订单创建流程

1. **购物车 → 订单**
   - 从 `oms_cart_item` 读取购物车数据
   - 创建 `oms_order` 主记录（status=0 待付款）
   - 创建多条 `oms_order_item` 记录
   - 创建 `oms_order_operate_history` 记录（操作：用户下单）
   - 清空或标记删除 `oms_cart_item` 对应记录

### 4.2 订单支付流程

1. **更新订单状态**
   - 更新 `oms_order.status` = 1（待发货）
   - 更新 `oms_order.payment_time`
   - 插入 `oms_order_operate_history`（操作：用户支付）

### 4.3 订单发货流程

1. **后台发货**
   - 更新 `oms_order.status` = 2（已发货）
   - 更新 `oms_order.delivery_company`、`oms_order.delivery_sn`
   - 更新 `oms_order.delivery_time`
   - 插入 `oms_order_operate_history`（操作：后台发货）

### 4.4 订单确认收货流程

1. **用户确认收货**
   - 更新 `oms_order.status` = 3（已完成）
   - 更新 `oms_order.receive_time`
   - 更新 `oms_order.confirm_status` = 1
   - 插入 `oms_order_operate_history`（操作：用户确认收货）

### 4.5 订单关闭流程

1. **关闭订单**
   - 更新 `oms_order.status` = 4（已关闭）
   - 更新 `oms_order.modify_time`
   - 插入 `oms_order_operate_history`（操作：订单关闭）

### 4.6 退货申请流程

1. **用户申请退货**
   - 创建 `oms_order_return_apply` 记录（status=0 待处理）
   - 关联 `oms_order.id`、`oms_company_address.id`

2. **商家审核退货**
   - 更新 `oms_order_return_apply.status` = 1（退货中）
   - 更新 `oms_order_return_apply.company_address_id`（指定退货地址）
   - 更新 `oms_order_return_apply.handle_time`、`handle_man`、`handle_note`

3. **商家收到退货**
   - 更新 `oms_order_return_apply.status` = 2（已完成）
   - 更新 `oms_order_return_apply.receive_time`、`receive_man`、`receive_note`

4. **拒绝退货**
   - 更新 `oms_order_return_apply.status` = 3（已拒绝）
   - 更新 `oms_order_return_apply.handle_time`、`handle_man`、`handle_note`

---

## 五、重要字段枚举值说明

### 5.1 订单状态 (status)

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 待付款 | 用户下单后未支付 |
| 1 | 待发货 | 用户已支付，等待商家发货 |
| 2 | 已发货 | 商家已发货，等待用户确认 |
| 3 | 已完成 | 用户已确认收货 |
| 4 | 已关闭 | 订单关闭（未支付超时/用户取消/拒绝退货等） |
| 5 | 无效订单 | 无效订单（如恶意订单等） |

### 5.2 订单类型 (order_type)

| 值 | 类型 | 说明 |
|----|------|------|
| 0 | 正常订单 | 普通购买的订单 |
| 1 | 秒杀订单 | 参与秒杀活动的订单 |

### 5.3 订单来源 (source_type)

| 值 | 来源 | 说明 |
|----|------|------|
| 0 | PC 订单 | 来自 PC 端 |
| 1 | APP 订单 | 来自移动端 APP |

### 5.4 支付方式 (pay_type)

| 值 | 方式 | 说明 |
|----|------|------|
| 0 | 未支付 | 尚未支付 |
| 1 | 支付宝 | 支付宝支付 |
| 2 | 微信 | 微信支付 |

### 5.5 发票类型 (bill_type)

| 值 | 类型 | 说明 |
|----|------|------|
| 0 | 不开发票 | 不需要发票 |
| 1 | 电子发票 | 电子发票 |
| 2 | 纸质发票 | 纸质发票 |

### 5.6 退货申请状态 (status)

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 待处理 | 用户提交申请，等待商家审核 |
| 1 | 退货中 | 商家同意退货，用户寄回商品 |
| 2 | 已完成 | 商家收到退货，完成退款 |
| 3 | 已拒绝 | 商家拒绝退货申请 |

### 5.7 删除状态 (delete_status)

| 值 | 状态 | 说明 |
|----|------|------|
| 0 | 未删除 | 正常数据 |
| 1 | 已删除 | 逻辑删除（数据保留但不可见） |

---

## 六、数据表设计特点

### 6.1 快照设计

- **订单商品快照**：`oms_order_item` 中的商品信息（名称、图片、价格等）都是下单时的快照，即使后续商品信息变更，订单中的信息也不会改变
- **收货信息快照**：`oms_order` 中的收货人信息是下单时的快照

### 6.2 金额分摊设计

- 订单级的优惠金额（促销、优惠券、积分）会分摊到每个订单项
- `oms_order_item` 中的 `promotion_amount`、`coupon_amount`、`integration_amount`、`real_amount` 体现了分摊逻辑

### 6.3 逻辑删除

- 核心表都采用逻辑删除（`delete_status` 字段），而非物理删除
- 保证数据可追溯性，便于数据恢复和历史查询

### 6.4 冗余设计

- `oms_order_item` 中冗余了 `order_sn`，便于独立查询
- `oms_order_return_apply` 中冗余了商品信息和订单编号，便于独立展示

### 6.5 操作可追溯

- 所有订单状态变更都会记录到 `oms_order_operate_history`
- 退货申请的每个处理环节都有详细记录（处理人、处理时间、处理备注）

---

## 七、典型 SQL 查询示例

### 7.1 查询订单列表（带条件）

```sql
SELECT * FROM oms_order
WHERE delete_status = 0
  AND order_sn = '202401010001'        -- 订单编号
  AND status = 1                        -- 订单状态
  AND source_type = 1                   -- 订单来源
  AND order_type = 0                    -- 订单类型
ORDER BY create_time DESC
```

### 7.2 查询订单详情（包含商品和操作历史）

```sql
SELECT 
    o.*,
    oi.id AS item_id,
    oi.product_id AS item_product_id,
    oi.product_sn AS item_product_sn,
    oi.product_pic AS item_product_pic,
    oi.product_name AS item_product_name,
    oi.product_brand AS item_product_brand,
    oi.product_price AS item_product_price,
    oi.product_quantity AS item_product_quantity,
    oi.product_attr AS item_product_attr,
    oh.id AS history_id,
    oh.operate_man AS history_operate_man,
    oh.create_time AS history_create_time,
    oh.order_status AS history_order_status,
    oh.note AS history_note
FROM oms_order o
LEFT JOIN oms_order_item oi ON o.id = oi.order_id
LEFT JOIN oms_order_operate_history oh ON o.id = oh.order_id
WHERE o.id = #{id}
  AND o.delete_status = 0
ORDER BY oi.id ASC, oh.create_time DESC
```

### 7.3 查询退货申请列表

```sql
SELECT 
    ra.*,
    ca.address_name AS company_address_name,
    ca.name AS receive_name,
    ca.phone AS receive_phone,
    ca.detail_address AS receive_address
FROM oms_order_return_apply ra
LEFT JOIN oms_company_address ca ON ra.company_address_id = ca.id
WHERE ra.status IN (0, 1)
ORDER BY ra.create_time DESC
```

---

## 八、总结

OMS 模块的 8 张核心数据表构成了完整的订单管理系统：

1. **核心表**：`oms_order` + `oms_order_item` + `oms_order_operate_history`
2. **售后表**：`oms_order_return_apply` + `oms_order_return_reason`
3. **配置表**：`oms_order_setting` + `oms_company_address`
4. **前置表**：`oms_cart_item`

表与表之间通过外键关联（主要是 order_id），形成了清晰的数据关系网络，支持订单的完整生命周期管理和售后流程。
