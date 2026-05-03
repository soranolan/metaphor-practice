package com.practice.metaphor.v2.engine;

import com.practice.metaphor.v2.exception.InsufficientBalanceExceptionV2;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 記憶體餘額帳本（AccountBook）
 *
 * <p>維護所有交易員的 available / frozen 餘額，作為撮合系統的資金真相來源。
 * DB 的 balances 表僅是非同步投影的 Read Model。
 *
 * <p>執行緒安全設計：
 * <ul>
 *   <li>{@code checkAndFreeze()} 由 HTTP 執行緒呼叫（TradingServiceV2）</li>
 *   <li>{@code settle()} / {@code release()} 由 Disruptor 單執行緒呼叫（MatchingEngineHandlerV2）</li>
 * </ul>
 * 使用 {@code synchronized} 確保跨執行緒的原子性。
 */
@Component
public class AccountBookV2 {

    /** key = "traderId:assetId" */
    private final Map<String, BigDecimal> available = new HashMap<>();
    private final Map<String, BigDecimal> frozen    = new HashMap<>();

    // -------------------------------------------------------------------------
    // 初始化（由 RecoveryServiceV2 呼叫）
    // -------------------------------------------------------------------------

    /** 載入單一餘額紀錄（啟動時從 DB 或 Snapshot 初始化）。 */
    public synchronized void load(long traderId, long assetId,
                                  BigDecimal availableAmt, BigDecimal frozenAmt) {
        String key = key(traderId, assetId);
        available.put(key, availableAmt);
        frozen.put(key, frozenAmt);
    }

    /** 清除所有資料（重建前呼叫）。 */
    public synchronized void clear() {
        available.clear();
        frozen.clear();
    }

    // -------------------------------------------------------------------------
    // 交易操作
    // -------------------------------------------------------------------------

    /**
     * 驗證並凍結餘額（HTTP 執行緒呼叫）。
     * available - amount；frozen + amount。
     *
     * @throws InsufficientBalanceExceptionV2 當可用餘額不足時
     */
    public synchronized void checkAndFreeze(long traderId, long assetId, BigDecimal amount) {
        String key = key(traderId, assetId);
        BigDecimal avail = available.getOrDefault(key, BigDecimal.ZERO);
        if (avail.compareTo(amount) < 0) {
            throw new InsufficientBalanceExceptionV2(
                    "【交易失敗】餘額不足，traderId=" + traderId + ", assetId=" + assetId
                            + "，需要=" + amount + "，可用=" + avail);
        }
        available.put(key, avail.subtract(amount));
        frozen.merge(key, amount, BigDecimal::add);
    }

    /**
     * 成交結算（Disruptor 執行緒呼叫）。
     * 解凍 frozenAmount，並將 receiveAmount 計入 available。
     *
     * @param traderId      交易員 ID
     * @param unfreezeAsset 解凍的資產 ID（賣出的資產）
     * @param unfreezeAmt   解凍金額
     * @param receiveAsset  收到的資產 ID（買入的資產）
     * @param receiveAmt    收到金額
     */
    public synchronized void settle(long traderId,
                                    long unfreezeAsset, BigDecimal unfreezeAmt,
                                    long receiveAsset, BigDecimal receiveAmt) {
        /* 解凍賣出資產 */
        String unfreezeKey = key(traderId, unfreezeAsset);
        frozen.merge(unfreezeKey, unfreezeAmt.negate(), BigDecimal::add);

        /* 增加買入資產的可用餘額 */
        String receiveKey = key(traderId, receiveAsset);
        available.merge(receiveKey, receiveAmt, BigDecimal::add);
    }

    /**
     * 退款（市價單未成交部分或限價買單的價差退款）。
     * frozen → available。
     */
    public synchronized void release(long traderId, long assetId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        String key = key(traderId, assetId);
        frozen.merge(key, amount.negate(), BigDecimal::add);
        available.merge(key, amount, BigDecimal::add);
    }

    // -------------------------------------------------------------------------
    // 查詢（供 Snapshot 使用）
    // -------------------------------------------------------------------------

    public synchronized BigDecimal getAvailable(long traderId, long assetId) {
        return available.getOrDefault(key(traderId, assetId), BigDecimal.ZERO);
    }

    public synchronized BigDecimal getFrozen(long traderId, long assetId) {
        return frozen.getOrDefault(key(traderId, assetId), BigDecimal.ZERO);
    }

    /** 取得所有 available 餘額的快照副本（供序列化用）。 */
    public synchronized Map<String, BigDecimal> snapshotAvailable() {
        return new HashMap<>(available);
    }

    /** 取得所有 frozen 餘額的快照副本（供序列化用）。 */
    public synchronized Map<String, BigDecimal> snapshotFrozen() {
        return new HashMap<>(frozen);
    }

    // -------------------------------------------------------------------------
    // 工具
    // -------------------------------------------------------------------------

    private String key(long traderId, long assetId) {
        return traderId + ":" + assetId;
    }
}
