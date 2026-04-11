package com.practice.metaphor.v1.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客戶持倉模型 (最終事實表)
 *
 * @param id              唯一 ID
 * @param traderId        所屬交易員 ID
 * @param assetId         所屬資產 ID
 * @param availableAmount 可用餘額
 * @param frozenAmount    凍結餘額 (掛單中)
 * @param updatedAt       更新時間
 */
public record Balance(
    Long id,
    Long traderId,
    Long assetId,
    BigDecimal availableAmount,
    BigDecimal frozenAmount,
    LocalDateTime updatedAt
) {
}
