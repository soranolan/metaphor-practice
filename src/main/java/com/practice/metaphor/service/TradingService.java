package com.practice.metaphor.service;

import com.practice.metaphor.exception.BusinessException;
import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.mapper.MarketMapper;
import com.practice.metaphor.mapper.OrderMapper;
import com.practice.metaphor.model.entity.Market;
import com.practice.metaphor.model.entity.Balance;
import com.practice.metaphor.model.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易服務核心
 */
@Service
public class TradingService {

    private final BalanceMapper balanceMapper;
    private final OrderMapper orderMapper;
    private final MarketMapper marketMapper;
    private final MatchingService matchingService; // 新增：撮合引擎

    public TradingService(BalanceMapper balanceMapper, OrderMapper orderMapper, MarketMapper marketMapper, MatchingService matchingService) {
        this.balanceMapper = balanceMapper;
        this.orderMapper = orderMapper;
        this.marketMapper = marketMapper;
        this.matchingService = matchingService;
    }

    /**
     * 點擊委託下單 (基於市場 ID)
     */
    @Transactional
    public void placeOrder(Long traderId, Long marketId, int side, BigDecimal price, BigDecimal qty) {
        
        // 1. 查詢市場規則
        Market market = marketMapper.findById(marketId)
                .orElseThrow(() -> new BusinessException("【交易失敗】市場不存在"));

        Long baseAssetId = market.baseAssetId();
        Long quoteAssetId = market.quoteAssetId();

        // 2. 決定要鎖定哪種資產
        Long assetToLock = (side == 0) ? quoteAssetId : baseAssetId;
        BigDecimal amountToLock = (side == 0) ? price.multiply(qty) : qty;

        // 3. 獲取餘額並加鎖
        Balance balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetToLock);
        
        if (balance == null || balance.availableAmount().compareTo(amountToLock) < 0) {
            throw new BusinessException("【交易失敗】餘額不足");
        }

        // 4. 更新可用與凍結金額 (資產凍結)
        BigDecimal newAvailable = balance.availableAmount().subtract(amountToLock);
        BigDecimal newFrozen = balance.frozenAmount().add(amountToLock);
        balanceMapper.updateBalance(traderId, assetToLock, newAvailable, newFrozen);

        // 5. 建立委託單紀錄
        Order order = new Order(
                null, 
                traderId,
                baseAssetId,
                quoteAssetId,
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
