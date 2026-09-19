package com.babytracker.constants;

/**
 * 食材在某个宝宝档案下的过敏排除状态
 */
public enum IngredientStatusType {
    UNVERIFIED("未验证"),
    SAFE("已确认安全"),
    EXCLUDED("已排除");

    private final String label;

    IngredientStatusType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
