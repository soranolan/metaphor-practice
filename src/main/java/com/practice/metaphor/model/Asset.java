package com.practice.metaphor.model;

import java.time.LocalDateTime;

/**
 * 資產模型
 *
 * @param id        唯一 ID
 * @param symbol    標的代碼 (如 USD, TSM)
 * @param name      資產全稱
 * @param precision 小數點精準度
 * @param createdAt 建立時間
 */
public record Asset(
    Long id,
    String symbol,
    String name,
    Integer precision,
    LocalDateTime createdAt
) {
}
