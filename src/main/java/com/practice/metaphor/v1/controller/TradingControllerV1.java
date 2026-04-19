package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.api.TradingApiV1;
import com.practice.metaphor.v1.dto.ApiResponseV1;
import com.practice.metaphor.v1.dto.OrderRequestV1;
import com.practice.metaphor.v1.service.TradingServiceV1;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 */
@RestController
@RequestMapping("/api/v1/orders")
public class TradingControllerV1 implements TradingApiV1 {

    private final TradingServiceV1 tradingService;

    public TradingControllerV1(TradingServiceV1 tradingService) {
        this.tradingService = tradingService;
    }

    @Override
    @PostMapping
    public ApiResponseV1<String> placeOrder(@RequestBody OrderRequestV1 request) {
        // 轉交業務邏輯層
        tradingService.placeOrder(
                request.traderId(),
                request.marketId(),
                request.type() != null ? request.type() : 0, // Fallback to Limit
                request.side(),
                request.price(),
                request.totalQty()
        );
        // 使用包裝器封裝成功訊息
        return ApiResponseV1.success("委託下單成功 (已進行資產凍結)");
    }
}
