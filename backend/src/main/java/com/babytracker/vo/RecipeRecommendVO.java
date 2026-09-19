package com.babytracker.vo;

import com.babytracker.entity.FoodRecipe;
import lombok.Data;
import java.util.List;

@Data
public class RecipeRecommendVO {
    private List<FoodRecipe> recipes;
    /** 被排除的食材名称（来自该宝宝的过敏排除状态） */
    private List<String> excludedIngredients;
    private List<BlockedRecipeVO> blockedRecipes;
}
