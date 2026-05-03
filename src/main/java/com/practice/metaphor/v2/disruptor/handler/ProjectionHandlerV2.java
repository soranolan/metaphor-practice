package com.practice.metaphor.v2.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.practice.metaphor.v2.disruptor.event.TradeResultEventV2;
import com.practice.metaphor.v2.mapper.BalanceMapperV2;
import com.practice.metaphor.v2.mapper.OrderMapperV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

/**
 * DB 投影 Handler（非同步，單執行緒）
 *
 * <p>消費 Output RingBuffer 的 {@link TradeResultEventV2}，
 * 將撮合結果非同步寫入資料庫（Read Model）：
 * <ul>
 *   <li>更新 orders 表的 filled_qty / status</li>
 *   <li>更新 balances 表的 available_amount / frozen_amount</li>
 * </ul>
 *
 * <p>DB 更新具備冪等性：使用絕對值更新（SET filled_qty = ?），而非增量更新，
 * 重啟後 Replay 重複寫入不會造成資料錯誤。
 */
public class ProjectionHandlerV2 implements EventHandler<TradeResultEventV2> {

    private static final Logger log = LoggerFactory.getLogger(ProjectionHandlerV2.class);

    private final OrderMapperV2 orderMapper;
    private final BalanceMapperV2 balanceMapper;
    private final TransactionTemplate txTemplate;

    public ProjectionHandlerV2(OrderMapperV2 orderMapper,
                               BalanceMapperV2 balanceMapper,
                               PlatformTransactionManager txManager) {
        this.orderMapper = orderMapper;
        this.balanceMapper = balanceMapper;
        this.txTemplate = new TransactionTemplate(txManager);
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void onEvent(TradeResultEventV2 event, long sequence, boolean endOfBatch) {
        txTemplate.executeWithoutResult(status -> {
            try {
                /* 1. 更新 taker 訂單狀態 */
                orderMapper.updateOrderProgress(
                        event.getTakerOrderId(),
                        event.getTakerTotalFilledQty(),
                        event.getTakerNewStatus()
                );

                /* 2. 更新 maker 訂單狀態 */
                orderMapper.updateOrderProgress(
                        event.getMakerOrderId(),
                        event.getMakerTotalFilledQty(),
                        event.getMakerNewStatus()
                );

                /* 3. 結算買賣雙方 DB 餘額 */
                long buyTraderId  = (event.getTakerSide() == 0) ? event.getTakerTraderId() : event.getMakerTraderId();
                long sellTraderId = (event.getTakerSide() == 1) ? event.getTakerTraderId() : event.getMakerTraderId();
                long baseAssetId  = event.getBaseAssetId();
                long quoteAssetId = event.getQuoteAssetId();
                BigDecimal matchAmount = event.getMatchQty().multiply(event.getMatchPrice());

                /* 買方：解凍 quote，增加 base */
                balanceMapper.addAvailableAndDeductFrozen(buyTraderId, quoteAssetId, BigDecimal.ZERO, matchAmount);
                balanceMapper.addAvailableAndDeductFrozen(buyTraderId, baseAssetId, event.getMatchQty(), BigDecimal.ZERO);

                /* 賣方：解凍 base，增加 quote */
                balanceMapper.addAvailableAndDeductFrozen(sellTraderId, baseAssetId, BigDecimal.ZERO, event.getMatchQty());
                balanceMapper.addAvailableAndDeductFrozen(sellTraderId, quoteAssetId, matchAmount, BigDecimal.ZERO);

            } catch (Exception e) {
                log.error("Projection 失敗，sequence={}", sequence, e);
                status.setRollbackOnly();
            }
        });
    }
}
