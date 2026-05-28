package com.mtcarpenter.mall.portal.service.impl;

import com.mtcarpenter.mall.portal.service.RedisOrderCreateLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis SET NX lock for member order creation.
 */
@Service
public class RedisOrderCreateLockServiceImpl implements RedisOrderCreateLockService {
    private static final String ORDER_CREATE_LOCK_KEY_PREFIX = "mall:lock:order:create:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(Long memberId, String requestId, long expireSeconds) {
        Boolean result = stringRedisTemplate.opsForValue()
        // 对应SET NX命令
        // 1. 仅当键不存在时设置键值对
        //四个参数依次表示，redis的key, value, 过期时间, 过期时间单位
                .setIfAbsent(buildKey(memberId), requestId, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void release(Long memberId, String requestId) {
        //先看看锁里面存的是不是我的 requestId，如果是我的，就删除这个锁；
        // 如果不是，说明锁已经被别人拿走了，我什么都不做
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
        //- DefaultRedisScript<Long> : Spring 提供的 Redis 脚本封装类
        //- setScriptText(script) : 设置 Lua 脚本内容
        //- setResultType(Long.class) : 指定返回类型
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        //-- Lua 脚本中的变量
        //-- KEYS[1]   →  "mall:lock:order:create:123"  (buildKey(memberId))
        //-- ARGV[1]   →  "uuid-A"                      (requestId)
        stringRedisTemplate.execute(redisScript, Collections.singletonList(buildKey(memberId)), requestId);
    }

    private String buildKey(Long memberId) {
        return ORDER_CREATE_LOCK_KEY_PREFIX + memberId;
    }
}
