package com.practice.metaphor.v1.service.strategy;

import com.practice.metaphor.v1.model.entity.OrderV1;

/**
 * 撮合策略介面
 */
public interface MatchingStrategyV1 {
    /**
     * 執行撮合理論 (限價 / 市價)
     */
    void executeMatch(OrderV1 takerOrder);
}
