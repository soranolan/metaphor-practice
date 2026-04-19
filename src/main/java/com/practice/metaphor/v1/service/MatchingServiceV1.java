package com.practice.metaphor.v1.service;

import com.practice.metaphor.v1.model.entity.OrderV1;
import com.practice.metaphor.v1.model.enums.OrderTypeV1;
import com.practice.metaphor.v1.service.strategy.LimitOrderMatchingStrategyV1;
import com.practice.metaphor.v1.service.strategy.MarketOrderMatchingStrategyV1;
import org.springframework.stereotype.Service;

/**
 * 撮合引擎服務 (策略模式工廠)
 */
@Service
public class MatchingServiceV1 {

    private final LimitOrderMatchingStrategyV1 limitStrategy;
    private final MarketOrderMatchingStrategyV1 marketStrategy;

    public MatchingServiceV1(LimitOrderMatchingStrategyV1 limitStrategy, MarketOrderMatchingStrategyV1 marketStrategy) {
        this.limitStrategy = limitStrategy;
        this.marketStrategy = marketStrategy;
    }

    public void match(OrderV1 takerOrder) {
        OrderTypeV1 type = OrderTypeV1.fromValue(takerOrder.getType());
        
        switch(type) {
            case LIMIT:
                limitStrategy.executeMatch(takerOrder);
                break;
            case MARKET:
                marketStrategy.executeMatch(takerOrder);
                break;
        }
    }
}
