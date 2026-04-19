package com.practice.metaphor.v1.integration;

import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import com.practice.metaphor.v1.service.TradingServiceV1;
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
class TradingIntegrationTestV1 {

    @Autowired
    private TradingServiceV1 tradingService;

    @Autowired
    private BalanceMapperV1 balanceMapper;

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
        BalanceV1 finalAliceUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 1L);
        BigDecimal expectedAliceUsd = initialAliceUsd.subtract(new BigDecimal("500.0000"));
        org.junit.jupiter.api.Assertions.assertEquals(0, expectedAliceUsd.compareTo(finalAliceUsd.availableAmount()), "Alice 成交後可用餘額不正確");
        assertTrue(new BigDecimal("0.0000").compareTo(finalAliceUsd.frozenAmount()) == 0, "Alice 應該沒有殘留的凍結金");

        // Alice 預期拿到 5 股 VT
        BalanceV1 finalAliceVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(1L, 2L);
        BigDecimal expectedAliceVt = initialAliceVt.add(new BigDecimal("5.0000"));
        assertTrue(expectedAliceVt.compareTo(finalAliceVt.availableAmount()) == 0, "Alice 應該多拿到 5 股 VT");

        // --- 5. 驗證 Bob (賣家) 的最終餘額變化 ---
        // Bob 拿到了 500 USD
        BalanceV1 finalBobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L);
        BigDecimal expectedBobUsd = initialBobUsd.add(new BigDecimal("500.0000"));
        assertTrue(expectedBobUsd.compareTo(finalBobUsd.availableAmount()) == 0, "Bob 應該多拿到 500 USD");
        
        // Bob 賣出了 5 股 VT
        BalanceV1 finalBobVt = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 2L);
        BigDecimal expectedBobVt = initialBobVt.subtract(new BigDecimal("5.0000"));
        assertTrue(expectedBobVt.compareTo(finalBobVt.availableAmount()) == 0, "Bob 的 VT 庫存應該少了 5 股");
    }

    @Test
    void testMultipleMatchesAndUnaffectedOrders_PriceImprovement() {
        // --- 1. 紀錄 Bob 初始餘額 ---
        BigDecimal initialBobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L).availableAmount();

        // --- 2. Bob 下一個很低價的防守買單 (變成 Maker，不會成交) ---
        // 買 10 股 VT @ 50 USD/股，需要預先鎖定 500 USD。
        // 這筆 500 USD 的凍結金不應該在後續的其他訂單成交時被不小心解凍還回去！
        tradingService.placeOrder(2L, 1L, 0, new BigDecimal("50.00"), new BigDecimal("10.00"));
        
        BalanceV1 bobAfterFirstOrder = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L);
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("500.0000").compareTo(bobAfterFirstOrder.frozenAmount()), "Bob 第一張單應該凍結了 500");

        // --- 3. Alice 佈置兩張賣單簿 (Maker) ---
        // 掛賣 3 股 VT @ 90 USD/股
        tradingService.placeOrder(1L, 1L, 1, new BigDecimal("90.00"), new BigDecimal("3.00"));
        // 掛賣 2 股 VT @ 100 USD/股
        tradingService.placeOrder(1L, 1L, 1, new BigDecimal("100.00"), new BigDecimal("2.00"));

        // --- 4. Bob 高價主動吃單 (Taker)，橫跨兩筆不同價格的訂單 ---
        // Bob 出價 110 USD/股，打算買 5 股 (觸發撮合時預先鎖定 550 USD)
        // 他會依序吃到 Alice 的 90 元 x 3 股 (花 270)、100 元 x 2 股 (花 200)，總花費 470。
        // 這筆訂單他將會獲得 (550 - 470) = 80 USD 的溢價退款。
        tradingService.placeOrder(2L, 1L, 0, new BigDecimal("110.00"), new BigDecimal("5.00"));

        // --- 5. 驗證 Bob (買家) 的凍結金與餘額 ---
        BalanceV1 finalBobUsd = balanceMapper.findByTraderIdAndAssetIdForUpdate(2L, 1L);
        
        // Bob 總共解除了 550 的凍結，並實際扣款 470，所以：可用餘額 = 初始 - 500(防守單鎖定) - 470(實際花費)
        BigDecimal expectedAvailable = initialBobUsd.subtract(new BigDecimal("970.0000"));
        org.junit.jupiter.api.Assertions.assertEquals(0, expectedAvailable.compareTo(finalBobUsd.availableAmount()), "Bob 的剩餘可用餘額不對");

        // 關鍵斷言！如果還是用舊版暴力的 refundExcessFrozen，這裡就會不小心變成 0（防守單的錢被誤退）。
        // 用新的逐筆結算法，就會精確保留那沒動到的 500 USD！
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("500.0000").compareTo(finalBobUsd.frozenAmount()), "Bob 防守單的 500 USD 必須被安全地保留在凍結中");
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

        // --- 3. 驗證資產守恆 (AssetV1 Conservation Law) ---
        // 初始 USD: 10 * 10,000,000 = 100,000,000
        // 初始 VT:  10 * 1,000 = 10,000
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalVt = BigDecimal.ZERO;

        for (long id = 1; id <= 10; id++) {
            BalanceV1 usdBal = balanceMapper.findByTraderIdAndAssetIdForUpdate(id, 1L);
            BalanceV1 vtBal = balanceMapper.findByTraderIdAndAssetIdForUpdate(id, 2L);
            
            totalUsd = totalUsd.add(usdBal.availableAmount()).add(usdBal.frozenAmount());
            totalVt = totalVt.add(vtBal.availableAmount()).add(vtBal.frozenAmount());
        }

        assertTrue(new BigDecimal("100000000.0000").compareTo(totalUsd) == 0, "USD 總合不守恆！目前: " + totalUsd);
        assertTrue(new BigDecimal("10000.0000").compareTo(totalVt) == 0, "VT 總合不守恆！目前: " + totalVt);
    }
}
