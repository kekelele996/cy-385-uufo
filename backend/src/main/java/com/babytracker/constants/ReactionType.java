package com.babytracker.constants;

/**
 * 餐后反应类型：POSITIVE 表示出现疑似过敏反应，NEGATIVE 表示观察期内无异常
 */
public enum ReactionType {
    POSITIVE("有反应"),
    NEGATIVE("无异常");

    private final String label;

    ReactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
