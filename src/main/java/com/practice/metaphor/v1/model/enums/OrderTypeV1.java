package com.practice.metaphor.v1.model.enums;

/**
 * 訂單類型
 */
public enum OrderTypeV1 {
    LIMIT(0),   // 限價單
    MARKET(1);  // 市價單

    private final int value;

    OrderTypeV1(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static OrderTypeV1 fromValue(int value) {
        for (OrderTypeV1 type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的訂單類型: " + value);
    }
}
