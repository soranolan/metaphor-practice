package com.practice.metaphor.v1.service;

import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.mapper.OrderMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import com.practice.metaphor.v1.model.entity.OrderV1;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 簡易撮合引擎服務 (資料庫驅動版) - [Getter 修復版]
 */
@Service
public class MatchingServiceV1 {

    private final OrderMapperV1 orderMapper;
    private final BalanceMapperV1 balanceMapper;

    public MatchingServiceV1(OrderMapperV1 orderMapper, BalanceMapperV1 balanceMapper) {
        this.orderMapper = orderMapper;
        this.balanceMapper = balanceMapper;
    }

    @Transactional
    public void match(OrderV1 takerOrder) {
        int makerSide = (takerOrder.getSide() == 0) ? 1 : 0;
        List<OrderV1> makers = orderMapper.findMakers(
            takerOrder.getBaseAssetId(),
            takerOrder.getQuoteAssetId(),
            makerSide,
            takerOrder.getPrice()
        );

        BigDecimal remainingQty = takerOrder.getTotalQty().subtract(takerOrder.getFilledQty());

        for (OrderV1 maker : makers) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

            if (maker.getTraderId().equals(takerOrder.getTraderId())) {
                continue; // 避免自成交 (Wash Trading)，直接跳過
            }

            BigDecimal makerRemaining = maker.getTotalQty().subtract(maker.getFilledQty());
            BigDecimal matchQty = remainingQty.min(makerRemaining);

            executeClearing(takerOrder, maker, matchQty, maker.getPrice());

            BigDecimal newMakerFilled = maker.getFilledQty().add(matchQty);
            int newMakerStatus = (newMakerFilled.compareTo(maker.getTotalQty()) == 0) ? 2 : 1;
            orderMapper.updateOrderProgress(maker.getId(), newMakerFilled, newMakerStatus);

            remainingQty = remainingQty.subtract(matchQty);
        }

        BigDecimal totalFilled = takerOrder.getTotalQty().subtract(remainingQty);
        int takerStatus = (remainingQty.compareTo(BigDecimal.ZERO) == 0) ? 2 : 1;
        orderMapper.updateOrderProgress(takerOrder.getId(), totalFilled, takerStatus);

        // 完美的逐筆溢價退款：每個交易發生時就結算不需要追蹤全局
        // 刪除 refundExcessFrozen
        // 沒用到的方法已在此被移除
    }

    private void executeClearing(OrderV1 taker, OrderV1 maker, BigDecimal matchQty, BigDecimal matchPrice) {
        BigDecimal matchAmount = matchQty.multiply(matchPrice);
        OrderV1 buyer = (taker.getSide() == 0) ? taker : maker;
        OrderV1 seller = (taker.getSide() == 1) ? taker : maker;

        if (buyer.getTraderId() < seller.getTraderId()) {
            applyBuyerEffect(buyer, matchQty, matchAmount);
            applySellerEffect(seller, matchQty, matchAmount);
        } else {
            applySellerEffect(seller, matchQty, matchAmount);
            applyBuyerEffect(buyer, matchQty, matchAmount);
        }
    }

    private void applyBuyerEffect(OrderV1 buyer, BigDecimal matchQty, BigDecimal matchAmount) {
        // 1. 算出這筆成交的量，當初被凍結了多少錢？
        BigDecimal expectedLocked = matchQty.multiply(buyer.getPrice());
        
        // 2. 當初凍結的錢 - 實際花掉的錢 = 省下來的溢價退款
        BigDecimal refundAmount = expectedLocked.subtract(matchAmount);

        // 3. 買方可用餘額：+加上退款，買方凍結金額：-扣除當初這批數量的凍結金
        // 這樣買方就不會多扣錢，同時精確抵銷對應的凍結。
        updateBalanceAtomic(buyer.getTraderId(), buyer.getQuoteAssetId(), refundAmount, expectedLocked.negate());
        updateBalanceAtomic(buyer.getTraderId(), buyer.getBaseAssetId(), matchQty, BigDecimal.ZERO);
    }

    private void applySellerEffect(OrderV1 seller, BigDecimal matchQty, BigDecimal matchAmount) {
        updateBalanceAtomic(seller.getTraderId(), seller.getBaseAssetId(), BigDecimal.ZERO, matchQty.negate());
        updateBalanceAtomic(seller.getTraderId(), seller.getQuoteAssetId(), matchAmount, BigDecimal.ZERO);
    }

    private void updateBalanceAtomic(Long traderId, Long assetId, BigDecimal availableDelta, BigDecimal frozenDelta) {
        BalanceV1 balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetId);
        if (balance == null) {
            throw new RuntimeException("系統錯誤：交易員 " + traderId + " 缺少資產錢包 " + assetId);
        }
        balanceMapper.updateBalance(traderId, assetId, 
            balance.availableAmount().add(availableDelta), 
            balance.frozenAmount().add(frozenDelta)
        );
    }
}
