package com.babytracker.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedingVO {
    private Long id;
    private Long babyId;
    private String mealType;
    private String mealTypeLabel;
    private LocalDateTime eatenAt;
    private String note;
    private List<FeedingIngredientVO> ingredients;
    /** 有效反应（每餐至多一条），无则为 null */
    private ReactionVO reaction;
}
