package com.practice.metaphor.v2.disruptor.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventFactoryV2;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import com.practice.metaphor.v2.disruptor.event.TradeResultEventFactoryV2;
import com.practice.metaphor.v2.disruptor.event.TradeResultEventV2;
import com.practice.metaphor.v2.disruptor.handler.JournalWriterHandlerV2;
import com.practice.metaphor.v2.disruptor.handler.MatchingEngineHandlerV2;
import com.practice.metaphor.v2.disruptor.handler.OrderPersistHandlerV2;
import com.practice.metaphor.v2.disruptor.handler.ProjectionHandlerV2;
import com.practice.metaphor.v2.engine.AccountBookV2;
import com.practice.metaphor.v2.snapshot.SnapshotServiceV2;
import com.practice.metaphor.v2.mapper.BalanceMapperV2;
import com.practice.metaphor.v2.mapper.OrderMapperV2;
import jakarta.annotation.PreDestroy;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.Executors;

/**
 * Disruptor Pipeline 組裝設定
 *
 * <p>組裝兩條 Disruptor 管線：
 * <ol>
 *   <li><b>Input Pipeline</b>：HTTP → InputRingBuffer → JournalWriter → MatchingEngine</li>
 *   <li><b>Output Pipeline</b>：MatchingEngine → OutputRingBuffer → ProjectionHandler</li>
 * </ol>
 *
 * <p>Barrier 設計：{@link MatchingEngineHandlerV2} 與 {@link OrderPersistHandlerV2} 依賴 {@link JournalWriterHandlerV2}，
 * Disruptor 保證 JournalWriter 完成後才喚醒後續 Handler。
 */
@Configuration
public class DisruptorConfigV2 {

    private static final int INPUT_BUFFER_SIZE  = 1024;  // 必須為 2 的冪次
    private static final int OUTPUT_BUFFER_SIZE = 4096;  // 輸出事件可能多於輸入（一對多 match）

    private Disruptor<OrderCommandEventV2> inputDisruptor;
    private Disruptor<TradeResultEventV2>  outputDisruptor;

    @Bean
    public RingBuffer<TradeResultEventV2> outputRingBuffer(
            OrderMapperV2 orderMapper,
            BalanceMapperV2 balanceMapper,
            PlatformTransactionManager txManager) {

        /* --- Output Disruptor（ProjectionHandler） --- */
        outputDisruptor = new Disruptor<>(
                new TradeResultEventFactoryV2(),
                OUTPUT_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,          /* 僅 MatchingEngine 單執行緒發布 */
                new BlockingWaitStrategy()
        );

        outputDisruptor.handleEventsWith(
                new ProjectionHandlerV2(orderMapper, balanceMapper, txManager)
        );

        outputDisruptor.start();
        return outputDisruptor.getRingBuffer();
    }

    @Bean
    public RingBuffer<OrderCommandEventV2> inputRingBuffer(
            ChronicleQueue chronicleQueue,
            OrderMapperV2 orderMapper,
            MatchingEngineHandlerV2 matchingEngine) {

        /* --- Input Disruptor（JournalWriter → MatchingEngine & OrderPersist） --- */
        inputDisruptor = new Disruptor<>(
                new OrderCommandEventFactoryV2(),
                INPUT_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,           /* HTTP 多執行緒發布 */
                new BlockingWaitStrategy()
        );

        JournalWriterHandlerV2 journalWriter = new JournalWriterHandlerV2(chronicleQueue);
        OrderPersistHandlerV2 orderPersist = new OrderPersistHandlerV2(orderMapper);

        /* Pipeline：JournalWriter 先，MatchingEngine 與 OrderPersist 平行處理（依賴 Barrier） */
        inputDisruptor.handleEventsWith(journalWriter)
                      .then(matchingEngine, orderPersist);

        inputDisruptor.start();
        return inputDisruptor.getRingBuffer();
    }

    /** 提供 MatchingEngineHandlerV2 的引用，供 RecoveryServiceV2 使用。 */
    @Bean
    public MatchingEngineHandlerV2 matchingEngineHandler(
            AccountBookV2 accountBook,
            RingBuffer<TradeResultEventV2> outputRingBuffer,
            SnapshotServiceV2 snapshotService) {
        return new MatchingEngineHandlerV2(accountBook, outputRingBuffer, snapshotService);
    }

    @PreDestroy
    public void shutdown() {
        if (inputDisruptor != null) inputDisruptor.shutdown();
        if (outputDisruptor != null) outputDisruptor.shutdown();
    }
}
