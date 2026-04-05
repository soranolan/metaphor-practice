package com.practice.metaphor.controller;

import com.practice.metaphor.dto.OrderRequest;
import com.practice.metaphor.service.TradingService;
import org.springframework.web.bind.annotation.*;
import com.practice.metaphor.api.TradingApi;

/**
 * 交易控制器
 * 暴露委託下單介面
 */
@RestController
@RequestMapping("/api/orders")
public class TradingController implements TradingApi {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping
    @Override
    public String placeOrder(@RequestBody OrderRequest request) {
        tradingService.placeOrder(
                request.traderId(),
                request.baseAssetId(),
                request.quoteAssetId(),
                request.side(),
                request.price(),
                request.totalQty());
        return "【交易成功】委託單已受理，資金已凍結並寫入委託簿";
    }

}
