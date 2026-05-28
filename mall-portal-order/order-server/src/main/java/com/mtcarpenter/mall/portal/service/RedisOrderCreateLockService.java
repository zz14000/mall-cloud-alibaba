package com.mtcarpenter.mall.portal.service;

/**
 * Member-level order creation lock.
 */
public interface RedisOrderCreateLockService {
    boolean tryLock(Long memberId, String requestId, long expireSeconds);

    void release(Long memberId, String requestId);
}
