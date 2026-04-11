package com.practice.metaphor.v1.exception;

/**
 * 業務邏輯異常
 * 用於主動拋出我們預期內的錯誤 (如：餘額不足、市場不存在)
 */
public class BusinessExceptionV1 extends RuntimeException {
    public BusinessExceptionV1(String message) {
        super(message);
    }
}
