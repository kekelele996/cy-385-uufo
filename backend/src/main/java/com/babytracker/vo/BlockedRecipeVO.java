package com.babytracker.vo;

import lombok.Data;
import java.util.List;

@Data
public class BlockedRecipeVO {
    private Long id;
    private String name;
    /** 命中的已排除食材 */
    private List<String> hitIngredients;
}
