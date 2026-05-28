# Redis 库存预占与下单锁升级说明

本文档说明本次新增的优化版下单链路。原来的 `/order/generateOrder`、`OmsPortalOrderServiceImpl.generateOrder()`、`PmsPortalProductServiceImpl.lockStock()` 没有改动；新增了一套 `/order/optimized/*` 入口，用于演示 `mall:pms:sku:stock:{skuId}` 和 `mall:lock:order:create:{memberId}` 的升级实现。

## 一、改造目标

原链路的问题是：

- 库存检查和锁库存分离：`hasStock()` 先判断，`lockStock()` 后远程调用，两个动作不是原子操作。
- 同一会员重复点击提交订单时，可能并发创建多张订单。
- 下单线程同步调用商品服务锁库存，订单服务和商品库存写入耦合较紧。

升级后的目标：

- 用 Redis Lua 对 SKU 库存做原子预占。
- 用 Redis `SET NX EX` 对同一会员下单做短时互斥。
- 订单服务只负责本地订单事务，事务提交后通过 MQ 通知商品服务异步落库 `lock_stock`。

## 二、新增接口

新增控制器：

```text
mall-portal-order/order-server/src/main/java/com/mtcarpenter/mall/portal/controller/OmsPortalOrderOptimizedController.java
```

接口：

```text
POST /order/optimized/generateOrder
POST /order/optimized/paySuccess
POST /order/optimized/cancelOrder
```

旧接口仍然保留：

```text
POST /order/generateOrder
```

## 三、Redis Key 设计

### 1. SKU 可售库存

```text
mall:pms:sku:stock:{skuId}
```

类型：String  
值：当前 Redis 侧可售库存。

优化版下单第一次遇到某个 SKU 时，会根据购物车促销计算出来的 `realStock` 做 `setIfAbsent` 初始化。后续所有并发请求都通过 Lua 从这个 key 原子扣减。

实现位置：

```text
RedisSkuStockReserveServiceImpl.initStockIfAbsent()
RedisSkuStockReserveServiceImpl.reserve()
```

### 2. 订单预占明细

```text
mall:pms:sku:reserved:{orderSn}
```

类型：Hash  
结构：

```text
skuId -> quantity
```

用途：订单创建失败或超时取消时，根据这个 hash 把 Redis 库存补回去。TTL 默认为 30 分钟。

### 3. 会员下单锁

```text
mall:lock:order:create:{memberId}
```

类型：String  
值：本次请求的 `requestId`。  
TTL：10 秒。

实现位置：

```text
RedisOrderCreateLockServiceImpl.tryLock()
RedisOrderCreateLockServiceImpl.release()
```

加锁使用：

```text
SET key requestId NX EX 10
```

释放锁使用 Lua 校验 value，避免误删其他请求的锁：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
  return redis.call('del', KEYS[1])
else
  return 0
end
```

### 4. 异步落库保护标记

为了解决“支付/取消可能早于 MQ 消费”的时序问题，新增两个内部标记：

```text
mall:pms:sku:locked:{orderSn}
mall:pms:sku:canceled:{orderSn}
```

商品服务成功消费锁库存消息后写入 `locked` 标记；优化版支付接口会等待该标记出现后再扣真实库存。取消订单时写入 `canceled` 标记，商品消费者消费前后都会检查，避免取消后又异步加锁。

常量位置：

```text
mall-common/src/main/java/com/mtcarpenter/mall/common/constant/ProductStockQueueConstants.java
```

## 四、优化版下单流程

核心实现：

```text
mall-portal-order/order-server/src/main/java/com/mtcarpenter/mall/portal/service/impl/OmsPortalOrderOptimizedServiceImpl.java
```

流程：

1. 从 Redis 获取当前会员。
2. 使用 `mall:lock:order:create:{memberId}` 获取会员级下单锁。
3. 查询购物车促销信息、优惠券、积分、收货地址，组装订单与订单项。
4. 生成订单号。
5. 初始化并预占 Redis SKU 库存。
6. 本地事务写入 `oms_order`、`oms_order_item`，更新优惠券、积分、购物车。
7. 事务提交后，通过 MQ 发送 `SkuStockLockMessage`。
8. 事务提交后，发送优化版延迟取消订单消息。
9. 如果事务内抛异常，回滚 Redis 预占库存。
10. 事务完成后释放会员下单锁。

关键代码点：

```java
skuStockReserveService.initStockIfAbsent(cartPromotionItemList);
skuStockReserveService.reserve(orderSn, cartPromotionItemList);
```

这里的 `reserve()` 使用 Lua，一次性完成“检查所有 SKU 库存是否足够”和“扣减所有 SKU 库存”。同一个订单里相同 `skuId` 会先聚合数量，再进入 Lua，避免同一 SKU 多行时扣成负数。

事务提交后发送 MQ：

```java
runAfterCommit(new Runnable() {
    @Override
    public void run() {
        skuStockLockSender.sendMessage(stockLockMessage);
        sendDelayMessageCancelOrder(order.getId());
    }
});
```

这样订单事务回滚时不会发送库存落库消息。

## 五、Lua 原子预占逻辑

实现位置：

```text
RedisSkuStockReserveServiceImpl.reserveScript()
```

核心逻辑：

```lua
for i = 1, n do
  local stock = redis.call('get', KEYS[i])
  if stock == false then return -1 end
  if tonumber(stock) < tonumber(ARGV[i]) then return 0 end
end

for i = 1, n do
  redis.call('decrby', KEYS[i], tonumber(ARGV[i]))
  redis.call('hincrby', reserveKey, ARGV[n + i], tonumber(ARGV[i]))
end
```

返回含义：

```text
1  -> 预占成功
0  -> 库存不足
-1 -> Redis 库存 key 未初始化
```

## 六、MQ 异步锁库存落库

消息类：

```text
mall-common/src/main/java/com/mtcarpenter/mall/common/domain/SkuStockLockMessage.java
mall-common/src/main/java/com/mtcarpenter/mall/common/domain/SkuStockLockItem.java
```

队列配置：

```text
mall.product.stock.direct
mall.product.stock.lock
mall.product.stock.lock
```

订单侧发送：

```text
mall-portal-order/order-server/src/main/java/com/mtcarpenter/mall/portal/component/SkuStockLockSender.java
```

商品侧消费：

```text
mall-portal-product/product-server/src/main/java/com/mtcarpenter/mall/portal/component/SkuStockLockReceiver.java
```

商品服务本地事务：

```text
mall-portal-product/product-server/src/main/java/com/mtcarpenter/mall/portal/service/impl/OptimizedSkuStockServiceImpl.java
```

MySQL 原子更新：

```xml
UPDATE pms_sku_stock
SET lock_stock = lock_stock + #{quantity}
WHERE id = #{skuId}
  AND (stock - lock_stock) >= #{quantity}
```

这个 SQL 仍然保留数据库侧保护：即使 Redis/MQ 出现异常重放，也不会在 MySQL 层锁定超过可售库存的数量。

## 七、取消与支付处理

### 支付成功

优化版支付入口：

```text
POST /order/optimized/paySuccess
```

处理逻辑：

1. 根据订单号等待 `mall:pms:sku:locked:{orderSn}`。
2. 标记订单支付成功。
3. 调用原有 `PortalOrderDao.updateSkuStock()` 扣真实库存并释放锁定库存。
4. 删除 `mall:pms:sku:reserved:{orderSn}`。

如果 MQ 锁库存还没落库，接口会返回：

```text
库存锁定落库处理中，请稍后重试
```

### 取消订单

优化版取消入口：

```text
POST /order/optimized/cancelOrder
```

优化版延迟取消队列：

```text
mall.order.optimized.cancel.ttl
mall.order.optimized.cancel
```

处理逻辑：

1. 写入 `mall:pms:sku:canceled:{orderSn}`。
2. 如果商品侧已经锁库存落库，则释放 MySQL `lock_stock`。
3. 根据 `mall:pms:sku:reserved:{orderSn}` 回滚 Redis 可售库存。
4. 恢复优惠券和积分。

商品侧消费者在消费锁库存消息前后都会检查 canceled 标记：如果订单已经取消，则跳过锁库；如果刚锁完才发现取消，则立刻释放本次锁库。

## 八、已验证

执行过相关模块编译：

```bash
mvn -pl mall-portal-order/order-server,mall-portal-product/product-server -am -DskipTests compile
```

结果：`BUILD SUCCESS`。

本次编译还顺手修复了当前 JDK 下老 Lombok 版本不兼容的问题：在根 `pom.xml` 中把 Lombok 固定到 `1.18.30`。这是构建层面的兼容性调整，不涉及业务逻辑。

## 九、后续可继续加强

当前实现是“本地事务 + afterCommit 发 MQ”的轻量版。生产级可以继续加：

- 本地消息表 outbox：订单事务内写消息表，后台任务投递 MQ，解决 afterCommit 发送 MQ 失败的问题。
- 消费幂等表：按 `orderSn + skuId + eventType` 防止重复消费。
- Redis 库存定期校准：以 MySQL `stock - lock_stock` 为基准，低峰校正 Redis 库存。
- 支付与取消也完全改为商品侧 MQ 事件，进一步移除订单服务直接写 `pms_sku_stock` 的逻辑。
