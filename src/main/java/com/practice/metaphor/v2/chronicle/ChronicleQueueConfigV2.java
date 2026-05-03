package com.practice.metaphor.v2.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chronicle Queue 設定
 *
 * <p>建立 {@link ChronicleQueue} Bean，作為 WAL（Write-Ahead Log）的儲存後端。
 * Chronicle Queue 內部使用 memory-mapped file，提供接近記憶體的寫入速度。
 *
 * <p>資料目錄預設為 {@code ./chronicle-data/}（可透過 {@code v2.chronicle.path} 設定）。
 * 每日自動 Rotation，產生 {@code YYYYMMDD.cq4} 格式的 binary 檔案。
 *
 * <p>偵錯指令：
 * <pre>
 *   java -cp chronicle-queue-*.jar \
 *        net.openhft.chronicle.queue.ChronicleReaderMain \
 *        --path ./chronicle-data/
 * </pre>
 */
@Configuration
public class ChronicleQueueConfigV2 {

    @Value("${v2.chronicle.path}")
    private String chroniclePath;

    @Bean(destroyMethod = "close")
    public ChronicleQueue chronicleQueue() {
        return SingleChronicleQueueBuilder
                .binary(chroniclePath)
                .rollCycle(RollCycles.FAST_DAILY)
                .build();
    }
}
