package com.practice.metaphor.v1.model.enums;

/**
 * 訂單狀態：0: 新委託, 1: 部分成交, 2: 完全成交, 3: 已撤單
 */
public enum OrderStatusV1 {
    NEW(0, "新委託"),
    PARTIAL_FILLED(1, "部分成交"),
    FILLED(2, "完全成交"),
    CANCELED(3, "已撤單");

    public final int value;
    public final String description;

    OrderStatusV1(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static OrderStatusV1 fromValue(int value) {
        for (OrderStatusV1 status : OrderStatusV1.values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的訂單狀態代碼: " + value);
    }
}
