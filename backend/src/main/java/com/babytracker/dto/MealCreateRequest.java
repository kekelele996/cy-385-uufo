package com.babytracker.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 喂养记录创建请求：一餐可记录多个食材。
 * 喂养记录、食材明细在同一事务内一次生效。
 */
@Data
public class MealCreateRequest {
    private Long babyId;
    private String mealType;
    private LocalDateTime mealTime;
    /** 食材名称，支持字符串数组或顿号/逗号分隔的字符串 */
    private List<String> ingredients;
    private String ingredientsText;
}
