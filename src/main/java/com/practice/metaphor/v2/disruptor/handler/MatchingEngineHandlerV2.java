package com.practice.metaphor.v2.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import com.practice.metaphor.v2.disruptor.event.TradeResultEventV2;
import com.practice.metaphor.v2.engine.AccountBookV2;
import com.practice.metaphor.v2.engine.OrderBookV2;
import com.practice.metaphor.v2.engine.OrderBookV2.MatchResultV2;
import com.practice.metaphor.v2.engine.OrderEntryV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.practice.metaphor.v2.snapshot.EngineSnapshotV2;
import com.practice.metaphor.v2.snapshot.SnapshotServiceV2;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 撮合引擎 Handler（純記憶體，單執行緒）
 *
 * <p>在 Disruptor Pipeline 中，此 Handler 依賴 {@link JournalWriterHandlerV2}（透過 Barrier），
 * 確保每筆事件「先寫 Journal，再撮合」。
 *
 * <p>多市場支援：以 "baseAssetId:quoteAssetId" 為 key，每個市場維護獨立的 {@link OrderBookV2}。
 *
 * <p>撮合完成後，為每一筆成交對向 Output RingBuffer 發布 {@link TradeResultEventV2}。
 */
public class MatchingEngineHandlerV2 implements EventHandler<OrderCommandEventV2> {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineHandlerV2.class);

    private final AccountBookV2 accountBook;
    private final RingBuffer<TradeResultEventV2> outputBuffer;
    private final SnapshotServiceV2 snapshotService;

    /** key = "baseAssetId:quoteAssetId" */
    private final Map<String, OrderBookV2> orderBooks = new HashMap<>();

    public MatchingEngineHandlerV2(AccountBookV2 accountBook,
                                   RingBuffer<TradeResultEventV2> outputBuffer,
                                   SnapshotServiceV2 snapshotService) {
        this.accountBook = accountBook;
        this.outputBuffer = outputBuffer;
        this.snapshotService = snapshotService;
    }

    /** 供 RecoveryServiceV2 呼叫，預先建立或取得特定市場的 OrderBook。 */
    public OrderBookV2 getOrCreateOrderBook(long baseAssetId, long quoteAssetId) {
        return orderBooks.computeIfAbsent(marketKey(baseAssetId, quoteAssetId), k -> new OrderBookV2());
    }

    /** 供 SnapshotServiceV2 遍歷所有市場的 OrderBook。 */
    public Map<String, OrderBookV2> getOrderBooks() {
        return orderBooks;
    }

    @Override
    public void onEvent(OrderCommandEventV2 event, long sequence, boolean endOfBatch) {
        int commandType = event.getCommandType();

        if (commandType == 0) {
            /* PLACE_ORDER_LIMIT */
            handleLimitOrder(event);
        } else if (commandType == 1) {
            /* PLACE_ORDER_MARKET */
            handleMarketOrder(event);
        } else if (commandType == 777) {
            /* SNAPSHOT_COMMAND */
            handleSnapshot(event);
        } else {
            log.warn("未知的 commandType={}", commandType);
        }
    }

    private void handleSnapshot(OrderCommandEventV2 event) {
        List<EngineSnapshotV2.OrderSnapshotEntryV2> orders = new java.util.ArrayList<>();
        orderBooks.values().forEach(book -> {
            book.getBids().values().forEach(queue -> queue.forEach(o -> orders.add(EngineSnapshotV2.OrderSnapshotEntryV2.from(o))));
            book.getAsks().values().forEach(queue -> queue.forEach(o -> orders.add(EngineSnapshotV2.OrderSnapshotEntryV2.from(o))));
        });

        EngineSnapshotV2 snapshot = new EngineSnapshotV2(
                event.getChronicleIndex(),
                System.currentTimeMillis(),
                accountBook.snapshotAvailable(), // deep copy
                accountBook.snapshotFrozen(),    // deep copy
                orders                           // deep copy
        );
        snapshotService.asyncWriteSnapshotToFile(snapshot);
    }

    // -------------------------------------------------------------------------
    // 限價單處理
    // -------------------------------------------------------------------------

    private void handleLimitOrder(OrderCommandEventV2 event) {
        OrderBookV2 book = getOrCreateOrderBook(event.getBaseAssetId(), event.getQuoteAssetId());

        OrderEntryV2 taker = new OrderEntryV2(
                event.getOrderId(), event.getTraderId(),
                event.getBaseAssetId(), event.getQuoteAssetId(),
                event.getSide(), event.getPrice(), event.getTotalQty()
        );

        /* 先嘗試撮合（taker 主動吃對手盤） */
        List<MatchResultV2> matches = book.matchLimit(taker);

        for (MatchResultV2 match : matches) {
            applyClearing(event, taker, match);
            publishTradeResult(event, taker, match.maker(), match.matchQty(), match.matchPrice());
        }

        /* 限價買單：退還超額凍結（因下單時按 takerPrice 凍結，成交價為 makerPrice） */
        if (event.getSide() == 0 && !matches.isEmpty()) {
            BigDecimal totalMatchQty = taker.getFilledQty();
            BigDecimal refund = event.getPrice().subtract(
                    matches.stream()
                           .map(m -> m.matchPrice().multiply(m.matchQty()))
                           .reduce(BigDecimal.ZERO, BigDecimal::add)
                           .divide(totalMatchQty, 4, RoundingMode.HALF_UP)
            ).multiply(totalMatchQty);
            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                accountBook.release(event.getTraderId(), event.getQuoteAssetId(), refund);
            }
        }

        /* 未完全成交 → 掛入 OrderBook 等待後續對手單 */
        if (!taker.isFilled()) {
            book.addOrder(taker);
        }

        log.debug("限價單 orderId={} 處理完成，filledQty={}/{}",
                event.getOrderId(), taker.getFilledQty(), event.getTotalQty());
    }

    // -------------------------------------------------------------------------
    // 市價單處理
    // -------------------------------------------------------------------------

    private void handleMarketOrder(OrderCommandEventV2 event) {
        OrderBookV2 book = getOrCreateOrderBook(event.getBaseAssetId(), event.getQuoteAssetId());

        /* 市價單 price 設為 null，OrderEntryV2 price 填 null 但 matchMarket 不使用 price */
        OrderEntryV2 taker = new OrderEntryV2(
                event.getOrderId(), event.getTraderId(),
                event.getBaseAssetId(), event.getQuoteAssetId(),
                event.getSide(), BigDecimal.ZERO, event.getTotalQty()
        );

        List<MatchResultV2> matches = book.matchMarket(taker);

        BigDecimal totalMatchAmount = BigDecimal.ZERO;
        for (MatchResultV2 match : matches) {
            applyClearing(event, taker, match);
            publishTradeResult(event, taker, match.maker(), match.matchQty(), match.matchPrice());
            totalMatchAmount = totalMatchAmount.add(match.matchQty().multiply(match.matchPrice()));
        }

        /* 市價買單退款：凍結時按最大金額預留，成交後退還差額 */
        if (event.getSide() == 0) {
            BigDecimal frozenAmount = event.getTotalQty().multiply(event.getPrice()); /* event.getPrice() = 下單預留金額 */
            BigDecimal refund = frozenAmount.subtract(totalMatchAmount);
            if (refund.compareTo(BigDecimal.ZERO) > 0) {
                accountBook.release(event.getTraderId(), event.getQuoteAssetId(), refund);
            }
        }

        /* 市價賣單：未成交部分退還 base asset */
        if (event.getSide() == 1) {
            BigDecimal unfilledQty = taker.remainingQty();
            if (unfilledQty.compareTo(BigDecimal.ZERO) > 0) {
                accountBook.release(event.getTraderId(), event.getBaseAssetId(), unfilledQty);
            }
        }

        log.debug("市價單 orderId={} 處理完成，filledQty={}/{}",
                event.getOrderId(), taker.getFilledQty(), event.getTotalQty());
    }

    // -------------------------------------------------------------------------
    // 結算與發布
    // -------------------------------------------------------------------------

    /**
     * 套用成交結算至 AccountBook。
     */
    private void applyClearing(OrderCommandEventV2 event, OrderEntryV2 taker, MatchResultV2 match) {
        OrderEntryV2 maker = match.maker();
        BigDecimal matchQty = match.matchQty();
        BigDecimal matchPrice = match.matchPrice();
        BigDecimal matchAmount = matchQty.multiply(matchPrice);

        long buyTraderId  = (taker.getSide() == 0) ? taker.getTraderId() : maker.getTraderId();
        long sellTraderId = (taker.getSide() == 1) ? taker.getTraderId() : maker.getTraderId();
        long baseAssetId  = event.getBaseAssetId();
        long quoteAssetId = event.getQuoteAssetId();

        /* 買方：解凍 quote（已成交金額），獲得 base */
        accountBook.settle(buyTraderId, quoteAssetId, matchAmount, baseAssetId, matchQty);

        /* 賣方：解凍 base（已成交數量），獲得 quote */
        accountBook.settle(sellTraderId, baseAssetId, matchQty, quoteAssetId, matchAmount);
    }

    /**
     * 向 Output RingBuffer 發布一筆成交結果。
     */
    private void publishTradeResult(OrderCommandEventV2 event, OrderEntryV2 taker,
                                    OrderEntryV2 maker, BigDecimal matchQty, BigDecimal matchPrice) {
        long seq = outputBuffer.next();
        try {
            TradeResultEventV2 result = outputBuffer.get(seq);
            result.reset();

            result.setTakerOrderId(taker.getOrderId());
            result.setTakerTraderId(taker.getTraderId());
            result.setTakerSide(taker.getSide());
            result.setTakerOrderPrice(event.getPrice());
            result.setTakerTotalFilledQty(taker.getFilledQty());
            result.setTakerNewStatus(taker.isFilled() ? 2 : (taker.getFilledQty().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0));

            result.setMakerOrderId(maker.getOrderId());
            result.setMakerTraderId(maker.getTraderId());
            result.setMakerTotalFilledQty(maker.getFilledQty());
            result.setMakerNewStatus(maker.isFilled() ? 2 : 1);

            result.setBaseAssetId(event.getBaseAssetId());
            result.setQuoteAssetId(event.getQuoteAssetId());
            result.setMatchQty(matchQty);
            result.setMatchPrice(matchPrice);
        } finally {
            outputBuffer.publish(seq);
        }
    }

    // -------------------------------------------------------------------------
    // 工具
    // -------------------------------------------------------------------------

    private String marketKey(long baseAssetId, long quoteAssetId) {
        return baseAssetId + ":" + quoteAssetId;
    }
}
