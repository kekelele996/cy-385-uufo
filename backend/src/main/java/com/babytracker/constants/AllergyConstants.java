package com.babytracker.constants;

/**
 * 辅食过敏排除闭环相关常量
 */
public final class AllergyConstants {

    /** 食材状态：已确认安全 */
    public static final String STATUS_SAFE = "SAFE";
    /** 食材状态：已排除 */
    public static final String STATUS_EXCLUDED = "EXCLUDED";

    /** 餐后反应观察窗口：72 小时 */
    public static final long REACTION_WINDOW_HOURS = 72;

    /** 食材名称分隔符（兼容中文顿号、逗号与英文逗号） */
    public static final String INGREDIENT_SPLIT_REGEX = "[、,，;；/\\s]+";

    private AllergyConstants() {}
}
