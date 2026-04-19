package com.practice.metaphor.v1.service;

import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.mapper.MarketMapperV1;
import com.practice.metaphor.v1.mapper.OrderMapperV1;
import com.practice.metaphor.v1.model.entity.MarketV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
import com.practice.metaphor.v1.model.entity.OrderV1;
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

class TradingServiceV1Test {

    @Mock
    private BalanceMapperV1 balanceMapper;

    @Mock
    private OrderMapperV1 orderMapper;

    @Mock
    private MarketMapperV1 marketMapper; // 新增：模擬市場規則查詢

    @Mock
    private MatchingServiceV1 matchingService; // 修復 NPE

    @InjectMocks
    private TradingServiceV1 tradingService;

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

        // 模擬市場規則：MarketV1 101 = Base(2:VT), Quote(1:USD)
        MarketV1 mockMarket = new MarketV1(marketId, 2L, 1L, "VT/USD", 1);
        when(marketMapper.findById(marketId)).thenReturn(Optional.of(mockMarket));

        // 模擬買入時需要 200 USD 的餘額
        BalanceV1 mockBalance = new BalanceV1(1L, traderId, 1L, new BigDecimal("1000.0000"), BigDecimal.ZERO, LocalDateTime.now());
        when(balanceMapper.findByTraderIdAndAssetIdForUpdate(traderId, 1L)).thenReturn(mockBalance);

        // Act
        // 注意：這裡現在只傳入 marketId，而不是兩個資產 ID 了 (這會導致編譯失敗)
        tradingService.placeOrder(traderId, marketId, 0, sideInt, price, qty);

        // Assert
        // 驗證是否正確鎖定了 Quote AssetV1 (1:USD)
        verify(balanceMapper, times(1)).updateBalance(
                eq(traderId), eq(1L), 
                argThat(val -> val.compareTo(new BigDecimal("800")) == 0),
                argThat(val -> val.compareTo(new BigDecimal("200")) == 0)
        );
        verify(orderMapper, times(1)).insert(any(OrderV1.class));
    }

    @Test
    void testPlaceOrder_InvalidMarket() {
        // Arrange (模擬不存在的市場)
        Long invalidMarketId = 999L;
        when(marketMapper.findById(invalidMarketId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            tradingService.placeOrder(1L, invalidMarketId, 0, 0, new BigDecimal("100"), new BigDecimal("1"));
        });

        assertTrue(exception.getMessage().contains("市場不存在"));
    }
}
