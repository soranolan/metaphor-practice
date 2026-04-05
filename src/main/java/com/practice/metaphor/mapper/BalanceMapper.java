package com.practice.metaphor.mapper;

import com.practice.metaphor.model.entity.Balance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 持倉數據訪問層 (MyBatis)
 */
@Mapper
public interface BalanceMapper {
    /**
     * 查詢特定交易員的特定資產餘額
     */
    Optional<Balance> findByTraderIdAndAssetId(@Param("traderId") Long traderId, 
                                              @Param("assetId") Long assetId);
}
