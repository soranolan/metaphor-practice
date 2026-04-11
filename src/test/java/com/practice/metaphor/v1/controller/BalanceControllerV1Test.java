package com.practice.metaphor.v1.controller;

import com.practice.metaphor.v1.dto.ApiResponseV1;
import com.practice.metaphor.v1.mapper.BalanceMapperV1;
import com.practice.metaphor.v1.model.entity.BalanceV1;
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

class BalanceControllerV1Test {

    @Mock
    private BalanceMapperV1 balanceMapper;

    @InjectMocks
    private BalanceControllerV1 balanceController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetBalances() {
        // Arrange
        Long traderId = 1L;
        List<BalanceV1> mockBalances = Arrays.asList(
            new BalanceV1(1L, traderId, 1L, new BigDecimal("100.00"), BigDecimal.ZERO, LocalDateTime.now()),
            new BalanceV1(2L, traderId, 2L, new BigDecimal("10.00"), BigDecimal.ZERO, LocalDateTime.now())
        );
        when(balanceMapper.findByTraderId(traderId)).thenReturn(mockBalances);

        // Act
        // 現在預期回傳型別是 ApiResponseV1<List<BalanceV1>>
        ApiResponseV1<List<BalanceV1>> response = balanceController.getBalances(traderId);

        // Assert
        assertEquals(200, response.code());
        assertEquals("Success", response.message());
        assertEquals(2, response.data().size());
        verify(balanceMapper, times(1)).findByTraderId(traderId);
    }
}
