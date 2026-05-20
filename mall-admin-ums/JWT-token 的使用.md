# JWT Token 的使用详解

## 一、JWT Token 的生成

### 生成时机：登录时

**源码位置**：[`UmsAdminServiceImpl.login()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/service/impl/UmsAdminServiceImpl.java#L118-L138)

```java
public String login(String username, String password) {
    // 1. 加载用户信息（包含权限）
    UserDetails userDetails = loadUserByUsername(username);
    
    // 2. 验证密码
    if (!passwordEncoder.matches(password, userDetails.getPassword())) {
        throw new BadCredentialsException("密码不正确");
    }
    
    // 3. 构建认证令牌
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    // 4. ★ 生成 JWT Token
    token = jwtTokenUtil.generateToken(userDetails);
    //    ↑ 包含用户名、创建时间、过期时间
    
    // 5. 将 token 存入 Redis（用于服务端主动失效）
    adminCacheService.setToken(username, tokenHead + token);
    
    // 6. 记录登录日志
    insertLoginLog(username);
    
    return token;
}
```

### Token 结构

**生成代码**：[`JwtTokenUtil.generateToken()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-security/src/main/java/com/mtcarpenter/mall/security/util/JwtTokenUtil.java#L44-L50)

```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());  // "sub": "admin"
    claims.put(CLAIM_KEY_CREATED, new Date());                  // "created": 时间戳
    return generateToken(claims);
}

private String generateToken(Map<String, Object> claims) {
    return Jwts.builder()
        .setClaims(claims)
        .setExpiration(generateExpirationDate())  // 过期时间
        .signWith(SignatureAlgorithm.HS512, secret)  // 签名
        .compact();
}
```

**Token 格式**：`header.payload.signature`

**Payload 示例**：
```json
{
  "sub": "admin",                    // 用户名
  "created": 1489079981393,          // 创建时间
  "exp": 1489684781                  // 过期时间
}
```

---

## 二、JWT Token 的使用场景

### 使用场景 1：前端每次请求携带 Token

**源码位置**：[`request.js`](file:///d:/mall-cloud-alibaba/mall-admin-web/src/utils/request.js#L12-L16)

```javascript
// request 拦截器
service.interceptors.request.use(config => {
  if (store.getters.token) {
    // 每个请求都携带 Token
    config.headers['Authorization'] = getToken()  // 如 "Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6..."
  }
  return config
})
```

**使用频率**：**每个后端请求**都会携带

---

### 使用场景 2：后端验证 Token 并加载权限

**源码位置**：[`JwtAuthenticationTokenFilter.doFilterInternal()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-security/src/main/java/com/mtcarpenter/mall/security/component/JwtAuthenticationTokenFilter.java#L48-L67)

```java
protected void doFilterInternal(HttpServletRequest request, ...) {
    // 1. 从请求头提取 Token
    String authHeader = request.getHeader(this.tokenHeader);  // "Authorization"
    
    if (authHeader != null && authHeader.startsWith(this.tokenHead)) {
        String authToken = authHeader.substring(this.tokenHead.length());
        // 如 "Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6..." → "eyJhbGciOiJIUzUxMiIsInR5cCI6..."
        
        // 2. 从 Token 解析用户名
        String username = jwtTokenUtil.getUserNameFromToken(authToken);
        // 如 "admin"
        
        // 3. 加载用户信息和权限
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
        // 返回 AdminUserDetails {
        //   umsAdmin: {id:1, username:"admin", ...},
        //   resourceList: [{id:1, name:"商品添加", url:"/product/create"}, ...]
        // }
        
        // 4. 验证 Token 是否有效
        if (jwtTokenUtil.validateToken(authToken, userDetails)) {
            // 5. ★ 构建认证对象并放入 SecurityContextHolder
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
    chain.doFilter(request, response);
}
```

**使用频率**：**每次请求**都会经过此过滤器

---

### 使用场景 3：动态权限决策

**源码位置**：[`DynamicAccessDecisionManager.decide()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-admin-ums/动态权限实现.md#L33-L48)

```java
public void decide(Authentication authentication, Object object,
                   Collection<ConfigAttribute> configAttributes) {
    // 1. 获取接口需要的权限
    String needAuthority = configAttribute.getAttribute();  // 如 "1:商品添加"
    
    // 2. 从 Authentication 获取用户权限列表
    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        // 3. ★ 比对：用户权限 vs 接口所需权限
        if (needAuthority.trim().equals(grantedAuthority.getAuthority())) {
            return;  // 匹配成功，放行
        }
    }
    throw new AccessDeniedException("抱歉，您没有访问权限");
}
```

**使用的信息来源**：
- `authentication.getAuthorities()` → 来自 JWT Token 解析后加载的用户权限

---

### 使用场景 4：Controller 中获取当前用户

**方式 A：通过 Principal 参数自动注入**

```java
@ApiOperation("获取当前登录用户信息")
@RequestMapping(value = "/info", method = RequestMethod.GET)
@ResponseBody
public CommonResult<UmsAdmin> info(Principal principal) {
    // Spring MVC 自动从 SecurityContextHolder 注入
    String username = principal.getName();  // "admin"
    UmsAdmin admin = adminService.getAdminByUsername(username);
    return CommonResult.success(admin);
}
```

**方式 B：手动从 SecurityContextHolder 获取**

```java
@ApiOperation("刷新 token")
@RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
@ResponseBody
public CommonResult refreshToken(HttpServletRequest request) {
    String token = request.getHeader(tokenHeader);
    String refreshToken = adminService.refreshToken(token);
    
    // 手动获取当前用户
    UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    
    String username = userDetails.getUsername();  // "admin"
    // ...
}
```

---

### 使用场景 5：Token 刷新

**源码位置**：[`UmsAdminController.refreshToken()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/controller/UmsAdminController.java#L77-L93)

```java
@ApiOperation("刷新 token")
@RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
@ResponseBody
public CommonResult refreshToken(HttpServletRequest request) {
    String token = request.getHeader(tokenHeader);
    String refreshToken = adminService.refreshToken(token);
    
    UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal();
    
    if (refreshToken == null) {
        // Token 已过期，删除 Redis 中的 token
        umsAdminCacheService.delToken(userDetails.getUsername());
        return CommonResult.failed("token 已经过期！");
    }
    
    // 更新 Redis 中的 token
    umsAdminCacheService.setToken(userDetails.getUsername(), tokenHead + refreshToken);
    
    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", refreshToken);
    tokenMap.put("tokenHead", tokenHead);
    return CommonResult.success(tokenMap);
}
```

---

### 使用场景 6：登出时删除 Token

**源码位置**：[`UmsAdminCacheService.delToken()`](file:///d:/mall-cloud-alibaba/mall-cloud-alibaba/mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/service/impl/UmsAdminCacheServiceImpl.java#L130-L141)

```java
@Override
public void delToken(String username) {
    String key = REDIS_DATABASE + ":" + REDIS_KEY_TOKEN + ":" + username;
    redisService.del(key);  // 删除 Redis 中的 token
}
```

**前端登出**：[`user.js`](file:///d:/mall-cloud-alibaba/mall-admin-web/src/store/modules/user.js#L60-L71)

```javascript
LogOut({ commit, state }) {
  return new Promise((resolve, reject) => {
    logout(state.token).then(() => {
      commit('SET_TOKEN', '')      // 清空 Vuex 中的 token
      commit('SET_ROLES', [])      // 清空角色
      removeToken()                // 删除 Cookie 中的 token
      resolve()
    }).catch(error => {
      reject(error)
    })
  })
}
```

---

## 三、JWT Token 的生命周期

### 完整生命周期图

```
┌─────────────────────────────────────────────────────────────────┐
│ ① 登录                                                         │
│    前端：POST /admin/login                                     │
│    后端：生成 JWT Token                                        │
│         → 存入 Redis（key: "mall:token:admin"）                │
│         → 返回前端（tokenHead + token）                        │
│    前端：存入 Cookie（key: "loginToken"）                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ ② 后续请求                                                     │
│    前端：每次请求携带 Token（Authorization 请求头）             │
│    后端：JwtAuthenticationTokenFilter 解析 Token                │
│         → 验证 Token 有效性                                    │
│         → 加载用户权限                                         │
│         → 放入 SecurityContextHolder                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ ③ Token 刷新（可选）                                           │
│    时机：Token 快过期时                                        │
│    前端：GET /admin/refreshToken                               │
│    后端：生成新 Token，更新 Redis                               │
│    前端：更新 Cookie 中的 Token                                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ ④ Token 失效                                                   │
│    情况 1：Token 过期（超过 expiration 时间）                   │
│    情况 2：用户主动登出（删除 Redis 和 Cookie 中的 Token）       │
│    情况 3：服务端强制失效（删除 Redis 中的 Token）              │
│    结果：后续请求会被拦截，返回 401，前端跳转登录页             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、Token 的有效期配置

**配置文件**：`application.yml` 或 `application.properties`

```yaml
jwt:
  secret: your-secret-key  # 签名密钥
  expiration: 604800       # 过期时间（秒）= 7 天
  tokenHeader: Authorization
  tokenHead: 'Bearer '
```

**过期时间计算**：
```java
private Date generateExpirationDate() {
    return new Date(System.currentTimeMillis() + expiration * 1000);
    // 当前时间 + 604800 * 1000 = 7 天后
}
```

---

## 五、Token 的双重验证机制

### 1. JWT 本身的验证

```java
public boolean validateToken(String token, UserDetails userDetails) {
    String username = getUserNameFromToken(token);
    // 验证 1：用户名是否匹配
    // 验证 2：Token 是否过期
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
}
```

### 2. Redis 中的 Token 验证

```java
// 登录时
adminCacheService.setToken(username, tokenHead + token);  // 存入 Redis

// 登出时
adminCacheService.delToken(username);  // 删除 Redis

// 过滤器中（可选）
String redisToken = redisService.get(key);
if (!redisToken.equals(tokenHead + token)) {
    throw new AccessDeniedException("Token 已失效");
}
```

**双重验证的作用**：
- **JWT 验证**：防止 Token 被篡改、过期
- **Redis 验证**：支持服务端主动使 Token 失效（如登出、修改密码、封禁用户）

---

## 六、总结

### JWT Token 的使用链路

| 阶段 | 操作 | 位置 |
|------|------|------|
| **生成** | 登录时生成 JWT Token | `UmsAdminServiceImpl.login()` |
| **存储** | 存入 Redis + 返回前端 | `adminCacheService.setToken()` + Cookie |
| **携带** | 每个请求携带 Token | `request.js` 拦截器 |
| **解析** | 解析 Token 获取用户名 | `JwtAuthenticationTokenFilter` |
| **验证** | 验证 Token 有效性 | `JwtTokenUtil.validateToken()` |
| **加载权限** | 从数据库加载用户权限 | `loadUserByUsername()` |
| **权限比对** | 比对用户权限和接口权限 | `DynamicAccessDecisionManager.decide()` |
| **获取用户** | Controller 中获取当前用户 | `Principal` 参数或 `SecurityContextHolder` |
| **刷新** | Token 快过期时刷新 | `UmsAdminController.refreshToken()` |
| **删除** | 登出时删除 Token | `adminCacheService.delToken()` |

### Token 的生命周期

```
登录生成 → 前端存储 → 每次请求携带 → 后端解析验证 → 加载权限 → 权限决策
     ↑                                                                      ↓
     └────────────────── 刷新 Token ←───────────────────────────────────┘
     
失效条件：
1. Token 过期（7 天）
2. 用户主动登出
3. 服务端强制失效（删除 Redis）
```

**核心作用**：JWT Token 是**无状态认证**的载体，将用户身份信息编码到 Token 中，服务端无需 Session 即可识别用户身份和权限。

---

## 七、关键代码文件索引

| 文件 | 作用 | 路径 |
|------|------|------|
| `JwtTokenUtil.java` | JWT Token 生成、验证、解析工具类 | `mall-security/src/main/java/com/mtcarpenter/mall/security/util/JwtTokenUtil.java` |
| `JwtAuthenticationTokenFilter.java` | JWT 认证过滤器，解析 Token 并加载权限 | `mall-security/src/main/java/com/mtcarpenter/mall/security/component/JwtAuthenticationTokenFilter.java` |
| `UmsAdminServiceImpl.java` | 登录逻辑，生成 Token | `mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/service/impl/UmsAdminServiceImpl.java` |
| `UmsAdminController.java` | Token 刷新接口 | `mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/controller/UmsAdminController.java` |
| `UmsAdminCacheServiceImpl.java` | Token 的 Redis 存储 | `mall-admin-ums/ums-server/src/main/java/com/mtcarpenter/mall/service/impl/UmsAdminCacheServiceImpl.java` |
| `request.js` | 前端请求拦截器，携带 Token | `mall-admin-web/src/utils/request.js` |
| `user.js` | 前端 Token 管理（登录/登出） | `mall-admin-web/src/store/modules/user.js` |
| `auth.js` | 前端 Token 存取工具 | `mall-admin-web/src/utils/auth.js` |
