package com.babytracker.controller;

import com.babytracker.entity.FoodRecipe;
import com.babytracker.service.FoodService;
import com.babytracker.vo.RecipeRecommendVO;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService service;
    public FoodController(FoodService service) { this.service = service; }

    /** 旧版推荐：按月龄和过敏原关键字过滤 */
    @GetMapping("/recommend")
    public List<FoodRecipe> recommend(@RequestParam Integer monthAge,
                                      @RequestParam(required = false) String allergen) {
        return service.recommend(monthAge, allergen);
    }

    /**
     * 按月龄推荐，并屏蔽该宝宝已排除食材涉及的食谱；
     * 返回可推荐食谱、已排除食材清单与被屏蔽食谱（含命中食材与原因）。
     */
    @GetMapping("/recommend-safe")
    public RecipeRecommendVO recommendSafe(@RequestParam Long babyId,
                                           @RequestParam Integer monthAge) {
        return service.recommendForBaby(babyId, monthAge);
    }
}
