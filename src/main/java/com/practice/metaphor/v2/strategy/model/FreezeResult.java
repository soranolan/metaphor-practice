package com.practice.metaphor.v2.strategy.model;

import java.math.BigDecimal;

/**
 * 凍結計算結果
 */
public record FreezeResult(
    long assetId,
    BigDecimal amount
) {}
