package com.practice.metaphor.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委託單實體類別
 * 改回普通 Class 之理由：MyBatis 的 useGeneratedKeys 需要透過 setter 回填 ID，
 * 且訂單在撮合過程中會頻繁更新成交量與狀態，Class 比 Record 更靈活。
 */
public class Order {
    private Long id;
    private Long traderId;
    private Long baseAssetId;
    private Long quoteAssetId;
    private int side;
    private BigDecimal price;
    private BigDecimal totalQty;
    private BigDecimal filledQty;
    private int status;
    private LocalDateTime createdAt;

    // 給 MyBatis 使用的無參建構子
    public Order() {}

    public Order(Long id, Long traderId, Long baseAssetId, Long quoteAssetId, int side, 
                 BigDecimal price, BigDecimal totalQty, BigDecimal filledQty, 
                 int status, LocalDateTime createdAt) {
        this.id = id;
        this.traderId = traderId;
        this.baseAssetId = baseAssetId;
        this.quoteAssetId = quoteAssetId;
        this.side = side;
        this.price = price;
        this.totalQty = totalQty;
        this.filledQty = filledQty;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTraderId() { return traderId; }
    public void setTraderId(Long traderId) { this.traderId = traderId; }

    public Long getBaseAssetId() { return baseAssetId; }
    public void setBaseAssetId(Long baseAssetId) { this.baseAssetId = baseAssetId; }

    public Long getQuoteAssetId() { return quoteAssetId; }
    public void setQuoteAssetId(Long quoteAssetId) { this.quoteAssetId = quoteAssetId; }

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
