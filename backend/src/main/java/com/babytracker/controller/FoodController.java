package com.babytracker.controller;

import com.babytracker.entity.FoodRecipe;
import com.babytracker.service.FoodService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService service;
    public FoodController(FoodService service) { this.service = service; }

    /**
     * 按月龄推荐食谱；传 babyId 时自动屏蔽该宝宝已排除食材的食谱。
     */
    @GetMapping("/recommend")
    public List<FoodRecipe> recommend(@RequestParam Integer monthAge,
                                      @RequestParam(required = false) String allergen,
                                      @RequestParam(required = false) Long babyId) {
        return service.recommend(monthAge, allergen, babyId);
    }
}
