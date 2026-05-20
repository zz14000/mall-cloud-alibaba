# Redis 使用情况分析

## 📍 Redis 使用位置

### 1. **RedisService 接口和实现类**

这是基础工具类，在mall-security/service里面，相当于mybatis

- `RedisService.java` - Redis 操作接口定义
- `RedisServiceImpl.java` - Redis 操作实现类

### 2. **业务服务类中使用 Redis**

#### (1) **UmsAdminCacheServiceImpl** (后台管理员缓存)
路径：`mall-admin-ums\ums-server\src\main\java\com\mtcarpenter\mall\service\impl\UmsAdminCacheServiceImpl.java`
- 缓存管理员信息
- 缓存管理员资源列表
- 缓存 Token

#### (2) **UmsMemberCacheServiceImpl** (前台会员缓存)
路径：`mall-portal-member\member-server\src\main\java\com\mtcarpenter\mall\portal\service\impl\UmsMemberCacheServiceImpl.java`
- 缓存会员信息
- 缓存验证码（authCode）

#### (3) **OmsPortalOrderServiceImpl** (前台订单服务)
路径：`mall-portal-order\order-server\src\main\java\com\mtcarpenter\mall\portal\service\impl\OmsPortalOrderServiceImpl.java`
- 生成订单号时使用 Redis 递增

---

## 📦 Redis 数据存储格式

### **Key 的命名格式：**
```
其中{key_prefix}里面包含了{业务模块}和{数据类型}，{标识}里面存放的是id类字段，可以唯一定位一个资源
{database}:{key_prefix}:{identifier}
{database}:{业务模块}:{数据类型}:{标识}
```

### **具体 Key 结构：**

#### 1. **后台管理员相关**
```yaml
redis:
  database: mall
  key:
    admin: 'ums:admin'
    resourceList: 'ums:resourceList'
    token: 未配置（代码中直接使用 REDIS_KEY_TOKEN）
```

**实际 Key 示例：**
- 管理员信息：`mall:ums:admin:username`
- 资源列表：`mall:ums:resourceList:adminId`
- Token：`mall:ums:token:username`

#### 2. **前台会员相关**
```yaml
redis:
  database: mall
  key:
    member: 'ums:member'
    authCode: 'ums:authCode'
    orderId: 'oms:orderId'
```

**实际 Key 示例：**
- 会员信息：`mall:ums:member:username`
- 验证码：`mall:ums:authCode:telephone`
- 订单号生成：`mall:oms:orderId:yyyyMMdd`

#### 3. **订单号生成**
- Key：`mall:oms:orderId:20260520`
- 使用 `incr` 操作实现递增

---

## 💾 Value 的数据类型

1. **对象类型**：
   - `UmsAdmin` - 管理员对象
   - `UmsMember` - 会员对象
   - `List<UmsResource>` - 资源列表

2. **字符串类型**：
   - `String` - Token、验证码

3. **数值类型**：
   - `Long` - 订单号递增计数器

---

## ⚙️ Redis 配置

### 过期时间配置：
```yaml
redis:
  expire:
    common: 86400        # 24 小时（通用缓存）
    authCode: 90         # 验证码 90 秒
  expiration: 604800     # JWT Token 7 天
```

### 序列化方式：
使用 `Jackson2JsonRedisSerializer` 进行 JSON 序列化存储。

---

## 📝 总结

本项目 Redis 主要用途：
1. ✅ **缓存用户信息**（管理员、会员）
2. ✅ **缓存权限资源列表**
3. ✅ **存储 Token**
4. ✅ **存储验证码**
5. ✅ **生成唯一订单号**

所有 Redis Key 都遵循统一的命名规范：`{database}:{业务模块}:{数据类型}:{标识}`
