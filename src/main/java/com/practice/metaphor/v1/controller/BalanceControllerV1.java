package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.api.BalanceApiV1;
import com.practice.metaphor.v1.dto.ApiResponseV1;
import com.practice.metaphor.v1.dto.TraderBalancesV1;
import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public ApiResponseV1<List<TraderBalancesV1>> getAllBalances() {
        // 1. 從資料庫取得所有持倉
        List<BalanceV1> allBalances = balanceMapper.findAll();

        // 2. 依照交易員 ID 進行分組
        Map<Long, List<BalanceV1>> grouped = allBalances.stream()
                .collect(Collectors.groupingBy(BalanceV1::traderId));

        // 3. 轉換為 DTO 列表
        List<TraderBalancesV1> result = grouped.entrySet().stream()
                .map(entry -> new TraderBalancesV1(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return ApiResponseV1.success(result);
    }
}
