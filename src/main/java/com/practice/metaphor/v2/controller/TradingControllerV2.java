package com.practice.metaphor.v2.controller;

import com.practice.metaphor.v2.dto.ApiResponseV2;
import com.practice.metaphor.v2.api.TradingApiV2;
import com.practice.metaphor.v2.dto.OrderRequestV2;
import com.practice.metaphor.v2.service.TradingServiceV2;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradingControllerV2 implements TradingApiV2 {

    private final TradingServiceV2 tradingService;

    public TradingControllerV2(TradingServiceV2 tradingService) {
        this.tradingService = tradingService;
    }

    @Override
    public ApiResponseV2<String> placeOrder(OrderRequestV2 request) {
        tradingService.placeOrder(
                request.traderId(),
                request.marketId(),
                request.type(),
                request.side(),
                request.price(),
                request.qty()
        );
        return ApiResponseV2.success("委託單已送出撮合（V2 記憶體引擎）");
    }
}
