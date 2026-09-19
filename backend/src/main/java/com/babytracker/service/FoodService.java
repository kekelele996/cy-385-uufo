package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.entity.FoodRecipe;
import com.babytracker.mapper.FoodMapper;
import com.babytracker.utils.IngredientNames;
import com.babytracker.vo.BlockedRecipeVO;
import com.babytracker.vo.RecipeRecommendVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FoodService {

    private final FoodMapper mapper;
    private final IngredientStatusService ingredientStatusService;

    public FoodService(FoodMapper mapper, IngredientStatusService ingredientStatusService) {
        this.mapper = mapper;
        this.ingredientStatusService = ingredientStatusService;
    }

    /** 旧版推荐：按月龄与过敏原关键字过滤 */
    public List<FoodRecipe> recommend(Integer monthAge, String allergen) {
        QueryWrapper<FoodRecipe> query = new QueryWrapper<FoodRecipe>()
                .le("month_age_min", monthAge)
                .ge("month_age_max", monthAge);
        if (allergen != null && !allergen.isBlank()) {
            query.notLike("allergens", allergen);
        }
        return mapper.selectList(query);
    }

    /**
     * 按月龄推荐食谱，并根据宝宝的过敏排除状态屏蔽含已排除食材的食谱。
     * 同时返回被屏蔽的食谱及命中食材，前端展示状态与原因。
     */
    public RecipeRecommendVO recommendForBaby(Long babyId, Integer monthAge) {
        List<FoodRecipe> monthAgeRecipes = mapper.selectList(new QueryWrapper<FoodRecipe>()
                .le("month_age_min", monthAge)
                .ge("month_age_max", monthAge)
                .orderByAsc("id"));

        List<String> excluded = ingredientStatusService.listExcludedNames(babyId);
        Set<String> excludedKeys = new LinkedHashSet<>();
        for (String name : excluded) {
            excludedKeys.add(IngredientNames.matchKey(name));
        }

        List<FoodRecipe> allowed = new ArrayList<>();
        List<BlockedRecipeVO> blocked = new ArrayList<>();
        for (FoodRecipe recipe : monthAgeRecipes) {
            List<String> hits = hitExcluded(recipe, excludedKeys);
            if (hits.isEmpty()) {
                allowed.add(recipe);
            } else {
                BlockedRecipeVO vo = new BlockedRecipeVO();
                vo.setId(recipe.getId());
                vo.setName(recipe.getName());
                vo.setHitIngredients(hits);
                blocked.add(vo);
            }
        }

        RecipeRecommendVO result = new RecipeRecommendVO();
        result.setRecipes(allowed);
        result.setExcludedIngredients(excluded);
        result.setBlockedRecipes(blocked);
        return result;
    }

    private List<String> hitExcluded(FoodRecipe recipe, Set<String> excludedKeys) {
        if (excludedKeys.isEmpty()) {
            return List.of();
        }
        Set<String> hit = new LinkedHashSet<>();
        for (String token : IngredientNames.split(recipe.getIngredients())) {
            if (excludedKeys.contains(IngredientNames.matchKey(token))) {
                hit.add(token);
            }
        }
        // 过敏原字段同样参与屏蔽（如“花生”“虾”）
        for (String token : IngredientNames.split(recipe.getAllergens())) {
            String key = IngredientNames.matchKey(token);
            if (excludedKeys.contains(key)) {
                hit.add(token);
            }
        }
        return List.copyOf(hit);
    }
}
