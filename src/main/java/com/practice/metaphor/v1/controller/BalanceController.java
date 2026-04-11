package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.api.BalanceApi;
import com.practice.metaphor.v1.dto.ApiResponse;
import com.practice.metaphor.v1.mapper.BalanceMapper;
import com.practice.metaphor.v1.model.entity.Balance;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 餘額管理控制器
 */
@RestController
@RequestMapping("/api/balances")
public class BalanceController implements BalanceApi {

    private final BalanceMapper balanceMapper;

    public BalanceController(BalanceMapper balanceMapper) {
        this.balanceMapper = balanceMapper;
    }

    @Override
    public ApiResponse<List<Balance>> getBalances(Long traderId) {
        List<Balance> balances = balanceMapper.findByTraderId(traderId);
        // 使用包裝器封裝成功回傳
        return ApiResponse.success(balances);
    }
}
