package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("baby_ingredient_status")
public class BabyIngredientStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String ingredientName;
    /** SAFE / EXCLUDED */
    private String status;
    private String reason;
    private LocalDateTime firstConfirmedAt;
    private LocalDateTime updatedAt;
}
