package com.practice.metaphor.v2.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V2 餘額實體（供 DB 查詢與快照還原使用）
 */
public record BalanceV2(
    Long id,
    Long traderId,
    Long assetId,
    BigDecimal availableAmount,
    BigDecimal frozenAmount,
    LocalDateTime updatedAt
) {}
