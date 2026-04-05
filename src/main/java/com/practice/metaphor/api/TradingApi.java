package com.practice.metaphor.api;

import com.practice.metaphor.dto.OrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 交易 API 規範介面
 * 這裡集中管理所有的 Swagger/OpenAPI 註解，保持 Controller 程式碼整潔
 */
@Tag(name = "交易管理", description = "處理訂單委託、撮合與交易相關操作")
public interface TradingApi {

    @Operation(
        summary = "提交新委託單",
        description = "提交一個新的買入或賣出委託單。系統會驗證餘額並凍結相應資產，隨後將訂單寫入資料庫與委託簿。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "委託單已受理，資金已凍結"),
        @ApiResponse(responseCode = "400", description = "無效的請求參數"),
        @ApiResponse(responseCode = "403", description = "餘額不足，無法下單")
    })
    String placeOrder(@RequestBody OrderRequest request);
}
