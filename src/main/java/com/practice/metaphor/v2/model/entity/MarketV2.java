package com.practice.metaphor.v2.model.entity;

/**
 * 交易市場實體 (V2)
 */
public record MarketV2(
        Long id,
        Long baseAssetId,
        Long quoteAssetId,
        String symbol,
        Integer status
) {}
