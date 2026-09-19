package com.babytracker.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedingCreateRequest {
    @NotNull(message = "宝宝ID不能为空")
    private Long babyId;
    @NotNull(message = "餐次类型不能为空")
    private String mealType;
    @NotNull(message = "喂养时间不能为空")
    private LocalDateTime eatenAt;
    @NotEmpty(message = "每餐至少记录一个食材")
    private List<String> ingredients;
    private String note;
}
