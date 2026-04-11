package com.practice.metaphor.v1.model.entity;

import java.time.LocalDateTime;

/**
 * 交易員模型
 *
 * @param id        唯一 ID
 * @param name      名稱
 * @param createdAt 建立時間
 */
public record Trader(
    Long id,
    String name,
    LocalDateTime createdAt
) {
}
