package com.babytracker.constants;

/**
 * 辅食过敏排除模块用到的业务错误码
 */
public final class AllergyErrorCode {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String FEEDING_NOT_FOUND = "FEEDING_NOT_FOUND";
    public static final String REACTION_NOT_FOUND = "REACTION_NOT_FOUND";
    public static final String REACTION_WINDOW_EXPIRED = "REACTION_WINDOW_EXPIRED";
    public static final String REACTION_ALREADY_EXISTS = "REACTION_ALREADY_EXISTS";
    public static final String REACTION_ALREADY_REVOKED = "REACTION_ALREADY_REVOKED";
    public static final String BABY_MISMATCH = "BABY_MISMATCH";
    private AllergyErrorCode() {}
}
