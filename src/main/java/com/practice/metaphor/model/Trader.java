package com.practice.metaphor.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 交易員模型
 *
 * @param id        唯一 ID
 * @param name      名稱
 * @param createdAt 建立時間
 */
@Table("traders")
public record Trader(
    @Id Long id,
    String name,
    LocalDateTime createdAt
) {
}
