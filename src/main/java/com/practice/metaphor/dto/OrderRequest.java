package com.practice.metaphor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * 下單請求資料傳輸物件
 */
@Schema(description = "下單請求資訊")
public record OrderRequest(
        @Schema(description = "交易員 ID", example = "1001") Long traderId,

        @Schema(description = "基礎資產 ID (例如 VT 的 ID)", example = "1") Long baseAssetId,

        @Schema(description = "計價資產 ID (例如 USDT 的 ID)", example = "2") Long quoteAssetId,

        @Schema(description = "交易方向 (1: 買入, 2: 賣出)", example = "1") int side,

        @Schema(description = "委託價格", example = "50000.00") BigDecimal price,

        @Schema(description = "委託數量", example = "0.5") BigDecimal totalQty) {
}
