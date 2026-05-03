package com.practice.metaphor.v2.mapper;

import com.practice.metaphor.v2.model.entity.BalanceV2;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * V2 餘額 Mapper (DB 作為 Read Model)
 */
@Mapper
public interface BalanceMapperV2 {

    /**
     * 取得所有餘額（系統啟動時載入初始狀態用）
     */
    List<BalanceV2> findAll();

    /**
     * 更新餘額：增加 available 並扣除 frozen。
     * 用於 ProjectionHandlerV2 非同步結算。
     */
    void addAvailableAndDeductFrozen(
            @Param("traderId") Long traderId,
            @Param("assetId") Long assetId,
            @Param("addAvailable") BigDecimal addAvailable,
            @Param("deductFrozen") BigDecimal deductFrozen
    );
}
