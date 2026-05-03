package com.practice.metaphor.v2.disruptor.event;

import java.math.BigDecimal;

/**
 * Input RingBuffer 事件槽（OrderCommandEvent）
 *
 * <p>Disruptor 預先分配固定數量的實例，透過 {@link #reset()} 重用，避免 GC。
 * 欄位皆為可變（mutable），由發布者（TradingServiceV2）填入後交由 Handler 消費。
 *
 * <p>commandType 定義：
 * <ul>
 *   <li>0 = PLACE_ORDER_LIMIT（限價單）</li>
 *   <li>1 = PLACE_ORDER_MARKET（市價單）</li>
 *   <li>777 = SNAPSHOT（觸發內部快照）</li>
 * </ul>
 */
public class OrderCommandEventV2 {

    private int commandType;
    private long chronicleIndex; // WAL 分配的真實 index
    private long orderId;
    private long traderId;
    private long baseAssetId;
    private long quoteAssetId;
    private int side;
    private BigDecimal price;
    private BigDecimal totalQty;

    /** 清除事件槽，供下一次發布重用。 */
    public void reset() {
        this.commandType = -1;
        this.chronicleIndex = 0L;
        this.orderId = 0L;
        this.traderId = 0L;
        this.baseAssetId = 0L;
        this.quoteAssetId = 0L;
        this.side = 0;
        this.price = null;
        this.totalQty = null;
    }

    // Getters & Setters
    public int getCommandType() { return commandType; }
    public void setCommandType(int commandType) { this.commandType = commandType; }

    public long getChronicleIndex() { return chronicleIndex; }
    public void setChronicleIndex(long chronicleIndex) { this.chronicleIndex = chronicleIndex; }

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
}
