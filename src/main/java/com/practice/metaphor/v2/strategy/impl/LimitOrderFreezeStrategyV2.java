package com.practice.metaphor.v2.strategy.impl;

import com.practice.metaphor.v2.exception.BusinessExceptionV2;
import com.practice.metaphor.v2.model.entity.MarketV2;
import com.practice.metaphor.v2.strategy.OrderFreezeStrategyV2;
import com.practice.metaphor.v2.strategy.model.FreezeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 限價單凍結策略 (type=0)
 */
@Component
public class LimitOrderFreezeStrategyV2 implements OrderFreezeStrategyV2 {

    @Override
    public FreezeResult calculate(int side, BigDecimal price, BigDecimal qty, MarketV2 market) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessExceptionV2("【交易失敗】限價單必須指定有效價格");
        }

        if (side == 0) { // BUY
            // 買入：凍結價格 * 數量 (Quote Asset)
            return new FreezeResult(market.quoteAssetId(), price.multiply(qty));
        } else { // SELL
            // 賣出：凍結數量 (Base Asset)
            return new FreezeResult(market.baseAssetId(), qty);
        }
    }
}
