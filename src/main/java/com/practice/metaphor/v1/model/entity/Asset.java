package com.practice.metaphor.v1.model.entity;

import java.time.LocalDateTime;

/**
 * 資產模型
 *
 * @param id        唯一 ID
 * @param symbol    資產代碼 (如 VT, USD)
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
