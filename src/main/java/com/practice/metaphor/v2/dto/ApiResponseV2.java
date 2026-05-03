package com.practice.metaphor.v2.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 統一 API 回應格式 (V2)
 */
@Schema(description = "V2 統一 API 回應格式")
public record ApiResponseV2<T>(

    @Schema(description = "回應代碼 (200 為成功)", example = "200")
    int code,

    @Schema(description = "回應訊息", example = "Success")
    String message,

    @Schema(description = "回應資料")
    T data
) {
    /**
     * 成功回應 (無資料)
     */
    public static ApiResponseV2<String> success() {
        return new ApiResponseV2<>(200, "Success", "OK");
    }

    /**
     * 成功回應 (帶資料)
     */
    public static <T> ApiResponseV2<T> success(T data) {
        return new ApiResponseV2<>(200, "Success", data);
    }

    /**
     * 錯誤回應
     */
    public static ApiResponseV2<Void> error(int code, String message) {
        return new ApiResponseV2<>(code, message, null);
    }
}
