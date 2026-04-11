package com.practice.metaphor.v1.exception;

import com.practice.metaphor.v1.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全域例外攔截器 - [終極安全版]
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 【業務異常】：這是我們自己丟出的「餘額不足」、「市場無效」
     * 這些訊息需要明確讓使用者看見，且不需要印出詳細 Stacktrack
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("【業務攔截】: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    /**
     * 【系統異常】：凡是 MyBatis、資料庫報錯、空指標 (NPE)、語法錯誤
     * 全部都在這裡被「蒙面」。我們只給前端一個模糊的提示與 Error ID。
     */
    @ExceptionHandler(Throwable.class)
    public ApiResponse<Void> handleOtherException(Throwable e) {
        // 在伺服器端印出「完整」錯誤，方便工程師救火
        log.error("【系統崩潰】關鍵錯誤: ", e);
        
        // 對外呈現絕對友善且安全的訊息
        return ApiResponse.error(500, "交易系統繁忙，請稍後重試。 (Support ID: " + System.currentTimeMillis() + ")");
    }
}
