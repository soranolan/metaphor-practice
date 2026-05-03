package com.practice.metaphor.v2.engine;

import com.practice.metaphor.v2.exception.BusinessExceptionV2;
import com.practice.metaphor.v2.mapper.MarketMapperV2;
import com.practice.metaphor.v2.model.entity.MarketV2;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體市場規則簿 (V2)
 *
 * <p>專門用來在啟動時載入靜態市場規則配置，讓 TradingServiceV2 下單時能達到 Zero-DB 讀取。
 */
@Service
public class MarketBookV2 {

    private static final Logger log = LoggerFactory.getLogger(MarketBookV2.class);

    private final MarketMapperV2 marketMapper;

    // key = marketId
    private final Map<Long, MarketV2> markets = new ConcurrentHashMap<>();

    public MarketBookV2(MarketMapperV2 marketMapper) {
        this.marketMapper = marketMapper;
    }

    /**
     * 啟動時載入所有市場資料至記憶體
     */
    @PostConstruct
    public void init() {
        log.info("=== 初始化 MarketBookV2 開始 ===");
        List<MarketV2> allMarkets = marketMapper.findAll();
        for (MarketV2 market : allMarkets) {
            markets.put(market.id(), market);
        }
        log.info("MarketBookV2 載入 {} 筆市場規則完成", markets.size());
    }

    /**
     * 取得市場配置
     */
    public MarketV2 getMarket(Long marketId) {
        MarketV2 market = markets.get(marketId);
        if (market == null || market.status() == 0) {
            throw new BusinessExceptionV2("【交易失敗】市場不存在或未開放");
        }
        return market;
    }
}
