package com.practice.metaphor.v1.mapper;

import com.practice.metaphor.v1.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 委託單映射介面 (MyBatis)
 */
@Mapper
public interface OrderMapper {

    /**
     * 建立一筆新委託單
     */
    int insert(Order order);

    /**
     * 查詢特定的委託單 (含加鎖)
     */
    Order findByIdForUpdate(@Param("id") Long id);

    /**
     * 尋找可成交的對手單 (Makers)
     * 
     * @param baseAssetId  標的資產
     * @param quoteAssetId 計價資產
     * @param side         對手方向 (原本下買，就找賣)
     * @param price        價格門檻 (買單 = price越高越好, 賣單 = price越低越好)
     * @return 合適的掛單列表 (按價格優先、時間優先排序)
     */
    List<Order> findMakers(
        @Param("baseAssetId") Long baseAssetId,
        @Param("quoteAssetId") Long quoteAssetId,
        @Param("side") int side,
        @Param("price") BigDecimal price
    );

    /**
     * 更新委託單成交進度
     */
    void updateOrderProgress(
        @Param("id") Long id,
        @Param("filledQty") BigDecimal filledQty,
        @Param("status") int status
    );
}
