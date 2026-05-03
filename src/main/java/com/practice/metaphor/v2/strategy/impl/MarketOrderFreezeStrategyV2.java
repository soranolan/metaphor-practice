package com.practice.metaphor.v2.strategy.impl;

import com.practice.metaphor.v2.exception.BusinessExceptionV2;
import com.practice.metaphor.v2.model.entity.MarketV2;
import com.practice.metaphor.v2.strategy.OrderFreezeStrategyV2;
import com.practice.metaphor.v2.strategy.model.FreezeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 市價單凍結策略 (type=1)
 */
@Component
public class MarketOrderFreezeStrategyV2 implements OrderFreezeStrategyV2 {

    @Override
    public FreezeResult calculate(int side, BigDecimal price, BigDecimal qty, MarketV2 market) {
        if (side == 0) { // BUY
            // 市價買入：目前強制拋錯，要求明確定義（花費金額或預估價）
            throw new BusinessExceptionV2("【交易失敗】市價買入需指定總花費金額或預估價格，目前尚未支援此下單方式");
        } else { // SELL
            // 賣出：凍結數量 (Base Asset)
            return new FreezeResult(market.baseAssetId(), qty);
        }
    }
}
