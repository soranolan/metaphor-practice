package com.practice.metaphor.controller;

import com.practice.metaphor.mapper.BalanceMapper;
import com.practice.metaphor.model.entity.Balance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BalanceControllerTest {

    @Mock
    private BalanceMapper balanceMapper;

    @InjectMocks
    private BalanceController balanceController;

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
    void testGetBalances_Success() {
        // Arrange (準備測試資料)
        Long traderId = 1L;
        List<Balance> mockBalances = new ArrayList<>();
        // 模擬兩筆持倉：USD 和 VT
        mockBalances.add(new Balance(1L, traderId, 1L, new BigDecimal("10000.0000"), BigDecimal.ZERO, LocalDateTime.now()));
        mockBalances.add(new Balance(2L, traderId, 2L, new BigDecimal("10.0000"), BigDecimal.ZERO, LocalDateTime.now()));

        when(balanceMapper.findByTraderId(traderId)).thenReturn(mockBalances);

        // Act (執行被測方法)
        List<Balance> result = balanceController.getBalances(traderId);

        // Assert (驗證結果是否正確)
        assertEquals(2, result.size(), "應該正確返回該交易員的所有 2 個資產持倉");
        assertEquals(traderId, result.get(0).traderId(), "traderId 應為 1");
        
        // 驗證 Mapper 的方法確實被調用過一次
        verify(balanceMapper, times(1)).findByTraderId(traderId);
    }

    @Test
    void testGetBalances_Empty() {
        // Arrange (模擬找不到任何持倉的情況)
        Long traderId = 999L; 
        when(balanceMapper.findByTraderId(traderId)).thenReturn(new ArrayList<>());

        // Act
        List<Balance> result = balanceController.getBalances(traderId);

        // Assert
        assertEquals(0, result.size(), "若找不到對應交易員，應返回空清單而不是 null");
        verify(balanceMapper, times(1)).findByTraderId(traderId);
    }
}
