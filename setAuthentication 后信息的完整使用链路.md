# `setAuthentication` 后信息的完整使用链路

## 核心结论

**`setAuthentication` 放入的信息，会在当前请求的后续处理中被多次使用**，主要有 3 个使用点：

---

## 使用点 1：动态权限决策（立即使用）

**时机**：`DynamicSecurityFilter` → `DynamicAccessDecisionManager.decide()`

**代码链路**：

```java
// ① JwtAuthenticationTokenFilter 设置 Authentication
SecurityContextHolder.getContext().setAuthentication(authentication);
// authentication = {
//   principal: AdminUserDetails {
//     umsAdmin: {id:1, username:"admin", ...},
//     resourceList: [{id:1, name:"商品添加", url:"/product/create"}, ...]
//   },
//   authorities: ["1:商品添加", "3:商品删除", ...]
// }

// ② DynamicSecurityFilter 执行
public void doFilter(...) {
    // 调用父类的 beforeInvocation
    InterceptorStatusToken token = super.beforeInvocation(fi);
    //                    ↑
    //                    内部会调用 AccessDecisionManager.decide()
}

// ③ DynamicAccessDecisionManager.decide() 使用 Authentication
public void decide(Authentication authentication, Object object,
                   Collection<ConfigAttribute> configAttributes) {
    // ★ 从 Authentication 获取用户权限
    String needAuthority = configAttribute.getAttribute();  // 如 "1:商品添加"
    
    for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        // ★ 比对：用户权限 vs 接口所需权限
        if (needAuthority.trim().equals(grantedAuthority.getAuthority())) {
            return;  // 匹配成功，放行
        }
    }
    throw new AccessDeniedException("抱歉，您没有访问权限");
}
```

**使用的信息**：
- `authentication.getAuthorities()` → 用户权限列表 `["1:商品添加", "3:商品删除", ...]`

**时间点**：**每次请求经过 `DynamicSecurityFilter` 时立即使用**

---

## 使用点 2：Controller 中获取当前用户信息（业务使用）

### 场景 A：通过 `Principal` 参数自动注入

```java
@ApiOperation("获取当前登录用户信息")
@RequestMapping(value = "/info", method = RequestMethod.GET)
@ResponseBody
public CommonResult<UmsAdmin> info(Principal principal) {
    // ★ Spring MVC 自动从 SecurityContextHolder 注入
    // 等价于：
    // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // UserDetails userDetails = (UserDetails) auth.getPrincipal();
    
    String username = principal.getName();  // 获取用户名
    // 后续可以根据 username 查询完整用户信息
    UmsAdmin admin = adminService.getAdminByUsername(username);
    return CommonResult.success(admin);
}
```

**Spring MVC 的自动注入原理**：

```java
// Spring MVC 内部处理逻辑
public class HandlerMethod {
    public Object invoke(HttpServletRequest request, ...) {
        // 检测参数类型
        if (parameterType == Principal.class) {
            // 从 SecurityContextHolder 获取
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth.getPrincipal();  // 返回 AdminUserDetails
        }
    }
}
```

---

### 场景 B：手动从 `SecurityContextHolder` 获取

```java
@ApiOperation("刷新 token")
@RequestMapping(value = "/refreshToken", method = RequestMethod.GET)
@ResponseBody
public CommonResult refreshToken(HttpServletRequest request) {
    String token = request.getHeader(tokenHeader);
    String refreshToken = adminService.refreshToken(token);
    
    // ★ 手动从 SecurityContextHolder 获取用户信息
    UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    
    if (refreshToken == null) {
        umsAdminCacheService.delToken(userDetails.getUsername());
        return CommonResult.failed("token 已经过期！");
    }
    
    Map<String, String> tokenMap = new HashMap<>();
    tokenMap.put("token", refreshToken);
    tokenMap.put("tokenHead", tokenHead);
    umsAdminCacheService.setToken(userDetails.getUsername(), tokenHead + refreshToken);
    return CommonResult.success(tokenMap);
}
```

**获取的信息**：
- `getPrincipal()` → `AdminUserDetails` 对象
- `userDetails.getUsername()` → 用户名

---

### 场景 C：Service 层获取当前用户

```java
@Override
public UmsMember getCurrentMember() {
    // ★ 从 SecurityContextHolder 获取当前用户
    SecurityContext ctx = SecurityContextHolder.getContext();
    Authentication auth = ctx.getAuthentication();
    MemberDetails memberDetails = (MemberDetails) auth.getPrincipal();
    return memberDetails.getUmsMember();
}
```

**使用场景**：
- 获取当前登录用户 ID
- 记录操作日志（谁在什么时候做了什么）
- 基于用户的业务逻辑（如查看"我的订单"）

---

## 使用点 3：Spring Security 内部使用（隐式使用）

### 场景 A：`@PreAuthorize` 注解

```java
@RestController
@RequestMapping("/product")
public class UmsProductController {
    
    @PreAuthorize("hasAuthority('1:商品添加')")
    @PostMapping("/create")
    public CommonResult create(@RequestBody UmsProduct product) {
        // 只有拥有 "1:商品添加" 权限的用户才能访问
        // Spring Security 会自动从 SecurityContextHolder 获取 Authentication
        // 并检查是否包含指定权限
    }
}
```

**内部原理**：

```java
// Spring Security 的 PreAuthorize 拦截器
public class PreAuthorizeInterceptor {
    public boolean preHandle(JoinPoint joinPoint) {
        // 获取方法上的 @PreAuthorize 注解
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        String expression = annotation.value();  // 如 "hasAuthority('1:商品添加')"
        
        // 从 SecurityContextHolder 获取 Authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 执行 SpEL 表达式校验
        return expressionHandler.getExpressionEvaluator()
            .hasAuthority(auth, "1:商品添加");
    }
}
```

---

### 场景 B：方法级权限控制

```java
@Service
public class UmsOrderServiceImpl implements UmsOrderService {
    
    @PreAuthorize("authentication.principal.username == #username")
    public List<UmsOrder> getOrdersByUsername(String username) {
        // 只能查询当前登录用户的订单
        // Spring Security 会校验 authentication.principal.username 是否等于参数 username
    }
}
```

---

## 完整使用流程图

```
请求：POST /product/create
     │
     ▼
① JwtAuthenticationTokenFilter.doFilterInternal()
     │ 解析 JWT Token
     │ 调用 loadUserByUsername("admin")
     │   → 返回 AdminUserDetails {
     │        umsAdmin: {id:1, username:"admin"},
     │        resourceList: [{id:1, name:"商品添加", ...}, ...]
     │     }
     │ 构建 Authentication
     │   authentication = new UsernamePasswordAuthenticationToken(
     │     userDetails,      // AdminUserDetails
     │     null,             // credentials
     │     userDetails.getAuthorities()  // ["1:商品添加", "3:商品删除", ...]
     │   )
     │
     │ ★ setAuthentication(authentication)
     │   SecurityContextHolder {
     │     ThreadLocal<SecurityContext> {
     │       context: {
     │         authentication: authentication  ← 放入这里
     │       }
     │     }
     │   }
     │
     ▼
② DynamicSecurityFilter.doFilter()
     │ 调用 super.beforeInvocation(fi)
     │   │
     │   ▼
     │  DynamicSecurityMetadataSource.getAttributes(fi)
     │     │ 获取当前请求路径 /product/create
     │     │ 查询该路径需要的权限
     │     └─▶ 返回 ["1:商品添加"]
     │
     │   ▼
     │  DynamicAccessDecisionManager.decide(authentication, ...)
     │     │ ★ 使用 authentication.getAuthorities()
     │     │ 比对：用户权限 ["1:商品添加", "3:商品删除", ...]
     │     │       vs 所需权限 ["1:商品添加"]
     │     └─▶ 匹配成功 ✅ 放行
     │
     ▼
③ UmsProductController.create(Principal principal)
     │ Spring MVC 自动注入 Principal
     │   │
     │   ▼
     │   SecurityContextHolder.getContext().getAuthentication()
     │     │ ★ 使用 authentication.getPrincipal()
     │     └─▶ 返回 AdminUserDetails
     │
     │ 业务逻辑：
     │   String username = principal.getName();  // "admin"
     │   // 创建商品...
     │
     ▼
④ 请求结束，Filter 清理
     │ SecurityContextHolder.clearContext()
     │ ThreadLocal.remove()  ← 清理，防止内存泄漏
```

---

## 两次 `setAuthentication` 的对比

### 第一次：登录时

```java
// UmsAdminServiceImpl.login()
public String login(String username, String password) {
    UserDetails userDetails = loadUserByUsername(username);
    // 验证密码...
    
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
    //                          ↑
    //                          放入当前登录线程的 ThreadLocal
    
    // ★ 立即使用：生成 JWT Token
    token = jwtTokenUtil.generateToken(userDetails);
    //                        ↑ 使用 userDetails.getUsername()
    
    // ★ 立即使用：记录登录日志
    insertLoginLog(username);
    //             ↑ 使用 username
    
    return token;
}
```

**使用的信息**：
- `userDetails.getUsername()` → 生成 JWT
- `userDetails.getAuthorities()` → 放入 Authentication（但登录时不会立即使用）

**生命周期**：登录请求结束 → ThreadLocal 清理

---

### 第二次：每次请求鉴权时

```java
// JwtAuthenticationTokenFilter.doFilterInternal()
protected void doFilterInternal(HttpServletRequest request, ...) {
    String username = jwtTokenUtil.getUserNameFromToken(authToken);
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    
    UsernamePasswordAuthenticationToken authentication = 
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    
    SecurityContextHolder.getContext().setAuthentication(authentication);
    //                          ↑
    //                          放入当前请求线程的 ThreadLocal
    
    // ★ 后续使用点：
    // 1. DynamicSecurityFilter → 权限比对
    // 2. Controller → Principal 参数
    // 3. Service → SecurityContextHolder.getAuthentication()
    // 4. @PreAuthorize → 注解权限校验
}
```

**使用的信息**：
- `authentication.getAuthorities()` → 权限比对
- `authentication.getPrincipal()` → 获取用户信息
- `authentication.getName()` → 用户名

**生命周期**：请求结束 → ThreadLocal 清理

---

## 为什么需要 ThreadLocal？

因为 **Spring MVC 是单例多线程** 架构：

```java
// Spring MVC 处理请求
public class DispatcherServlet {
    // 单例的 Controller
    private UmsProductController productController = new UmsProductController();
    
    protected void doDispatch(HttpServletRequest request, ...) {
        // 从线程池获取线程处理请求
        Thread thread = Thread.currentThread();
        
        // 调用 Controller
        productController.create(request, ...);
        //               ↑
        //               如何知道是哪个用户？
        //               → 从 ThreadLocal 获取 Authentication
    }
}
```

**如果没有 ThreadLocal**：
- 每个方法参数都要传递 `Authentication` 或 `username`
- 代码耦合严重，难以维护

**有了 ThreadLocal**：
- 任何地方都能通过 `SecurityContextHolder.getContext().getAuthentication()` 获取
- 代码解耦，优雅简洁

---

## 总结：`setAuthentication` 信息的使用时机

| 使用点 | 位置 | 使用的信息 | 时间点 |
|--------|------|-----------|--------|
| **权限决策** | `DynamicAccessDecisionManager.decide()` | `getAuthorities()` | 请求经过 `DynamicSecurityFilter` 时 |
| **获取用户名** | Controller 的 `Principal` 参数 | `getPrincipal().getName()` | Controller 方法执行时 |
| **获取用户详情** | Controller/Service 手动获取 | `getPrincipal()` | 业务逻辑需要时 |
| **注解权限** | `@PreAuthorize` 注解 | `getAuthorities()` | 方法调用前 |
| **生成 Token** | `JwtTokenUtil.generateToken()` | `getPrincipal().getUsername()` | 登录或刷新 Token 时 |

**核心思想**：`setAuthentication` 后，当前请求线程的任何地方都能通过 `SecurityContextHolder` 获取认证信息，实现**一次认证，全局使用**。
