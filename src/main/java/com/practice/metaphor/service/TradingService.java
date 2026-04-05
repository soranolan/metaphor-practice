package com.practice.metaphor.service;

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
 * 負責處理委託下單、資金凍結與原子性事務
 */
@Service
public class TradingService {

    private final BalanceMapper balanceMapper;
    private final OrderMapper orderMapper;
    private final MarketMapper marketMapper; // 新增：用於市場規則驗證

    public TradingService(BalanceMapper balanceMapper, OrderMapper orderMapper, MarketMapper marketMapper) {
        this.balanceMapper = balanceMapper;
        this.orderMapper = orderMapper;
        this.marketMapper = marketMapper;
    }

    /**
     * 點擊委託下單 (基於市場 ID)
     *
     * @param traderId     交易員 ID
     * @param marketId     交易市場 ID (如 VT/USD)
     * @param side         方向 (0: BUY, 1: SELL)
     * @param price        委託價格
     * @param qty          委託數量
     */
    @Transactional
    public void placeOrder(Long traderId, Long marketId, int side, BigDecimal price, BigDecimal qty) {
        
        // 1. 查詢市場規則，獲取資產 ID 對應
        Market market = marketMapper.findById(marketId)
                .orElseThrow(() -> new RuntimeException("【交易失敗】市場不存在，或市場已關閉"));

        Long baseAssetId = market.baseAssetId();
        Long quoteAssetId = market.quoteAssetId();

        // 2. 決定要鎖定哪種資產 (與原本邏輯一致，但資產 ID 現在由市場規則決定)
        // BUY: 支出計價資產 (USD), 數量 = 價格 * 委託量
        // SELL: 支出標的資產 (VT), 數量 = 委託量
        Long assetToLock = (side == 0) ? quoteAssetId : baseAssetId;
        BigDecimal amountToLock = (side == 0) ? price.multiply(qty) : qty;

        // 3. 獲取餘額並加鎖 (Row Lock)
        Balance balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetToLock);
        
        // 4. 檢查可用資金
        if (balance == null || balance.availableAmount().compareTo(amountToLock) < 0) {
            throw new RuntimeException("【交易失敗】餘額不足，無法進行資產凍結");
        }

        // 5. 更新餘額 (從可用轉移至凍結)
        BigDecimal newAvailable = balance.availableAmount().subtract(amountToLock);
        BigDecimal newFrozen = balance.frozenAmount().add(amountToLock);
        balanceMapper.updateBalance(traderId, assetToLock, newAvailable, newFrozen);

        // 6. 建立委託單紀錄
        Order order = new Order(
                null, // ID 由資料庫序列生成
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
    }
}
