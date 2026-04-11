package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.api.TradingApi;
import com.practice.metaphor.v1.dto.ApiResponse;
import com.practice.metaphor.v1.dto.OrderRequest;
import com.practice.metaphor.v1.service.TradingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 */
@RestController
@RequestMapping("/api/orders")
public class TradingController implements TradingApi {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @Override
    @PostMapping
    public ApiResponse<String> placeOrder(@RequestBody OrderRequest request) {
        // 轉交業務邏輯層
        tradingService.placeOrder(
                request.traderId(),
                request.marketId(),
                request.side(),
                request.price(),
                request.totalQty()
        );
        // 使用包裝器封裝成功訊息
        return ApiResponse.success("委託下單成功 (已進行資產凍結)");
    }
}
