package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feeding_meal")
public class FeedingMeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String mealType;
    private LocalDateTime mealTime;
    private LocalDateTime createdAt;

    /** 该餐的食材列表（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<String> ingredients;
    /** 该餐的有效反应（非数据库字段，无反应时为 null） */
    @TableField(exist = false)
    private AllergyReaction reaction;
}
