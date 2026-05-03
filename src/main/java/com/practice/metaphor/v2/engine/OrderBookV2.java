package com.practice.metaphor.v2.engine;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;

/**
 * 純記憶體訂單簿（Price-Time Priority）
 *
 * <p>bids（買盤）：降冪排列，最高出價優先。
 * <p>asks（賣盤）：升冪排列，最低賣價優先。
 *
 * <p>注意：此類別僅由 MatchingEngineHandlerV2（單一執行緒）操作，無需加鎖。
 */
public class OrderBookV2 {

    /** 買盤：降冪（最高買價在最前） */
    private final TreeMap<BigDecimal, Deque<OrderEntryV2>> bids =
            new TreeMap<>((a, b) -> b.compareTo(a));

    /** 賣盤：升冪（最低賣價在最前） */
    private final TreeMap<BigDecimal, Deque<OrderEntryV2>> asks =
            new TreeMap<>();

    // -------------------------------------------------------------------------
    // 公開操作
    // -------------------------------------------------------------------------

    /**
     * 新增一筆限價委託至 OrderBook。
     * side 0 = BUY → 放入 bids；side 1 = SELL → 放入 asks。
     */
    public void addOrder(OrderEntryV2 order) {
        TreeMap<BigDecimal, Deque<OrderEntryV2>> book = (order.getSide() == 0) ? bids : asks;
        book.computeIfAbsent(order.getPrice(), k -> new ArrayDeque<>()).addLast(order);
    }

    /**
     * 從 OrderBook 移除指定委託（撤單或成交後清除）。
     */
    public void removeOrder(OrderEntryV2 order) {
        TreeMap<BigDecimal, Deque<OrderEntryV2>> book = (order.getSide() == 0) ? bids : asks;
        Deque<OrderEntryV2> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) {
                book.remove(order.getPrice());
            }
        }
    }

    /**
     * 限價單撮合：找出所有可與 taker 成交的 maker，回傳成交記錄清單。
     * 此方法不修改 AccountBook，由呼叫方（MatchingEngineHandlerV2）處理結算。
     *
     * @param taker 新進的吃單
     * @return 每筆成交紀錄（maker + matchQty + matchPrice）
     */
    public List<MatchResultV2> matchLimit(OrderEntryV2 taker) {
        List<MatchResultV2> results = new ArrayList<>();

        /* taker 是買單 → 掃 asks（最低賣價優先）；taker 是賣單 → 掃 bids（最高買價優先） */
        TreeMap<BigDecimal, Deque<OrderEntryV2>> counterBook = (taker.getSide() == 0) ? asks : bids;

        while (taker.remainingQty().compareTo(BigDecimal.ZERO) > 0 && !counterBook.isEmpty()) {
            BigDecimal bestPrice = counterBook.firstKey();

            /* 價格條件：買單要求 bestAsk <= takerPrice；賣單要求 bestBid >= takerPrice */
            boolean priceMatches = (taker.getSide() == 0)
                    ? bestPrice.compareTo(taker.getPrice()) <= 0
                    : bestPrice.compareTo(taker.getPrice()) >= 0;

            if (!priceMatches) break;

            Deque<OrderEntryV2> queue = counterBook.get(bestPrice);
            while (!queue.isEmpty() && taker.remainingQty().compareTo(BigDecimal.ZERO) > 0) {
                OrderEntryV2 maker = queue.peekFirst();

                /* 避免自成交 */
                if (maker.getTraderId() == taker.getTraderId()) {
                    queue.pollFirst();
                    continue;
                }

                BigDecimal matchQty = taker.remainingQty().min(maker.remainingQty());
                BigDecimal matchPrice = maker.getPrice();

                taker.addFilledQty(matchQty);
                maker.addFilledQty(matchQty);

                results.add(new MatchResultV2(maker, matchQty, matchPrice));

                if (maker.isFilled()) {
                    queue.pollFirst();
                }
            }
            if (queue.isEmpty()) {
                counterBook.remove(bestPrice);
            }
        }
        return results;
    }

    /**
     * 市價單撮合：不限制價格，直接吃對手盤直到 qty 用盡或對手盤清空。
     *
     * @param taker 市價單（price 欄位忽略）
     * @return 每筆成交紀錄
     */
    public List<MatchResultV2> matchMarket(OrderEntryV2 taker) {
        List<MatchResultV2> results = new ArrayList<>();
        TreeMap<BigDecimal, Deque<OrderEntryV2>> counterBook = (taker.getSide() == 0) ? asks : bids;

        while (taker.remainingQty().compareTo(BigDecimal.ZERO) > 0 && !counterBook.isEmpty()) {
            BigDecimal bestPrice = counterBook.firstKey();
            Deque<OrderEntryV2> queue = counterBook.get(bestPrice);

            while (!queue.isEmpty() && taker.remainingQty().compareTo(BigDecimal.ZERO) > 0) {
                OrderEntryV2 maker = queue.peekFirst();

                if (maker.getTraderId() == taker.getTraderId()) {
                    queue.pollFirst();
                    continue;
                }

                BigDecimal matchQty = taker.remainingQty().min(maker.remainingQty());
                BigDecimal matchPrice = maker.getPrice();

                taker.addFilledQty(matchQty);
                maker.addFilledQty(matchQty);

                results.add(new MatchResultV2(maker, matchQty, matchPrice));

                if (maker.isFilled()) {
                    queue.pollFirst();
                }
            }
            if (queue.isEmpty()) {
                counterBook.remove(bestPrice);
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // 快照支援（供 SnapshotServiceV2 使用）
    // -------------------------------------------------------------------------

    public TreeMap<BigDecimal, Deque<OrderEntryV2>> getBids() { return bids; }
    public TreeMap<BigDecimal, Deque<OrderEntryV2>> getAsks() { return asks; }

    // -------------------------------------------------------------------------
    // 內部成交記錄
    // -------------------------------------------------------------------------

    public record MatchResultV2(OrderEntryV2 maker, BigDecimal matchQty, BigDecimal matchPrice) {}
}
