package com.practice.metaphor.v1.dto;

import com.practice.metaphor.v1.model.entity.BalanceV1;
import java.util.List;

/**
 * 交易員餘額分組 DTO
 * 
 * @param traderId 交易員 ID
 * @param balances 該交易員的所有資產餘額列表
 */
public record TraderBalancesV1(
    Long traderId,
    List<BalanceV1> balances
) {}
