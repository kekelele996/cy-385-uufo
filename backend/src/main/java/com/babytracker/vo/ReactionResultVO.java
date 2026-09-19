package com.babytracker.vo;

import com.babytracker.entity.AllergyReaction;
import com.babytracker.entity.BabyIngredientStatus;
import lombok.Data;
import java.util.List;

/**
 * 反应登记/撤销结果：同一餐重复或并发提交只返回同一条有效反应，
 * 并携带重算后的食材状态快照，刷新页面后内容一致。
 */
@Data
public class ReactionResultVO {
    private boolean duplicated;
    private AllergyReaction reaction;
    private List<BabyIngredientStatus> statuses;
}
