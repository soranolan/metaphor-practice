package com.practice.metaphor.v2.api;

import com.practice.metaphor.v2.dto.ApiResponseV2;
import com.practice.metaphor.v2.dto.OrderRequestV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * V2 交易 API 介面定義（Swagger 文件）
 */
@Tag(name = "V2 Trading API", description = "LMAX Disruptor + Chronicle Queue 記憶體撮合引擎")
@RequestMapping("/api/v2/trading")
public interface TradingApiV2 {

    @Operation(summary = "V2 下單", description = "記憶體凍結 + 非同步撮合，不阻塞 HTTP 執行緒")
    @PostMapping("/order")
    ApiResponseV2<String> placeOrder(
            @RequestAttribute("traderId") Long traderId,
            @RequestBody OrderRequestV2 request
    );
}
