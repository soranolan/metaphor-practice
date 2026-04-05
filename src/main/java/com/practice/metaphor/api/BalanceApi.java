package com.practice.metaphor.api;

import com.practice.metaphor.dto.ApiResponse;
import com.practice.metaphor.model.entity.Balance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 餘額 API 規範
 */
@Tag(name = "餘額管理", description = "查詢交易員的資產持倉與餘額")
public interface BalanceApi {

    @Operation(summary = "查詢交易員的所有餘額", description = "傳入交易員 ID，獲取其名下所有的資產餘額清單 (包含可用與凍結金額)")
    @GetMapping("/{traderId}")
    ApiResponse<List<Balance>> getBalances(
        @Parameter(description = "交易員 ID", example = "1")
        @PathVariable("traderId") Long traderId
    );
}
