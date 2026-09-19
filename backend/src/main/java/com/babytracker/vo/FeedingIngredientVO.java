package com.babytracker.vo;

import lombok.Data;

@Data
public class FeedingIngredientVO {
    private String name;
    /** 该餐登记反应时该食材的状态：UNVERIFIED/SAFE/EXCLUDED */
    private String status;
    private String statusLabel;
    private String reason;
}
