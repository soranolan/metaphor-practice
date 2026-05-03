package com.practice.metaphor.v1.exception;

import com.practice.metaphor.v1.dto.ApiResponseV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全域例外攔截器 - [終極安全版]
 */
@RestControllerAdvice(basePackages = "com.practice.metaphor.v1.controller")
public class GlobalExceptionHandlerV1 {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlerV1.class);

    /**
     * 【業務異常】：這是我們自己丟出的「餘額不足」、「市場無效」
     * 這些訊息需要明確讓使用者看見，且不需要印出詳細 Stacktrack
     */
    @ExceptionHandler(BusinessExceptionV1.class)
    public ApiResponseV1<Void> handleBusinessException(BusinessExceptionV1 e) {
        log.warn("【業務攔截】: {}", e.getMessage());
        return ApiResponseV1.error(400, e.getMessage());
    }

    /**
     * 【資源缺失】：例如瀏覽器自動請求的 favicon.ico，或是打錯的 API 路徑。
     * 這些不屬於系統崩潰，我們將其視為 INFO 記錄即可。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponseV1<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.info("【路徑不存在】: {}", e.getMessage());
        return ApiResponseV1.error(404, "資源路徑不存在");
    }

    /**
     * 【系統異常】：凡是 MyBatis、資料庫報錯、空指標 (NPE)、語法錯誤
     * 全部都在這裡被「蒙面」。我們只給前端一個模糊的提示與 Error ID。
     */
    @ExceptionHandler(Throwable.class)
    public ApiResponseV1<Void> handleOtherException(Throwable e) {
        // 在伺服器端印出「完整」錯誤，方便工程師救火
        log.error("【系統崩潰】關鍵錯誤: ", e);
        
        // 對外呈現絕對友善且安全的訊息
        return ApiResponseV1.error(500, "交易系統繁忙，請稍後重試。 (Support ID: " + System.currentTimeMillis() + ")");
    }
}
