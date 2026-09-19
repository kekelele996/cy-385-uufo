package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("feeding_ingredient")
public class FeedingIngredient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long feedingId;
    private String name;
}
