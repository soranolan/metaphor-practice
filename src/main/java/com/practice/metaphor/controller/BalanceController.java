package com.practice.metaphor.controller;

import com.practice.metaphor.api.BalanceApi;
import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.model.entity.Balance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 餘額控制器
 * 實作 BalanceApi 介面，保持程式碼整潔
 */
@RestController
@RequestMapping("/api/balances")
public class BalanceController implements BalanceApi {

    private final BalanceMapper balanceMapper;

    public BalanceController(BalanceMapper balanceMapper) {
        this.balanceMapper = balanceMapper;
    }

    @Override
    @GetMapping("/{traderId}")
    public List<Balance> getBalances(@PathVariable Long traderId) {
        // 現在改為調用 findByTraderId 來獲取該交易員的所有資產列表
        return balanceMapper.findByTraderId(traderId);
    }
}
