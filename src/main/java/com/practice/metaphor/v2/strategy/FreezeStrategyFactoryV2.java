package com.practice.metaphor.v2.strategy;

import com.practice.metaphor.v2.exception.BusinessExceptionV2;
import com.practice.metaphor.v2.strategy.impl.LimitOrderFreezeStrategyV2;
import com.practice.metaphor.v2.strategy.impl.MarketOrderFreezeStrategyV2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 凍結策略工廠
 */
@Component
public class FreezeStrategyFactoryV2 {

    private final Map<Integer, OrderFreezeStrategyV2> strategies = new HashMap<>();

    public FreezeStrategyFactoryV2(LimitOrderFreezeStrategyV2 limitStrategy,
                                   MarketOrderFreezeStrategyV2 marketStrategy) {
        strategies.put(0, limitStrategy);
        strategies.put(1, marketStrategy);
    }

    /**
     * 根據訂單類型獲取策略
     *
     * @param type 0=LIMIT, 1=MARKET
     */
    public OrderFreezeStrategyV2 getStrategy(int type) {
        OrderFreezeStrategyV2 strategy = strategies.get(type);
        if (strategy == null) {
            throw new BusinessExceptionV2("【交易失敗】未知的訂單類型: " + type);
        }
        return strategy;
    }
}
