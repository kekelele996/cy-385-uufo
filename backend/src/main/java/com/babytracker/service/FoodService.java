package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.entity.FoodRecipe;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.FoodMapper;
import com.babytracker.utils.IngredientNameUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class FoodService {

    private final FoodMapper mapper;
    private final IngredientStatusService statusService;

    public FoodService(FoodMapper mapper, IngredientStatusService statusService) {
        this.mapper = mapper;
        this.statusService = statusService;
    }

    /**
     * 按月龄推荐辅食。
     *
     * @param babyId 指定宝宝时，自动屏蔽该宝宝已排除食材的食谱（过敏排除闭环）
     * @param allergen 额外的过敏原过滤（保留原有的按过敏原筛选能力）
     */
    public List<FoodRecipe> recommend(Integer monthAge, String allergen, Long babyId) {
        if (monthAge == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "请提供月龄");
        }
        QueryWrapper<FoodRecipe> query = new QueryWrapper<FoodRecipe>()
                .le("month_age_min", monthAge)
                .ge("month_age_max", monthAge);
        if (allergen != null && !allergen.isBlank()) {
            query.notLike("allergens", allergen);
        }
        List<FoodRecipe> recipes = mapper.selectList(query);
        if (babyId == null) {
            return recipes;
        }
        List<String> excluded = statusService.listExcludedNames(babyId);
        if (excluded.isEmpty()) {
            return recipes;
        }
        return recipes.stream().filter(recipe -> !containsExcluded(recipe, excluded)).toList();
    }

    /** 食材清单中命中任一已排除食材则屏蔽该食谱（名称规范化后包含匹配） */
    private boolean containsExcluded(FoodRecipe recipe, List<String> excluded) {
        String ingredients = recipe.getIngredients() == null ? "" : recipe.getIngredients().toLowerCase(Locale.ROOT);
        for (String name : excluded) {
            String target = IngredientNameUtils.normalize(name).toLowerCase(Locale.ROOT);
            if (!target.isEmpty() && ingredients.contains(target)) {
                return true;
            }
        }
        return false;
    }
}
