package com.practice.metaphor.v1.api;

import com.practice.metaphor.v1.dto.ApiResponseV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
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
public interface BalanceApiV1 {

    @Operation(summary = "查詢交易員的所有餘額", description = "傳入交易員 ID，獲取其名下所有的資產餘額清單 (包含可用與凍結金額)")
    @GetMapping("/{traderId}")
    ApiResponseV1<List<BalanceV1>> getBalances(
        @Parameter(description = "交易員 ID", example = "1")
        @PathVariable("traderId") Long traderId
    );
}
