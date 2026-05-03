package com.practice.metaphor.v2.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V2 委託單實體（與 V1 完全隔離）
 * 使用 Class 而非 Record，以支援 MyBatis useGeneratedKeys 回填 ID。
 */
public class OrderV2 {

    private Long id;
    private Long traderId;
    private Long baseAssetId;
    private Long quoteAssetId;
    private int type;
    private int side;
    private BigDecimal price;
    private BigDecimal totalQty;
    private BigDecimal filledQty;
    private int status;
    private LocalDateTime createdAt;

    public OrderV2() {}

    public OrderV2(Long id, Long traderId, Long baseAssetId, Long quoteAssetId,
                   int type, int side, BigDecimal price, BigDecimal totalQty,
                   BigDecimal filledQty, int status, LocalDateTime createdAt) {
        this.id = id;
        this.traderId = traderId;
        this.baseAssetId = baseAssetId;
        this.quoteAssetId = quoteAssetId;
        this.type = type;
        this.side = side;
        this.price = price;
        this.totalQty = totalQty;
        this.filledQty = filledQty;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTraderId() { return traderId; }
    public void setTraderId(Long traderId) { this.traderId = traderId; }

    public Long getBaseAssetId() { return baseAssetId; }
    public void setBaseAssetId(Long baseAssetId) { this.baseAssetId = baseAssetId; }

    public Long getQuoteAssetId() { return quoteAssetId; }
    public void setQuoteAssetId(Long quoteAssetId) { this.quoteAssetId = quoteAssetId; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public int getSide() { return side; }
    public void setSide(int side) { this.side = side; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTotalQty() { return totalQty; }
    public void setTotalQty(BigDecimal totalQty) { this.totalQty = totalQty; }

    public BigDecimal getFilledQty() { return filledQty; }
    public void setFilledQty(BigDecimal filledQty) { this.filledQty = filledQty; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
