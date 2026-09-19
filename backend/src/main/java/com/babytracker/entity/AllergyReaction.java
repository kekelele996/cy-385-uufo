package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("allergy_reaction")
public class AllergyReaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long mealId;
    /** true=出现过敏反应；false=餐后满72小时无反应 */
    private Boolean reacted;
    private String symptoms;
    private LocalDateTime reactedAt;
    private Boolean revoked;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime createdAt;

    /** 生成列：未撤销时等于 mealId，撤销后为 NULL（只读，禁止插入/更新） */
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
                updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private Long activeMealId;
}
