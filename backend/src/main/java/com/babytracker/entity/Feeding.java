package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feeding")
public class Feeding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long babyId;
    private String mealType;
    private LocalDateTime eatenAt;
    private String note;
    private LocalDateTime createdAt;
}
