package com.practice.metaphor.v2.exception;

/**
 * V2 餘額不足例外
 */
public class InsufficientBalanceExceptionV2 extends RuntimeException {
    public InsufficientBalanceExceptionV2(String message) {
        super(message);
    }
}
