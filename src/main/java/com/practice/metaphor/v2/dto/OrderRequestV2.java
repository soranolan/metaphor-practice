package com.practice.metaphor.v2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * V2 下單請求 DTO
 */
@Schema(description = "V2 下單請求")
public record OrderRequestV2(

    @Schema(description = "市場 ID", example = "1")
    Long marketId,

    @Schema(description = "訂單類型：0=限價，1=市價", example = "0")
    Integer type,

    @Schema(description = "方向：0=買入，1=賣出", example = "0")
    int side,

    @Schema(description = "限價價格（市價單傳 null）", example = "100.00")
    BigDecimal price,

    @Schema(description = "委託數量", example = "5.00")
    BigDecimal qty
) {}
