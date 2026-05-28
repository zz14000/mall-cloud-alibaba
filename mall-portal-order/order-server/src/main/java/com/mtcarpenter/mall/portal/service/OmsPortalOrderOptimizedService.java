package com.mtcarpenter.mall.portal.service;

import com.mtcarpenter.mall.portal.domain.OrderParam;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Optimized order service based on Redis stock reservation and MQ.
 */
public interface OmsPortalOrderOptimizedService {
    @Transactional
    Map<String, Object> generateOrder(OrderParam orderParam);

    @Transactional
    Integer paySuccess(Long orderId, Integer payType);

    @Transactional
    void cancelOrder(Long orderId);

    void sendDelayMessageCancelOrder(Long orderId);
}
