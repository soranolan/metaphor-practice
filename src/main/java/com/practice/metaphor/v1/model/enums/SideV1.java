package com.practice.metaphor.v1.model.enums;

/**
 * 交易方向：0 代表買入，1 代表賣出
 */
public enum SideV1 {
    BUY(0, "買入"),
    SELL(1, "賣出");

    public final int value;
    public final String description;

    SideV1(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SideV1 fromValue(int value) {
        for (SideV1 side : SideV1.values()) {
            if (side.value == value) {
                return side;
            }
        }
        throw new IllegalArgumentException("未知的交易方向代碼: " + value);
    }
}
