package com.practice.metaphor.v2.disruptor.event;

import java.math.BigDecimal;

/**
 * Output RingBuffer 事件槽（TradeResultEvent）
 *
 * <p>MatchingEngineHandlerV2 為每一筆 taker-maker 成交對發布一個此事件，
 * 由 ProjectionHandlerV2 消費後非同步更新 DB（orders / balances 表）。
 */
public class TradeResultEventV2 {

    /* 訂單資訊 */
    private long takerOrderId;
    private long takerTraderId;
    private int  takerSide;          // 0=BUY, 1=SELL
    private BigDecimal takerOrderPrice;      // 下單時的限價（用於退款計算）
    private int  takerNewStatus;     // 此次撮合後 taker 的最新狀態
    private BigDecimal takerTotalFilledQty;  // 累積成交量

    private long makerOrderId;
    private long makerTraderId;
    private int  makerNewStatus;
    private BigDecimal makerTotalFilledQty;

    /* 成交資訊 */
    private long baseAssetId;
    private long quoteAssetId;
    private BigDecimal matchQty;
    private BigDecimal matchPrice;   // maker 的掛單價格（即最終成交價）

    public void reset() {
        takerOrderId = 0; takerTraderId = 0; takerSide = 0;
        takerOrderPrice = null; takerNewStatus = 0; takerTotalFilledQty = null;
        makerOrderId = 0; makerTraderId = 0; makerNewStatus = 0; makerTotalFilledQty = null;
        baseAssetId = 0; quoteAssetId = 0; matchQty = null; matchPrice = null;
    }

    // Getters & Setters
    public long getTakerOrderId() { return takerOrderId; }
    public void setTakerOrderId(long takerOrderId) { this.takerOrderId = takerOrderId; }

    public long getTakerTraderId() { return takerTraderId; }
    public void setTakerTraderId(long takerTraderId) { this.takerTraderId = takerTraderId; }

    public int getTakerSide() { return takerSide; }
    public void setTakerSide(int takerSide) { this.takerSide = takerSide; }

    public BigDecimal getTakerOrderPrice() { return takerOrderPrice; }
    public void setTakerOrderPrice(BigDecimal takerOrderPrice) { this.takerOrderPrice = takerOrderPrice; }

    public int getTakerNewStatus() { return takerNewStatus; }
    public void setTakerNewStatus(int takerNewStatus) { this.takerNewStatus = takerNewStatus; }

    public BigDecimal getTakerTotalFilledQty() { return takerTotalFilledQty; }
    public void setTakerTotalFilledQty(BigDecimal takerTotalFilledQty) { this.takerTotalFilledQty = takerTotalFilledQty; }

    public long getMakerOrderId() { return makerOrderId; }
    public void setMakerOrderId(long makerOrderId) { this.makerOrderId = makerOrderId; }

    public long getMakerTraderId() { return makerTraderId; }
    public void setMakerTraderId(long makerTraderId) { this.makerTraderId = makerTraderId; }

    public int getMakerNewStatus() { return makerNewStatus; }
    public void setMakerNewStatus(int makerNewStatus) { this.makerNewStatus = makerNewStatus; }

    public BigDecimal getMakerTotalFilledQty() { return makerTotalFilledQty; }
    public void setMakerTotalFilledQty(BigDecimal makerTotalFilledQty) { this.makerTotalFilledQty = makerTotalFilledQty; }

    public long getBaseAssetId() { return baseAssetId; }
    public void setBaseAssetId(long baseAssetId) { this.baseAssetId = baseAssetId; }

    public long getQuoteAssetId() { return quoteAssetId; }
    public void setQuoteAssetId(long quoteAssetId) { this.quoteAssetId = quoteAssetId; }

    public BigDecimal getMatchQty() { return matchQty; }
    public void setMatchQty(BigDecimal matchQty) { this.matchQty = matchQty; }

    public BigDecimal getMatchPrice() { return matchPrice; }
    public void setMatchPrice(BigDecimal matchPrice) { this.matchPrice = matchPrice; }
}
