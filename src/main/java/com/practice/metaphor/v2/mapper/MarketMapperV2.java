package com.practice.metaphor.v2.mapper;

import com.practice.metaphor.v2.model.entity.MarketV2;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * V2 市場 Mapper
 */
@Mapper
public interface MarketMapperV2 {
    /**
     * 取得所有市場（供 MarketBook 啟動時快取用）
     */
    List<MarketV2> findAll();
}
