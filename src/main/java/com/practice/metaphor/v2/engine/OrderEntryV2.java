package com.practice.metaphor.v2.engine;

import java.math.BigDecimal;

/**
 * OrderBook 中的記憶體訂單物件。
 * 輕量化設計，僅保留撮合必要的欄位。
 */
public class OrderEntryV2 {

    private final long orderId;
    private final long traderId;
    private final long baseAssetId;
    private final long quoteAssetId;
    private final int side;
    private final BigDecimal price;
    private final BigDecimal totalQty;
    private BigDecimal filledQty;

    public OrderEntryV2(long orderId, long traderId, long baseAssetId, long quoteAssetId,
                        int side, BigDecimal price, BigDecimal totalQty) {
        this.orderId = orderId;
        this.traderId = traderId;
        this.baseAssetId = baseAssetId;
        this.quoteAssetId = quoteAssetId;
        this.side = side;
        this.price = price;
        this.totalQty = totalQty;
        this.filledQty = BigDecimal.ZERO;
    }

    public BigDecimal remainingQty() {
        return totalQty.subtract(filledQty);
    }

    public boolean isFilled() {
        return filledQty.compareTo(totalQty) >= 0;
    }

    public void addFilledQty(BigDecimal qty) {
        this.filledQty = this.filledQty.add(qty);
    }

    public long getOrderId() { return orderId; }
    public long getTraderId() { return traderId; }
    public long getBaseAssetId() { return baseAssetId; }
    public long getQuoteAssetId() { return quoteAssetId; }
    public int getSide() { return side; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getTotalQty() { return totalQty; }
    public BigDecimal getFilledQty() { return filledQty; }
    public void setFilledQty(BigDecimal filledQty) { this.filledQty = filledQty; }
}
