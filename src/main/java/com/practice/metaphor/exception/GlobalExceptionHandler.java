package com.practice.metaphor.exception;

import com.practice.metaphor.dto.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域例外攔截器
 * 捕捉業務發生異常時，將髒髒的 Stacktrace 轉換為乾淨的 ApiResponse 回傳
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理業務邏輯異常 (如餘額不足、市場不存在)
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e) {
        // 設定 400 為普通業務邏輯錯誤代碼
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 處理所有未捕捉的異常 (500)
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOtherException(Exception e) {
        return ApiResponse.error(500, "系統發生未預期錯誤：" + e.getMessage());
    }
}
