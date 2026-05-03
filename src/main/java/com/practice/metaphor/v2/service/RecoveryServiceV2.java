package com.practice.metaphor.v2.service;

import com.practice.metaphor.v2.disruptor.handler.MatchingEngineHandlerV2;
import com.practice.metaphor.v2.engine.AccountBookV2;
import com.practice.metaphor.v2.engine.OrderEntryV2;
import com.practice.metaphor.v2.mapper.BalanceMapperV2;
import com.practice.metaphor.v2.model.entity.BalanceV2;
import com.practice.metaphor.v2.snapshot.EngineSnapshotV2;
import com.practice.metaphor.v2.snapshot.SnapshotServiceV2;
import jakarta.annotation.PostConstruct;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 引擎重啟恢復服務
 *
 * <p>
 * 系統啟動時（{@link PostConstruct}）依序執行：
 * <ol>
 * <li>嘗試載入最新 Snapshot，還原 OrderBookV2 + AccountBookV2</li>
 * <li>若無 Snapshot，從 DB 載入所有餘額作為 AccountBook 初始狀態</li>
 * <li>使用 Chronicle Queue ExcerptTailer 從 Snapshot 的 chronicleIndex 繼續
 * Replay</li>
 * </ol>
 *
 * <p>
 * Replay 為純記憶體操作，不觸發任何 DB 寫入。
 */
@Service
public class RecoveryServiceV2 {

    private static final Logger log = LoggerFactory.getLogger(RecoveryServiceV2.class);

    private final AccountBookV2 accountBook;
    private final MatchingEngineHandlerV2 matchingEngine;
    private final SnapshotServiceV2 snapshotService;
    private final BalanceMapperV2 balanceMapper;
    private final ChronicleQueue chronicleQueue;

    public RecoveryServiceV2(AccountBookV2 accountBook,
            MatchingEngineHandlerV2 matchingEngine,
            SnapshotServiceV2 snapshotService,
            BalanceMapperV2 balanceMapper,
            ChronicleQueue chronicleQueue) {
        this.accountBook = accountBook;
        this.matchingEngine = matchingEngine;
        this.snapshotService = snapshotService;
        this.balanceMapper = balanceMapper;
        this.chronicleQueue = chronicleQueue;
    }

    @PostConstruct
    public void recover() {
        log.info("=== V2 引擎恢復開始 ===");
        long replayFromIndex = 0L;

        try {
            EngineSnapshotV2 snapshot = snapshotService.loadLatestSnapshot();

            if (snapshot != null) {
                /* === 路徑 A：從快照還原 === */
                log.info("發現快照（chronicleIndex={}），開始還原...", snapshot.getChronicleIndex());
                restoreFromSnapshot(snapshot);
                replayFromIndex = snapshot.getChronicleIndex();
            } else {
                /* === 路徑 B：無快照，從 DB 載入初始餘額 === */
                log.info("無快照，從 DB 載入初始餘額...");
                bootstrapFromDb();
                replayFromIndex = 0L;
            }

            /* === Replay Chronicle Journal === */
            long replayed = replayJournal(replayFromIndex);
            log.info("=== V2 引擎恢復完成，共 Replay {} 筆事件 ===", replayed);

        } catch (Exception e) {
            log.error("引擎恢復失敗，系統可能處於不一致狀態！", e);
            throw new RuntimeException("V2 引擎恢復失敗", e);
        }
    }

    // -------------------------------------------------------------------------
    // 還原邏輯
    // -------------------------------------------------------------------------

    private void restoreFromSnapshot(EngineSnapshotV2 snapshot) {
        accountBook.clear();

        /* 還原 AccountBook */
        if (snapshot.getAvailable() != null) {
            snapshot.getAvailable().forEach((key, amount) -> {
                String[] parts = key.split(":");
                long traderId = Long.parseLong(parts[0]);
                long assetId = Long.parseLong(parts[1]);
                BigDecimal frozen = snapshot.getFrozen() != null
                        ? snapshot.getFrozen().getOrDefault(key, BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                accountBook.load(traderId, assetId, amount, frozen);
            });
        }

        /* 還原 OrderBook */
        if (snapshot.getOrders() != null) {
            snapshot.getOrders().forEach(entry -> {
                OrderEntryV2 order = entry.toOrderEntry();
                matchingEngine.getOrCreateOrderBook(order.getBaseAssetId(), order.getQuoteAssetId())
                        .addOrder(order);
            });
        }
    }

    private void bootstrapFromDb() {
        accountBook.clear();
        List<BalanceV2> balances = balanceMapper.findAll();
        balances.forEach(b -> accountBook.load(
                b.traderId(), b.assetId(),
                b.availableAmount(), b.frozenAmount()));
        log.info("從 DB 載入 {} 筆餘額紀錄", balances.size());
    }

    // -------------------------------------------------------------------------
    // Journal Replay
    // -------------------------------------------------------------------------

    private long replayJournal(long fromIndex) {
        long count = 0;
        try (ExcerptTailer tailer = chronicleQueue.createTailer("recovery")) {
            if (fromIndex > 0) {
                tailer.moveToIndex(fromIndex);
            }

            while (true) {
                try (DocumentContext dc = tailer.readingDocument()) {
                    if (!dc.isPresent())
                        break;

                    int commandType = dc.wire().read("commandType").int32();
                    long orderId = dc.wire().read("orderId").int64();
                    long traderId = dc.wire().read("traderId").int64();
                    long baseAssetId = dc.wire().read("baseAssetId").int64();
                    long quoteAssetId = dc.wire().read("quoteAssetId").int64();
                    int side = dc.wire().read("side").int32();
                    String priceStr = dc.wire().read("price").text();
                    String qtyStr = dc.wire().read("totalQty").text();

                    BigDecimal price = (priceStr != null && !priceStr.isEmpty())
                            ? new BigDecimal(priceStr)
                            : null;
                    BigDecimal qty = (qtyStr != null && !qtyStr.isEmpty())
                            ? new BigDecimal(qtyStr)
                            : BigDecimal.ZERO;

                    applyReplayEvent(commandType, orderId, traderId,
                            baseAssetId, quoteAssetId, side, price, qty);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 將 Journal 事件重新 apply 至記憶體（純 OrderBook 操作，不觸發 AccountBook 結算，
     * 因為餘額已由快照還原；若無快照，結算邏輯需完整 replay — 此處為簡化版本）。
     */
    private void applyReplayEvent(int commandType, long orderId, long traderId,
            long baseAssetId, long quoteAssetId,
            int side, BigDecimal price, BigDecimal qty) {
        if (commandType == 777) {
            return; // 略過快照事件
        }

        if (commandType == 0 && price != null) {
            /* PLACE_ORDER_LIMIT：重建 OrderBook 中的掛單 */
            OrderEntryV2 order = new OrderEntryV2(orderId, traderId,
                    baseAssetId, quoteAssetId, side, price, qty);
            matchingEngine.getOrCreateOrderBook(baseAssetId, quoteAssetId).addOrder(order);
        }
        /* 市價單與撮合事件的完整 Replay 邏輯可在後續迭代中補充 */
    }
}
