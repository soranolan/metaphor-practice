package com.practice.metaphor.v2.mapper;

import com.practice.metaphor.v2.model.entity.OrderV2;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * V2 委託單 Mapper (只保留 DB 寫入，不再提供查詢與加鎖方法)
 */
@Mapper
public interface OrderMapperV2 {

    /**
     * 建立一筆新委託單（回填 generated ID）。
     */
    int insert(OrderV2 order);

    /**
     * 冪等更新委託單進度（由 ProjectionHandler 非同步執行）。
     *
     * @param id        委託單 ID
     * @param filledQty 累積成交量
     * @param status    最新狀態
     */
    void updateOrderProgress(
            @Param("id") Long id,
            @Param("filledQty") BigDecimal filledQty,
            @Param("status") int status
    );
}
