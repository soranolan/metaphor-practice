package com.practice.metaphor.v1.mapper;

import com.practice.metaphor.v1.model.entity.MarketV1;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 交易市場數據訪問層 (MyBatis)
 */
@Mapper
public interface MarketMapperV1 {
    /**
     * 根據 ID 查詢市場規則
     */
    Optional<MarketV1> findById(@Param("id") Long id);
}
