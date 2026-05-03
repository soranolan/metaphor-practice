package com.practice.metaphor.v2.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WAL 寫入 Handler（Journal Writer）
 *
 * <p>在 Disruptor Pipeline 中是第一個執行的 Handler。
 * 每筆 OrderCommandEvent 都會先透過 Chronicle Queue 的 {@link ExcerptAppender}
 * 追加寫入 memory-mapped binary 檔案（Write-Ahead Log），
 * 完成後 Disruptor Barrier 才會喚醒 {@link MatchingEngineHandlerV2}。
 *
 * <p>此設計確保：即使 JVM 在撮合過程中崩潰，Journal 中的事件仍然存在，
 * 可於重啟時由 RecoveryServiceV2 進行 Replay。
 */
public class JournalWriterHandlerV2 implements EventHandler<OrderCommandEventV2> {

    private static final Logger log = LoggerFactory.getLogger(JournalWriterHandlerV2.class);

    private final ExcerptAppender appender;

    public JournalWriterHandlerV2(ChronicleQueue queue) {
        this.appender = queue.createAppender();
    }

    @Override
    public void onEvent(OrderCommandEventV2 event, long sequence, boolean endOfBatch) {
        try (DocumentContext dc = appender.writingDocument()) {
            dc.wire().write("seq").int64(sequence)
              .write("commandType").int32(event.getCommandType())
              .write("orderId").int64(event.getOrderId())
              .write("traderId").int64(event.getTraderId())
              .write("baseAssetId").int64(event.getBaseAssetId())
              .write("quoteAssetId").int64(event.getQuoteAssetId())
              .write("side").int32(event.getSide())
              .write("price").text(event.getPrice() != null ? event.getPrice().toPlainString() : "")
              .write("totalQty").text(event.getTotalQty() != null ? event.getTotalQty().toPlainString() : "");
            
            // 寫入完成後，從 appender 獲取 Chronicle Queue 分配的真實 index，並存回 event
            event.setChronicleIndex(appender.lastIndexAppended());
        } catch (Exception e) {
            log.error("Chronicle Queue 寫入失敗，sequence={}", sequence, e);
            throw new RuntimeException("Chronicle WAL 寫入失敗", e);
        }
    }
}
