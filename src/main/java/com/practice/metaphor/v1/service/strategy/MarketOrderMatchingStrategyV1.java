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
public class MarketOrderMatchingStrategyV1 implements MatchingStrategyV1 {

    private final OrderMapperV1 orderMapper;
    private final BalanceMapperV1 balanceMapper;

    public MarketOrderMatchingStrategyV1(OrderMapperV1 orderMapper, BalanceMapperV1 balanceMapper) {
        this.orderMapper = orderMapper;
        this.balanceMapper = balanceMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void executeMatch(OrderV1 takerOrder) {
        int makerSide = (takerOrder.getSide() == 0) ? 1 : 0;
        
        // 市價單：價格參數為 null 或不限制 (在 DB 中如果 price 可以為 null，則 Mapper 要處理)
        // 因為目前的 Mapper `findMakers` 是用 `<=` 或 `>=`，如果市價單傳入極端值，就能抓到全部
        // 我們給予買家無限高的價格去吃單，賣家以 0 的價格去拋售
        BigDecimal extremePrice = (takerOrder.getSide() == 0) ? new BigDecimal("999999999.00") : BigDecimal.ZERO;
        
        List<OrderV1> makers = orderMapper.findMakers(
            takerOrder.getBaseAssetId(),
            takerOrder.getQuoteAssetId(),
            makerSide,
            extremePrice
        );

        BigDecimal remainingQty = takerOrder.getTotalQty().subtract(takerOrder.getFilledQty());

        for (OrderV1 maker : makers) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

            if (maker.getTraderId().equals(takerOrder.getTraderId())) {
                continue; // 避免自成交
            }

            BigDecimal makerRemaining = maker.getTotalQty().subtract(maker.getFilledQty());
            BigDecimal matchQty = remainingQty.min(makerRemaining);
            
            // 市價單永遠以「Maker (掛單方)」的價格作為成交價
            BigDecimal matchPrice = maker.getPrice();

            executeClearing(takerOrder, maker, matchQty, matchPrice);

            BigDecimal newMakerFilled = maker.getFilledQty().add(matchQty);
            int newMakerStatus = (newMakerFilled.compareTo(maker.getTotalQty()) == 0) ? 2 : 1;
            orderMapper.updateOrderProgress(maker.getId(), newMakerFilled, newMakerStatus);

            remainingQty = remainingQty.subtract(matchQty);
        }

        BigDecimal totalFilled = takerOrder.getTotalQty().subtract(remainingQty);
        
        // 【市價單核心精神 IOC】
        // 如果 remainingQty > 0，代表把市場吃光了還是沒滿足。
        // 這時狀態直接變成 3 (已撤單)，並且要把這剩餘的量解凍！
        int takerStatus = (remainingQty.compareTo(BigDecimal.ZERO) == 0) ? 2 : 3;
        orderMapper.updateOrderProgress(takerOrder.getId(), totalFilled, takerStatus);

        // 如果被部分撤銷 (Cancel remainder)，就要退回剩餘數量一開始鎖定的資金
        if (takerStatus == 3) {
            cancelRemainder(takerOrder, remainingQty);
        }
    }

    private void cancelRemainder(OrderV1 takerOrder, BigDecimal remainingQty) {
        if (takerOrder.getSide() == 0) {
            // 買單的殘餘：退還原本為了這 remainingQty 鎖定的 Quote AssetV1 (預先假設用市價單下單時鎖定了特定的金額，但在 V1 實作上，此處需要依賴 TradingServiceV1 的鎖定邏輯)
            // 注意：由於市價單在下單時，我們目前 TradingServiceV1 還是用 `price * qty` 去鎖定。如果 price 是 null，TradingServiceV1 必須被改寫！
            // 由於前階段修改 `price` 可為 null，如果 `TradingServiceV1` 帶 null，這裡就需要特殊處理。
            // 這裡暫時依照下單時帶入的 `price` 來計算當初鎖定多少預扣款。
            if (takerOrder.getPrice() != null) {
                BigDecimal refundAmount = remainingQty.multiply(takerOrder.getPrice());
                updateBalanceAtomic(takerOrder.getTraderId(), takerOrder.getQuoteAssetId(), refundAmount, refundAmount.negate());
            }
        } else {
            // 賣單殘餘：退還 base asset
            updateBalanceAtomic(takerOrder.getTraderId(), takerOrder.getBaseAssetId(), remainingQty, remainingQty.negate());
        }
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
        // 如果市價單帶的 price 是 null，代表他用 Quote 按金額下市價單。但在這版中，我們假設市價單還是走 Base 量，並且可能用某種預設價格鎖定了資金。
        // 為簡化系統，我們假設市價單同樣在 price 帶有上限。
        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal expectedLocked = matchAmount;

        if (buyer.getPrice() != null) {
            expectedLocked = matchQty.multiply(buyer.getPrice());
            refundAmount = expectedLocked.subtract(matchAmount);
        }

        updateBalanceAtomic(buyer.getTraderId(), buyer.getQuoteAssetId(), refundAmount, expectedLocked.negate());
        updateBalanceAtomic(buyer.getTraderId(), buyer.getBaseAssetId(), matchQty, BigDecimal.ZERO);
    }

    private void applySellerEffect(OrderV1 seller, BigDecimal matchQty, BigDecimal matchAmount) {
        updateBalanceAtomic(seller.getTraderId(), seller.getBaseAssetId(), BigDecimal.ZERO, matchQty.negate());
        updateBalanceAtomic(seller.getTraderId(), seller.getQuoteAssetId(), matchAmount, BigDecimal.ZERO);
    }

    private void updateBalanceAtomic(Long traderId, Long assetId, BigDecimal availableDelta, BigDecimal frozenDelta) {
        BalanceV1 balance = balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, assetId);
        balanceMapper.updateBalance(traderId, assetId, 
            balance.availableAmount().add(availableDelta), 
            balance.frozenAmount().add(frozenDelta)
        );
    }
}
