package com.practice.metaphor.controller;

import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.model.entity.Balance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 餘額查詢介面
 */
@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private final BalanceMapper balanceMapper;

    public BalanceController(BalanceMapper balanceMapper) {
        this.balanceMapper = balanceMapper;
    }

    /**
     * 獲取特定交易員對特定資產的持倉詳情
     */
    @GetMapping("/{traderId}/{assetId}")
    public Balance getBalance(@PathVariable Long traderId, @PathVariable Long assetId) {
        return balanceMapper.findByTraderIdAndAssetId(traderId, assetId)
                .orElse(null); // 如果找不到則返回 null (實際上可以丟 404)
    }
}
