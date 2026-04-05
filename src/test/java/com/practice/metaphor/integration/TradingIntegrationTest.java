package com.practice.metaphor.integration;

import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.model.entity.Balance;
import com.practice.metaphor.service.TradingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 實際資料庫連測：驗證撮合與資產劃撥
 */
@SpringBootTest
@ActiveProfiles("test") // 使用 H2 資料庫設定
@Transactional // 測試完自動 Rollback，保持資料庫乾淨
class TradingIntegrationTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private BalanceMapper balanceMapper;

    @Test
    void testAliceBuyBobSell_WithPriceImprovement() {
        // --- 準備資料 (使用 data.sql 中的初始狀態) ---
        // Alice (ID:1) 初始: 10,000 USD (Asset 1), 0 VT (Asset 2)
        // Bob (ID:2) 初始: 0 USD (Asset 1), 100 VT (Asset 2)
        // 市場 ID: 1 (VT/USD)

        // 1. Bob 先掛賣單 (Maker): 賣 5 股 VT @ 100 USD/股
        // 因為沒對手，這筆單會掛在 Book 裡
        tradingService.placeOrder(2L, 1L, 1, new BigDecimal("100.00"), new BigDecimal("5.00"));

        // 2. Alice 下買單 (Taker): 買 5 股 VT @ 110 USD/股 (出高價)
        // 觸發撮合！因為 Bob 賣更便宜，會發生 "Price Improvement"
        tradingService.placeOrder(1L, 1L, 0, new BigDecimal("110.00"), new BigDecimal("5.00"));

        // --- 3. 驗證 Alice (買家) 的最終餘額 ---
        Balance aliceUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 1L);
        assertTrue(new BigDecimal("9500.0000").compareTo(aliceUsd.availableAmount()) == 0, "Alice 成交後可用餘額不正確");
        assertTrue(new BigDecimal("0.0000").compareTo(aliceUsd.frozenAmount()) == 0, "Alice 應該沒有殘留的凍結金");

        // 預期拿到 5 股 VT
        Balance aliceVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 2L);
        assertTrue(new BigDecimal("5.0000").compareTo(aliceVt.availableAmount()) == 0, "Alice 應該拿到 5 股 VT");

        // --- 4. 驗證 Bob (賣家) 的最終餘額 ---
        // 賣家拿到了 500 USD
        Balance bobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L);
        assertTrue(new BigDecimal("500.0000").compareTo(bobUsd.availableAmount()) == 0, "Bob 應該拿到 500 USD");
        
        // 賣家剩下 100 - 5 = 95 股 VT
        Balance bobVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 2L);
        assertTrue(new BigDecimal("95.0000").compareTo(bobVt.availableAmount()) == 0, "Bob 的 VT 庫存應該剩下 95 股");
    }

    @Test
    void testMarketChaos_ConservationLaw() throws Exception {
        // --- 1. 準備隨機性因子 ---
        java.util.Random random = new java.util.Random();
        int taskCount = 100; // 模擬總單量
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(10);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(taskCount);

        // --- 2. 開始混沌下單模式 ---
        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    Long traderId = (long) (random.nextInt(10) + 1); // 1~10
                    int side = random.nextInt(2); // 0:Buy, 1:Sell
                    // 價格在 95~105 之間隨機震盪
                    BigDecimal price = new BigDecimal(95 + random.nextInt(11)); 
                    BigDecimal qty = new BigDecimal(random.nextInt(5) + 1); // 1~5 股

                    tradingService.placeOrder(traderId, 1L, side, price, qty);
                } catch (Exception e) {
                    System.err.println("下單異常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // --- 3. 驗證資產守恆 (Asset Conservation Law) ---
        // 初始 USD: 10 * 10,000,000 = 100,000,000
        // 初始 VT:  10 * 1,000 = 10,000
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalVt = BigDecimal.ZERO;

        for (long id = 1; id <= 10; id++) {
            Balance usdBal = balanceMapper.findByTraderIdAndAssetIdForUpdate(id, 1L);
            Balance vtBal = balanceMapper.findByTraderIdAndAssetIdForUpdate(id, 2L);
            
            totalUsd = totalUsd.add(usdBal.availableAmount()).add(usdBal.frozenAmount());
            totalVt = totalVt.add(vtBal.availableAmount()).add(vtBal.frozenAmount());
        }

        assertTrue(new BigDecimal("100000000.0000").compareTo(totalUsd) == 0, "USD 總合不守恆！目前: " + totalUsd);
        assertTrue(new BigDecimal("10000.0000").compareTo(totalVt) == 0, "VT 總合不守恆！目前: " + totalVt);
    }
}
