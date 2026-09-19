package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("meal_reaction")
public class MealReaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long feedingId;
    private Long babyId;
    /** ReactionType: POSITIVE / NEGATIVE */
    private String reactionType;
    private LocalDateTime observedAt;
    private String symptoms;
    private String note;
    private Boolean revoked;
    private String revokedReason;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    /** 数据库生成列：仅未撤销反应有值，配合唯一约束保证每餐一条有效反应 */
    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private Long activeFeedingId;
}
