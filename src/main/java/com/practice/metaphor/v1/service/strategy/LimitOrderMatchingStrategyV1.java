package com.practice.metaphor.v1.service.strategy;

import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.mapper.OrderMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import com.practice.metaphor.v1.model.entity.OrderV1;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LimitOrderMatchingStrategyV1 implements MatchingStrategyV1 {

    private final OrderMapperV1 orderMapper;
    private final BalanceMapperV1 balanceMapper;

    public LimitOrderMatchingStrategyV1(OrderMapperV1 orderMapper, BalanceMapperV1 balanceMapper) {
        this.orderMapper = orderMapper;
        this.balanceMapper = balanceMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void executeMatch(OrderV1 takerOrder) {
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
        BigDecimal expectedLocked = matchQty.multiply(buyer.getPrice());
        BigDecimal refundAmount = expectedLocked.subtract(matchAmount);

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
