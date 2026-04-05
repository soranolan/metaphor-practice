package com.practice.metaphor.mapper;

import com.practice.metaphor.model.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 委託單映射介面
 */
@Mapper
public interface OrderMapper {

    /**
     * 建立一筆新委託單
     *
     * @param order 委託單物件
     * @return 影響行數
     */
    int insert(Order order);

    /**
     * 根據 ID 查詢委託單
     * 
     * @param id 委託單 ID
     * @return 委託單物件
     */
    Order findById(@Param("id") Long id);
}
