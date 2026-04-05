package com.practice.metaphor.controller;

import com.practice.metaphor.api.TradingApi;
import com.practice.metaphor.dto.OrderRequest;
import com.practice.metaphor.service.TradingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 * 實作 TradingApi 介面，保持實作與定義分離
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
    public String placeOrder(@RequestBody OrderRequest request) {
        // 現在傳遞市場 ID，不再傳遞個別資產 ID
        tradingService.placeOrder(
                request.traderId(),
                request.marketId(),
                request.side(),
                request.price(),
                request.totalQty()
        );
        return "委託下單成功 (已進行資產凍結)";
    }
}
