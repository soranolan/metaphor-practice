package com.practice.metaphor.v2.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import com.practice.metaphor.v2.mapper.OrderMapperV2;
import com.practice.metaphor.v2.model.entity.OrderV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單非同步持久化 Handler (V2)
 *
 * <p>監聽 Input RingBuffer 的 {@link OrderCommandEventV2}，
 * 在背景將原始訂單寫入資料庫，使 API 達到 Zero-DB 熱路徑。
 */
public class OrderPersistHandlerV2 implements EventHandler<OrderCommandEventV2> {

    private static final Logger log = LoggerFactory.getLogger(OrderPersistHandlerV2.class);

    private final OrderMapperV2 orderMapper;

    public OrderPersistHandlerV2(OrderMapperV2 orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void onEvent(OrderCommandEventV2 event, long sequence, boolean endOfBatch) {
        // 只有下單類型的指令才需要持久化
        if (event.getCommandType() == 0 || event.getCommandType() == 1) {
            try {
                OrderV2 order = new OrderV2(
                        event.getOrderId(), // 使用 Snowflake 產生的 ID
                        event.getTraderId(),
                        event.getBaseAssetId(),
                        event.getQuoteAssetId(),
                        event.getCommandType(),
                        event.getSide(),
                        event.getPrice(),
                        event.getTotalQty(),
                        BigDecimal.ZERO, // 初始成交數量為 0
                        0,               // 初始狀態為 PENDING/NEW (0)
                        LocalDateTime.now()
                );

                orderMapper.insert(order);
                log.debug("訂單非同步持久化成功：orderId={}", event.getOrderId());
            } catch (Exception e) {
                log.error("訂單非同步持久化失敗：orderId={}", event.getOrderId(), e);
                // 注意：這裡失敗不會影響記憶體撮合與 WAL，但會導致 Read Model 少一筆資料。
                // 實務上可以考慮重試機制或寫入錯誤日誌以便後續修復。
            }
        }
    }
}
