package com.practice.metaphor.v2.service;

import com.lmax.disruptor.RingBuffer;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import com.practice.metaphor.v2.engine.AccountBookV2;
import com.practice.metaphor.v2.exception.InsufficientBalanceExceptionV2;
import com.practice.metaphor.v2.engine.MarketBookV2;
import com.practice.metaphor.v2.exception.BusinessExceptionV2;
import com.practice.metaphor.v2.strategy.FreezeStrategyFactoryV2;
import com.practice.metaphor.v2.strategy.model.FreezeResult;
import com.practice.metaphor.v2.util.SnowflakeIdGeneratorV2;
import com.practice.metaphor.v2.model.entity.MarketV2;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;

/**
 * V2 交易服務（HTTP 入口）
 *
 * <p>
 * 下單流程（100% Zero-DB 熱路徑）：
 * <ol>
 * <li>查詢市場規則（純記憶體讀取）</li>
 * <li>使用策略模式計算需要凍結的資產與金額</li>
 * <li>{@link AccountBookV2#checkAndFreeze} — 純記憶體驗證與凍結</li>
 * <li>{@link SnowflakeIdGeneratorV2#nextId()} — 產生唯一訂單 ID</li>
 * <li>發布 {@link OrderCommandEventV2} 至 Input RingBuffer</li>
 * </ol>
 */
@Service
public class TradingServiceV2 {

    private final AccountBookV2 accountBook;
    private final MarketBookV2 marketBook;
    private final FreezeStrategyFactoryV2 strategyFactory;
    private final SnowflakeIdGeneratorV2 idGenerator;
    private final RingBuffer<OrderCommandEventV2> inputRingBuffer;

    public TradingServiceV2(AccountBookV2 accountBook,
            MarketBookV2 marketBook,
            FreezeStrategyFactoryV2 strategyFactory,
            SnowflakeIdGeneratorV2 idGenerator,
            RingBuffer<OrderCommandEventV2> inputRingBuffer) {
        this.accountBook = accountBook;
        this.marketBook = marketBook;
        this.strategyFactory = strategyFactory;
        this.idGenerator = idGenerator;
        this.inputRingBuffer = inputRingBuffer;
    }

    /**
     * 下委託單。
     */
    public void placeOrder(Long traderId, Long marketId, int type, int side,
            BigDecimal price, BigDecimal qty) {

        /* 1. 查詢市場規則（純記憶體讀取，Zero-DB I/O） */
        MarketV2 market = marketBook.getMarket(marketId);

        /* 2. 使用策略模式計算凍結資產與金額 */
        FreezeResult freeze = strategyFactory.getStrategy(type)
                .calculate(side, price, qty, market);

        /* 3. 記憶體驗證與凍結（可能拋出 InsufficientBalanceExceptionV2） */
        try {
            accountBook.checkAndFreeze(traderId, freeze.assetId(), freeze.amount());
        } catch (InsufficientBalanceExceptionV2 e) {
            throw new BusinessExceptionV2(e.getMessage());
        }

        /* 4. 產生唯一訂單 ID（Snowflake） */
        long orderId = idGenerator.nextId();

        /* 5. 發布至 Input Disruptor RingBuffer（非同步） */
        long sequence = inputRingBuffer.next();
        try {
            OrderCommandEventV2 event = inputRingBuffer.get(sequence);
            event.reset();
            event.setCommandType(type);
            event.setOrderId(orderId);
            event.setTraderId(traderId);
            event.setBaseAssetId(market.baseAssetId());
            event.setQuoteAssetId(market.quoteAssetId());
            event.setSide(side);
            event.setPrice(price);
            event.setTotalQty(qty);
        } finally {
            inputRingBuffer.publish(sequence);
        }
    }
}
