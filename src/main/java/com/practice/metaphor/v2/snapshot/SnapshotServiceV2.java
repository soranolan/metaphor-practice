package com.practice.metaphor.v2.snapshot;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.springframework.context.ApplicationContext;
import com.lmax.disruptor.RingBuffer;
import com.practice.metaphor.v2.disruptor.event.OrderCommandEventV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * 定時快照服務
 *
 * <p>
 * 將 OrderBookV2 + AccountBookV2 的當前記憶體狀態序列化為二進制 (.bin) 檔案，
 * 並記錄 Chronicle Queue 的當前 index 作為 Replay 起點。
 *
 * <p>
 * 快照路徑：{@code ./snapshots/snapshot-{timestamp}.bin}（可透過
 * {@code v2.snapshot.path} 設定）。
 */
@Service
public class SnapshotServiceV2 {

    private static final Logger log = LoggerFactory.getLogger(SnapshotServiceV2.class);
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ApplicationContext applicationContext;
    private RingBuffer<OrderCommandEventV2> inputRingBuffer;

    @Value("${v2.snapshot.path}")
    private String snapshotPath;

    public SnapshotServiceV2(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @SuppressWarnings("unchecked")
    private RingBuffer<OrderCommandEventV2> getRingBuffer() {
        if (this.inputRingBuffer == null) {
            this.inputRingBuffer = applicationContext.getBean("inputRingBuffer", RingBuffer.class);
        }
        return this.inputRingBuffer;
    }

    /**
     * 定時觸發快照（間隔由 {@code v2.snapshot.interval-ms} 設定）。
     * 發布 777 號事件到 Disruptor，由 MatchingEngine 負責攔截並觸發一致性快照。
     */
    @Scheduled(fixedDelayString = "${v2.snapshot.interval-ms}")
    public void takeSnapshot() {
        try {
            RingBuffer<OrderCommandEventV2> ringBuffer = getRingBuffer();
            long sequence = ringBuffer.next();
            try {
                OrderCommandEventV2 event = ringBuffer.get(sequence);
                event.reset();
                event.setCommandType(777); // SNAPSHOT_COMMAND
            } finally {
                ringBuffer.publish(sequence);
            }
            log.info("已發布 777 快照指令到 RingBuffer");
        } catch (Exception e) {
            log.error("快照發布失敗", e);
        }
    }

    // -------------------------------------------------------------------------
    // 公開方法（供 RecoveryServiceV2 與 MatchingEngineHandlerV2 使用）
    // -------------------------------------------------------------------------

    /**
     * 非同步將記憶體快照寫入磁碟 (供 MatchingEngineHandlerV2 呼叫)
     */
    public void asyncWriteSnapshotToFile(EngineSnapshotV2 snapshot) {
        CompletableFuture.runAsync(() -> {
            try {
                writeSnapshotToFile(snapshot);
                log.info("快照背景寫入完成，chronicleIndex={}", snapshot.getChronicleIndex());
            } catch (Exception e) {
                log.error("快照背景寫入失敗", e);
            }
        });
    }

    /**
     * 尋找最新的快照檔案，若不存在則回傳 null。
     */
    public EngineSnapshotV2 loadLatestSnapshot() throws IOException {
        Path dir = Paths.get(snapshotPath);
        if (!Files.exists(dir))
            return null;

        File[] files = dir.toFile()
                .listFiles(f -> f.getName().startsWith("snapshot-") && f.getName().endsWith(".bin"));
        if (files == null || files.length == 0)
            return null;

        /* 取最新（檔名含 timestamp，直接字串排序即可） */
        File latest = files[0];
        for (File f : files) {
            if (f.getName().compareTo(latest.getName()) > 0) {
                latest = f;
            }
        }

        log.info("載入快照：{}", latest.getName());
        byte[] data = Files.readAllBytes(latest.toPath());
        Bytes<ByteBuffer> bytes = Bytes.wrapForRead(ByteBuffer.wrap(data));
        try {
            Wire wire = WireType.BINARY_LIGHT.apply(bytes);

            long chronicleIndex = wire.read("chronicleIndex").int64();
            long createdAt = wire.read("createdAt").int64();

            Map<String, BigDecimal> available = new HashMap<>();
            int availableSize = wire.read("availableSize").int32();
            for (int i = 0; i < availableSize; i++) {
                String k = wire.read("k").text();
                String v = wire.read("v").text();
                available.put(k, new BigDecimal(v));
            }

            Map<String, BigDecimal> frozen = new HashMap<>();
            int frozenSize = wire.read("frozenSize").int32();
            for (int i = 0; i < frozenSize; i++) {
                String k = wire.read("k").text();
                String v = wire.read("v").text();
                frozen.put(k, new BigDecimal(v));
            }

            List<EngineSnapshotV2.OrderSnapshotEntryV2> orders = new ArrayList<>();
            int ordersSize = wire.read("ordersSize").int32();
            for (int i = 0; i < ordersSize; i++) {
                EngineSnapshotV2.OrderSnapshotEntryV2 o = new EngineSnapshotV2.OrderSnapshotEntryV2();
                o.setOrderId(wire.read("orderId").int64());
                o.setTraderId(wire.read("traderId").int64());
                o.setBaseAssetId(wire.read("baseAssetId").int64());
                o.setQuoteAssetId(wire.read("quoteAssetId").int64());
                o.setSide(wire.read("side").int32());
                String p = wire.read("price").text();
                o.setPrice((p == null || p.isEmpty()) ? null : new BigDecimal(p));
                o.setTotalQty(new BigDecimal(wire.read("totalQty").text()));
                o.setFilledQty(new BigDecimal(wire.read("filledQty").text()));
                orders.add(o);
            }

            return new EngineSnapshotV2(chronicleIndex, createdAt, available, frozen, orders);
        } finally {
            bytes.releaseLast();
        }
    }

    // -------------------------------------------------------------------------
    // 內部實作
    // -------------------------------------------------------------------------

    private void writeSnapshotToFile(EngineSnapshotV2 snapshot) throws IOException {
        Path dir = Paths.get(snapshotPath);
        Files.createDirectories(dir);
        String filename = "snapshot-" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".bin";
        Path file = dir.resolve(filename);

        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(1024 * 1024);
        try {
            Wire wire = WireType.BINARY_LIGHT.apply(bytes);

            wire.write("chronicleIndex").int64(snapshot.getChronicleIndex());
            wire.write("createdAt").int64(snapshot.getCreatedAt());

            wire.write("availableSize").int32(snapshot.getAvailable().size());
            for (Map.Entry<String, BigDecimal> entry : snapshot.getAvailable().entrySet()) {
                wire.write("k").text(entry.getKey());
                wire.write("v").text(entry.getValue().toPlainString());
            }

            wire.write("frozenSize").int32(snapshot.getFrozen().size());
            for (Map.Entry<String, BigDecimal> entry : snapshot.getFrozen().entrySet()) {
                wire.write("k").text(entry.getKey());
                wire.write("v").text(entry.getValue().toPlainString());
            }

            wire.write("ordersSize").int32(snapshot.getOrders().size());
            for (EngineSnapshotV2.OrderSnapshotEntryV2 o : snapshot.getOrders()) {
                wire.write("orderId").int64(o.getOrderId());
                wire.write("traderId").int64(o.getTraderId());
                wire.write("baseAssetId").int64(o.getBaseAssetId());
                wire.write("quoteAssetId").int64(o.getQuoteAssetId());
                wire.write("side").int32(o.getSide());
                wire.write("price").text(o.getPrice() != null ? o.getPrice().toPlainString() : "");
                wire.write("totalQty").text(o.getTotalQty().toPlainString());
                wire.write("filledQty").text(o.getFilledQty().toPlainString());
            }

            Files.write(file, bytes.toByteArray());
        } finally {
            bytes.releaseLast();
        }
    }
}
