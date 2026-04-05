package com.practice.metaphor.service;

import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.mapper.OrderMapper;
import com.practice.metaphor.model.entity.Balance;
import com.practice.metaphor.model.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 簡易撮合引擎服務 (資料庫驅動版) - [Getter 修復版]
 */
@Service
public class MatchingService {

    private final OrderMapper orderMapper;
    private final BalanceMapper balanceMapper;

    public MatchingService(OrderMapper orderMapper, BalanceMapper balanceMapper) {
        this.orderMapper = orderMapper;
        this.balanceMapper = balanceMapper;
    }

    @Transactional
    public void match(Order takerOrder) {
        int makerSide = (takerOrder.getSide() == 0) ? 1 : 0;
        List<Order> makers = orderMapper.findMakers(
            takerOrder.getBaseAssetId(),
            takerOrder.getQuoteAssetId(),
            makerSide,
            takerOrder.getPrice()
        );

        BigDecimal remainingQty = takerOrder.getTotalQty().subtract(takerOrder.getFilledQty());

        for (Order maker : makers) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

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

        // 【價格改進返還】如果買單完全成交，將多鎖定的「溢價金」解凍還給使用者
        if (takerStatus == 2 && takerOrder.getSide() == 0) {
            refundExcessFrozen(takerOrder);
        }
    }

    private void refundExcessFrozen(Order order) {
        Balance bal = balanceMapper.findByTraderIdAndAssetIdForUpdate(order.getTraderId(), order.getQuoteAssetId());
        if (bal != null && bal.frozenAmount().compareTo(BigDecimal.ZERO) > 0) {
            // 在極簡版中，我們假設一個資產同時只有一個掛單，直接將該資產的所有剩餘凍結歸零並轉回可用
            // 專業版應精確追蹤「此訂單」鎖定的金額。
            balanceMapper.updateBalance(order.getTraderId(), order.getQuoteAssetId(), 
                bal.availableAmount().add(bal.frozenAmount()), BigDecimal.ZERO);
        }
    }

    private void executeClearing(Order taker, Order maker, BigDecimal matchQty, BigDecimal matchPrice) {
        BigDecimal matchAmount = matchQty.multiply(matchPrice);
        Order buyer = (taker.getSide() == 0) ? taker : maker;
        Order seller = (taker.getSide() == 1) ? taker : maker;

        if (buyer.getTraderId() < seller.getTraderId()) {
            applyBuyerEffect(buyer, matchQty, matchAmount);
            applySellerEffect(seller, matchQty, matchAmount);
        } else {
            applySellerEffect(seller, matchQty, matchAmount);
            applyBuyerEffect(buyer, matchQty, matchAmount);
        }
    }

    private void applyBuyerEffect(Order buyer, BigDecimal matchQty, BigDecimal matchAmount) {
        updateBalanceAtomic(buyer.getTraderId(), buyer.getQuoteAssetId(), BigDecimal.ZERO, matchAmount.negate());
        updateBalanceAtomic(buyer.getTraderId(), buyer.getBaseAssetId(), matchQty, BigDecimal.ZERO);
    }

    private void applySellerEffect(Order seller, BigDecimal matchQty, BigDecimal matchAmount) {
        updateBalanceAtomic(seller.getTraderId(), seller.getBaseAssetId(), BigDecimal.ZERO, matchQty.negate());
        updateBalanceAtomic(seller.getTraderId(), seller.getQuoteAssetId(), matchAmount, BigDecimal.ZERO);
    }

    private void updateBalanceAtomic(Long traderId, Long assetId, BigDecimal availableDelta, BigDecimal frozenDelta) {
        Balance balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetId);
        if (balance == null) {
            throw new RuntimeException("系統錯誤：交易員 " + traderId + " 缺少資產錢包 " + assetId);
        }
        balanceMapper.updateBalance(traderId, assetId, 
            balance.availableAmount().add(availableDelta), 
            balance.frozenAmount().add(frozenDelta)
        );
    }
}
