package com.practice.metaphor.v2.strategy;

import com.practice.metaphor.v2.model.entity.MarketV2;
import com.practice.metaphor.v2.strategy.model.FreezeResult;

import java.math.BigDecimal;

/**
 * 訂單凍結策略介面
 */
public interface OrderFreezeStrategyV2 {
    /**
     * 計算下單需要凍結的資產與金額
     *
     * @param side   方向 (0=BUY, 1=SELL)
     * @param price  價格
     * @param qty    數量
     * @param market 市場配置
     * @return 凍結結果
     */
    FreezeResult calculate(int side, BigDecimal price, BigDecimal qty, MarketV2 market);
}
