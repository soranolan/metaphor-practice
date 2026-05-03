package com.practice.metaphor.v2.exception;

/**
 * 業務邏輯異常 (V2)
 */
public class BusinessExceptionV2 extends RuntimeException {
    public BusinessExceptionV2(String message) {
        super(message);
    }
}
