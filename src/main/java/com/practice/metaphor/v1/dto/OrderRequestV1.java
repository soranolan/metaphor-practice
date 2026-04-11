package com.practice.metaphor.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 下單請求資料傳輸物件
 * 修復資產保護結構：移除個別資產 ID，改用交易市場 marketId
 */
@Schema(description = "下單請求資訊 (基於市場 ID)")
public record OrderRequestV1(
        @Schema(description = "交易員 ID", example = "1") Long traderId,

        @Schema(description = "交易市場 ID (如 VT/USD 市場)", example = "101") Long marketId,

        @Schema(description = "交易方向 (0: 買入, 1: 賣出)", example = "0") int side,

        @Schema(description = "委託價格", example = "500.00") BigDecimal price,

        @Schema(description = "委託數量", example = "2.5") BigDecimal totalQty) {
}
