package com.mtcarpenter.mall.filter;

import com.mtcarpenter.mall.common.api.ResultCode;
import com.mtcarpenter.mall.common.exception.ApiException;
import com.mtcarpenter.mall.config.IgnoreUrlsConfig;
import com.mtcarpenter.mall.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;



/**
 * @author mtcarpenter
 * @github https://github.com/mtcarpenter/mall-cloud-alibaba
 * @desc 微信公众号：山间木匠
 */
@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private IgnoreUrlsConfig ignoreUrlsConfig;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    @Value("${redis.key.token}")
    private String REDIS_KEY_TOKEN;

    /**
     * 认证全局过滤器
     * @param exchange 交换对象，Spring WebFlux 的核心接口，封装了 HTTP 请求和响应的所有信息
     * @param chain 管道链，用于调用下一个过滤器或路由
     * @return Mono<Void> 异步处理结果 ，void 表示无返回值 Mono是WebFlux响应式编程模型中的核心类型
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //防止 OPTIONS 请求直接放行
        //浏览器在发送跨域请求前，会先发一个 OPTIONS 预检请求（preflight request），询问服务器是否允许跨域
        if (request.getMethod().equals(HttpMethod.OPTIONS)) {
            return chain.filter(exchange);
        }
        //白名单请求直接放行
        // 1. 从配置类中获取白名单路径
        // 2. 从请求中获取路径
        // 3. 匹配路径是否匹配白名单路径
        //pathMatcher 创建一个 Ant 风格路径匹配器
        PathMatcher pathMatcher = new AntPathMatcher();
        for (String path : ignoreUrlsConfig.getUrls()) {
            if (pathMatcher.match("/**" + path, request.getPath().toString())) {
                return chain.filter(exchange);
            }
        }
        String token = request.getHeaders().getFirst(tokenHeader);
        // 1. 从请求头获取 token
        // 2. 验证 token 是否为空
        // 3. 从 token 中获取用户名
        // 4. 从 Redis 中获取 token
        // 5. 验证 token 是否匹配
        if (StringUtils.isBlank(token)){        //判断是否为空
            log.error("token = {}",token);
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        String username = jwtTokenUtil.getUserNameFromToken(token); //提取用户名
        //构建 Redis Key 查询已存储的 Token 
        String key = REDIS_DATABASE + ":" + REDIS_KEY_TOKEN + ":" + username;
        // 从 Redis 中获取 token 并验证是否匹配 即 token 是否过期，用来查看用户登录状态是否还在
        String resultToken = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(resultToken)) {
            log.error("resultToken = {}",resultToken);
            throw new ApiException(ResultCode.UNAUTHORIZED);
        }
        log.error("resultToken = {}",resultToken);
        return chain.filter(exchange);//处理下一个过滤
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


}
