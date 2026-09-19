package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ingredient_status_log")
public class IngredientStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String ingredientName;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private Long relatedReactionId;
    private LocalDateTime createdAt;
}
