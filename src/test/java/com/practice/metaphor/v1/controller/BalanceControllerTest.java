package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.dto.ApiResponse;
import com.practice.metaphor.v1.mapper.BalanceMapper;
import com.practice.metaphor.v1.model.entity.Balance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BalanceControllerTest {

    @Mock
    private BalanceMapper balanceMapper;

    @InjectMocks
    private BalanceController balanceController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetBalances() {
        // Arrange
        Long traderId = 1L;
        List<Balance> mockBalances = Arrays.asList(
            new Balance(1L, traderId, 1L, new BigDecimal("100.00"), BigDecimal.ZERO, LocalDateTime.now()),
            new Balance(2L, traderId, 2L, new BigDecimal("10.00"), BigDecimal.ZERO, LocalDateTime.now())
        );
        when(balanceMapper.findByTraderId(traderId)).thenReturn(mockBalances);

        // Act
        // 現在預期回傳型別是 ApiResponse<List<Balance>>
        ApiResponse<List<Balance>> response = balanceController.getBalances(traderId);

        // Assert
        assertEquals(200, response.code());
        assertEquals("Success", response.message());
        assertEquals(2, response.data().size());
        verify(balanceMapper, times(1)).findByTraderId(traderId);
    }
}
