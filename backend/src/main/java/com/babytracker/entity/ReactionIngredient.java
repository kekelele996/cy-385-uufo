package com.babytracker.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("reaction_ingredient")
public class ReactionIngredient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reactionId;
    private String ingredientName;
    /** POSITIVE: 是否被牵连排除；NEGATIVE: 恒为 false 表示本次安全验证 */
    private Boolean implicated;
}
