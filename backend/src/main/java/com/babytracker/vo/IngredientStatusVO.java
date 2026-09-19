package com.babytracker.vo;

import lombok.Data;

@Data
public class IngredientStatusVO {
    private Long id;
    private Long babyId;
    private String ingredientName;
    /** UNVERIFIED / SAFE / EXCLUDED */
    private String status;
    private String statusLabel;
    /** 状态原因，说明由哪条反应验证/牵连，或撤销后恢复 */
    private String reason;
}
