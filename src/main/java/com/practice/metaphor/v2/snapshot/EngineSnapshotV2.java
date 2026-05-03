package com.practice.metaphor.v2.snapshot;

import com.practice.metaphor.v2.engine.OrderEntryV2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 引擎快照資料結構（供 Chronicle Wire 二進制序列化）
 *
 * <p>記錄快照當下的：
 * <ul>
 *   <li>{@code chronicleIndex}：Chronicle Queue 的寫入位移，供 Replay 從此點繼續</li>
 *   <li>{@code accountBook}：AccountBookV2 的完整餘額狀態</li>
 *   <li>{@code orders}：所有未完全成交的掛單（供 OrderBookV2 重建）</li>
 * </ul>
 */
public class EngineSnapshotV2 {

    private long chronicleIndex;
    private long createdAt; // epoch millis
    private Map<String, BigDecimal> available;
    private Map<String, BigDecimal> frozen;
    private List<OrderSnapshotEntryV2> orders;

    public EngineSnapshotV2() {}

    public EngineSnapshotV2(long chronicleIndex, long createdAt,
                            Map<String, BigDecimal> available, Map<String, BigDecimal> frozen,
                            List<OrderSnapshotEntryV2> orders) {
        this.chronicleIndex = chronicleIndex;
        this.createdAt = createdAt;
        this.available = available;
        this.frozen = frozen;
        this.orders = orders;
    }

    public long getChronicleIndex() { return chronicleIndex; }
    public void setChronicleIndex(long chronicleIndex) { this.chronicleIndex = chronicleIndex; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, BigDecimal> getAvailable() { return available; }
    public void setAvailable(Map<String, BigDecimal> available) { this.available = available; }

    public Map<String, BigDecimal> getFrozen() { return frozen; }
    public void setFrozen(Map<String, BigDecimal> frozen) { this.frozen = frozen; }

    public List<OrderSnapshotEntryV2> getOrders() { return orders; }
    public void setOrders(List<OrderSnapshotEntryV2> orders) { this.orders = orders; }

    // -------------------------------------------------------------------------
    // 內嵌：單筆掛單的序列化結構
    // -------------------------------------------------------------------------

    public static class OrderSnapshotEntryV2 {
        private long orderId;
        private long traderId;
        private long baseAssetId;
        private long quoteAssetId;
        private int side;
        private BigDecimal price;
        private BigDecimal totalQty;
        private BigDecimal filledQty;

        public OrderSnapshotEntryV2() {}

        public static OrderSnapshotEntryV2 from(OrderEntryV2 entry) {
            OrderSnapshotEntryV2 s = new OrderSnapshotEntryV2();
            s.orderId = entry.getOrderId();
            s.traderId = entry.getTraderId();
            s.baseAssetId = entry.getBaseAssetId();
            s.quoteAssetId = entry.getQuoteAssetId();
            s.side = entry.getSide();
            s.price = entry.getPrice();
            s.totalQty = entry.getTotalQty();
            s.filledQty = entry.getFilledQty();
            return s;
        }

        public OrderEntryV2 toOrderEntry() {
            OrderEntryV2 e = new OrderEntryV2(orderId, traderId, baseAssetId, quoteAssetId,
                                              side, price, totalQty);
            e.setFilledQty(filledQty);
            return e;
        }

        public long getOrderId() { return orderId; }
        public void setOrderId(long orderId) { this.orderId = orderId; }

        public long getTraderId() { return traderId; }
        public void setTraderId(long traderId) { this.traderId = traderId; }

        public long getBaseAssetId() { return baseAssetId; }
        public void setBaseAssetId(long baseAssetId) { this.baseAssetId = baseAssetId; }

        public long getQuoteAssetId() { return quoteAssetId; }
        public void setQuoteAssetId(long quoteAssetId) { this.quoteAssetId = quoteAssetId; }

        public int getSide() { return side; }
        public void setSide(int side) { this.side = side; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public BigDecimal getTotalQty() { return totalQty; }
        public void setTotalQty(BigDecimal totalQty) { this.totalQty = totalQty; }

        public BigDecimal getFilledQty() { return filledQty; }
        public void setFilledQty(BigDecimal filledQty) { this.filledQty = filledQty; }
    }
}
