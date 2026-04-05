package com.practice.metaphor.model.entity;

/**
 * 交易市場模型 (Trading Pair)
 * 定義了哪些資產可以互相交易，以及誰是標的 (Base)、誰是計價物 (Quote)
 */
public record Market(
    Long id,
    Long baseAssetId,
    Long quoteAssetId,
    String symbol, // 如 "VT/USD"
    int status     // 0: 禁止交易, 1: 正常
) {
}
