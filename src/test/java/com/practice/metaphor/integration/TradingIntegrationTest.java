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
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TradingIntegrationTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private BalanceMapper balanceMapper;

    @Test
    void testAliceBuyBobSell_WithPriceImprovement() {
        // --- 1. 抓取操作前的「初始相對基準點」 ---
        // 如此一來，不管 data.sql 中的預設金額怎麼改，我們只驗證「差額」是否正確
        BigDecimal initialAliceUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 1L).availableAmount();
        BigDecimal initialAliceVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 2L).availableAmount();
        
        BigDecimal initialBobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L).availableAmount();
        BigDecimal initialBobVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 2L).availableAmount();

        // 2. Bob 先掛賣單 (Maker): 賣 5 股 VT @ 100 USD/股
        // 因為沒對手，這筆單會掛在 Book 裡
        tradingService.placeOrder(2L, 1L, 1, new BigDecimal("100.00"), new BigDecimal("5.00"));

        // 3. Alice 下買單 (Taker): 買 5 股 VT @ 110 USD/股 (出高價)
        // 觸發撮合！因為 Bob 賣更便宜，會發生 "Price Improvement"
        tradingService.placeOrder(1L, 1L, 0, new BigDecimal("110.00"), new BigDecimal("5.00"));

        // --- 4. 驗證 Alice (買家) 的最終餘額變化 ---
        // Alice 花了 500 USD 去買，就算一開始用 110 掛單，也會退回溢價
        Balance finalAliceUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 1L);
        BigDecimal expectedAliceUsd = initialAliceUsd.subtract(new BigDecimal("500.0000"));
        org.junit.jupiter.api.Assertions.assertEquals(0, expectedAliceUsd.compareTo(finalAliceUsd.availableAmount()), "Alice 成交後可用餘額不正確");
        assertTrue(new BigDecimal("0.0000").compareTo(finalAliceUsd.frozenAmount()) == 0, "Alice 應該沒有殘留的凍結金");

        // Alice 預期拿到 5 股 VT
        Balance finalAliceVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 2L);
        BigDecimal expectedAliceVt = initialAliceVt.add(new BigDecimal("5.0000"));
        assertTrue(expectedAliceVt.compareTo(finalAliceVt.availableAmount()) == 0, "Alice 應該多拿到 5 股 VT");

        // --- 5. 驗證 Bob (賣家) 的最終餘額變化 ---
        // Bob 拿到了 500 USD
        Balance finalBobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L);
        BigDecimal expectedBobUsd = initialBobUsd.add(new BigDecimal("500.0000"));
        assertTrue(expectedBobUsd.compareTo(finalBobUsd.availableAmount()) == 0, "Bob 應該多拿到 500 USD");
        
        // Bob 賣出了 5 股 VT
        Balance finalBobVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 2L);
        BigDecimal expectedBobVt = initialBobVt.subtract(new BigDecimal("5.0000"));
        assertTrue(expectedBobVt.compareTo(finalBobVt.availableAmount()) == 0, "Bob 的 VT 庫存應該少了 5 股");
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
