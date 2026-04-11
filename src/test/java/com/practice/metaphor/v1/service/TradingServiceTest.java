package com.practice.metaphor.v1.service;

import com.practice.metaphor.v1.mapper.BalanceMapper;
import com.practice.metaphor.v1.mapper.MarketMapper;
import com.practice.metaphor.v1.mapper.OrderMapper;
import com.practice.metaphor.v1.model.entity.Market;
import com.practice.metaphor.v1.model.entity.Balance;
import com.practice.metaphor.v1.model.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class TradingServiceTest {

    @Mock
    private BalanceMapper balanceMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private MarketMapper marketMapper; // 新增：模擬市場規則查詢

    @Mock
    private MatchingService matchingService; // 修復 NPE

    @InjectMocks
    private TradingService tradingService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void testPlaceOrder_Success_MarketRefactor() {
        // Arrange
        Long marketId = 101L; // 假設 VT/USD 市場 ID
        Long traderId = 1L;
        BigDecimal price = new BigDecimal("100.0000");
        BigDecimal qty = new BigDecimal("2.0000");
        int sideInt = 0; // BUY

        // 模擬市場規則：Market 101 = Base(2:VT), Quote(1:USD)
        Market mockMarket = new Market(marketId, 2L, 1L, "VT/USD", 1);
        when(marketMapper.findById(marketId)).thenReturn(Optional.of(mockMarket));

        // 模擬買入時需要 200 USD 的餘額
        Balance mockBalance = new Balance(1L, traderId, 1L, new BigDecimal("1000.0000"), BigDecimal.ZERO, LocalDateTime.now());
        when(balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, 1L)).thenReturn(mockBalance);

        // Act
        // 注意：這裡現在只傳入 marketId，而不是兩個資產 ID 了 (這會導致編譯失敗)
        tradingService.placeOrder(traderId, marketId, sideInt, price, qty);

        // Assert
        // 驗證是否正確鎖定了 Quote Asset (1:USD)
        verify(balanceMapper, times(1)).updateBalance(
                eq(traderId), eq(1L), 
                argThat(val -> val.compareTo(new BigDecimal("800")) == 0),
                argThat(val -> val.compareTo(new BigDecimal("200")) == 0)
        );
        verify(orderMapper, times(1)).insert(any(Order.class));
    }

    @Test
    void testPlaceOrder_InvalidMarket() {
        // Arrange (模擬不存在的市場)
        Long invalidMarketId = 999L;
        when(marketMapper.findById(invalidMarketId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            tradingService.placeOrder(1L, invalidMarketId, 0, new BigDecimal("100"), new BigDecimal("1"));
        });

        assertTrue(exception.getMessage().contains("市場不存在"));
    }
}
