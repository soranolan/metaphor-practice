package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.api.BalanceApiV1;
import com.practice.metaphor.v1.dto.ApiResponseV1;
import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 餘額管理控制器
 */
@RestController
@RequestMapping("/api/v1/balances")
public class BalanceControllerV1 implements BalanceApiV1 {

    private final BalanceMapperV1 balanceMapper;

    public BalanceControllerV1(BalanceMapperV1 balanceMapper) {
        this.balanceMapper = balanceMapper;
    }

    @Override
    public ApiResponseV1<List<BalanceV1>> getBalances(Long traderId) {
        List<BalanceV1> balances = balanceMapper.findByTraderId(traderId);
        // 使用包裝器封裝成功回傳
        return ApiResponseV1.success(balances);
    }
}
