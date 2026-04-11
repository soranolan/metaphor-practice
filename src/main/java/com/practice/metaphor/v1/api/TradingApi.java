package com.practice.metaphor.v1.api;

import com.practice.metaphor.v1.dto.ApiResponse;
import com.practice.metaphor.v1.dto.OrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 交易 API 規範介面
 */
@Tag(name = "交易管理", description = "處理訂單委託、撮合與交易相關操作")
public interface TradingApi {

    @Operation(
        summary = "提交新委託單",
        description = "提交一個新的買入或賣出委託單。系統會驗證餘額並凍結相應資產，隨後將訂單寫入資料庫與委託簿。"
    )
    // 這裡直接使用全路徑來指定 Swagger 的註解，避開名稱衝突
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "委託單已受理，資金已凍結"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "無效的請求參數或業務邏輯錯誤（如餘額不足）")
    })
    ApiResponse<String> placeOrder(@RequestBody OrderRequest request);
}
