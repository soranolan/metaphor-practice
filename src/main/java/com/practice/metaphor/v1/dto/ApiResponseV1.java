package com.practice.metaphor.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 統一 API 回應包裝物件
 * 
 * @param <T> 回應數據的型別
 */
@Schema(description = "通用 API 回應包裝器")
public record ApiResponseV1<T>(
    @Schema(description = "業務狀態碼 (200 代表成功, 400x 代表異常)", example = "200")
    int code,

    @Schema(description = "提示訊息", example = "操作成功")
    String message,

    @Schema(description = "實際回傳的數據內容")
    T data
) {
    /**
     * 快速建立成功回應 (無資料)
     */
    public static ApiResponseV1<String> success() {
        return new ApiResponseV1<>(200, "Success", "OK");
    }

    /**
     * 快速建立成功回應 (有資料)
     */
    public static <T> ApiResponseV1<T> success(T data) {
        return new ApiResponseV1<>(200, "Success", data);
    }

    /**
     * 快速建立失敗回應
     */
    public static ApiResponseV1<Void> error(int code, String message) {
        return new ApiResponseV1<>(code, message, null);
    }
}
