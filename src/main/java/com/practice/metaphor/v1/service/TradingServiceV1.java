package com.practice.metaphor.v1.service;

import com.practice.metaphor.v1.exception.BusinessExceptionV1;
import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.mapper.MarketMapperV1;
import com.practice.metaphor.v1.mapper.OrderMapperV1;
import com.practice.metaphor.v1.model.entity.MarketV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import com.practice.metaphor.v1.model.entity.OrderV1;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易服務核心
 */
@Service
public class TradingServiceV1 {

    private final BalanceMapperV1 balanceMapper;
    private final OrderMapperV1 orderMapper;
    private final MarketMapperV1 marketMapper;
    private final MatchingServiceV1 matchingService; // 新增：撮合引擎

    public TradingServiceV1(BalanceMapperV1 balanceMapper, OrderMapperV1 orderMapper, MarketMapperV1 marketMapper, MatchingServiceV1 matchingService) {
        this.balanceMapper = balanceMapper;
        this.orderMapper = orderMapper;
        this.marketMapper = marketMapper;
        this.matchingService = matchingService;
    }

    /**
     * 點擊委託下單 (基於市場 ID)
     */
    @Transactional
    public void placeOrder(Long traderId, Long marketId, Integer type, int side, BigDecimal price, BigDecimal qty) {
        
        // 1. 查詢市場規則
        MarketV1 market = marketMapper.findById(marketId)
                .orElseThrow(() -> new BusinessExceptionV1("【交易失敗】市場不存在"));

        Long baseAssetId = market.baseAssetId();
        Long quoteAssetId = market.quoteAssetId();

        // 2. 決定要鎖定哪種資產
        Long assetToLock = (side == 0) ? quoteAssetId : baseAssetId;
        BigDecimal amountToLock = (side == 0) ? price.multiply(qty) : qty;

        // 3. 獲取餘額並加鎖
        BalanceV1 balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetToLock);
        
        if (balance == null || balance.availableAmount().compareTo(amountToLock) < 0) {
            throw new BusinessExceptionV1("【交易失敗】餘額不足");
        }

        // 4. 更新可用與凍結金額 (資產凍結)
        BigDecimal newAvailable = balance.availableAmount().subtract(amountToLock);
        BigDecimal newFrozen = balance.frozenAmount().add(amountToLock);
        balanceMapper.updateBalance(traderId, assetToLock, newAvailable, newFrozen);

        // 5. 建立委託單紀錄
        OrderV1 order = new OrderV1(
                null, 
                traderId,
                baseAssetId,
                quoteAssetId,
                type != null ? type : 0,
                side,
                price,
                qty,
                BigDecimal.ZERO, 
                0, // 狀態: 0 (NEW)
                LocalDateTime.now()
        );
        orderMapper.insert(order);

        // 6. 重要：下單完成後「立刻主動觸發撮合」
        // 讓新單 (Taker) 到舊單 (Makers) 中掃蕩。
        matchingService.match(order);
    }
}
