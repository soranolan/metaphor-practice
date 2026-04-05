package com.practice.metaphor.mapper;

import com.practice.metaphor.model.entity.Market;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 交易市場數據訪問層 (MyBatis)
 */
@Mapper
public interface MarketMapper {
    /**
     * 根據 ID 查詢市場規則
     */
    Optional<Market> findById(@Param("id") Long id);
}
