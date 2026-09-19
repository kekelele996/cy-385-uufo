package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ingredient_status")
public class IngredientStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String ingredientName;
    /** IngredientStatusType: UNVERIFIED / SAFE / EXCLUDED */
    private String status;
    private String reason;
    private LocalDateTime updatedAt;
}
