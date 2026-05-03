package com.practice.metaphor.v2.exception;

import com.practice.metaphor.v2.dto.ApiResponseV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域異常處理器 (V2)
 */
@RestControllerAdvice(basePackages = "com.practice.metaphor.v2.controller")
public class GlobalExceptionHandlerV2 {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlerV2.class);

    @ExceptionHandler(BusinessExceptionV2.class)
    public ApiResponseV2<Void> handleBusinessException(BusinessExceptionV2 e) {
        log.warn("V2 業務邏輯異常：{}", e.getMessage());
        return ApiResponseV2.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponseV2<Void> handleSystemException(Exception e) {
        log.error("V2 系統未預期異常", e);
        return ApiResponseV2.error(500, "系統繁忙，請稍後再試");
    }
}
