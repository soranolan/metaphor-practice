package com.practice.metaphor.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委託單模型 (Order Book 紀錄)
 *
 * @param id            唯一 ID
 * @param traderId      下單交易員 ID
 * @param baseAssetId   標的資產 ID (如 TSM)
 * @param quoteAssetId  計價資產 ID (如 USD)
 * @param side          方向：0: BUY, 1: SELL
 * @param price         委託價格
 * @param totalQty      委託總量
 * @param filledQty     已成交數量
 * @param status        狀態：0: NEW, 1: PARTIAL, 2: FILLED, 3: CANCELED
 * @param createdAt     下單時間
 */
public record Order(
    Long id,
    Long traderId,
    Long baseAssetId,
    Long quoteAssetId,
    Integer side,
    BigDecimal price,
    BigDecimal totalQty,
    BigDecimal filledQty,
    Integer status,
    LocalDateTime createdAt
) {
}
