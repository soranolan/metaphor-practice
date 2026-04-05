package com.practice.metaphor.api;

import com.practice.metaphor.model.entity.Balance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 餘額查詢 API 規範介面
 */
@Tag(name = "餘額管理", description = "查詢交易員的資產持倉與餘額狀況")
public interface BalanceApi {

    @Operation(
        summary = "獲取交易員所有資產持倉",
        description = "根據交易員 ID，獲取該交易員目前在系統中所有資產的可用餘額與凍結餘額列表。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功獲取持倉列表")
    })
    List<Balance> getBalances(
        @Parameter(description = "交易員 ID", example = "1") @PathVariable Long traderId
    );
}
