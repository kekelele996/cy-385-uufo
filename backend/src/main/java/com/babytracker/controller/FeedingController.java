package com.babytracker.controller;

import com.babytracker.dto.MealCreateRequest;
import com.babytracker.entity.FeedingMeal;
import com.babytracker.service.FeedingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 喂养记录：一餐多食材，记录与食材一次生效 */
@RestController
@RequestMapping("/api/feeding/meals")
public class FeedingController {

    private final FeedingService service;

    public FeedingController(FeedingService service) {
        this.service = service;
    }

    @GetMapping
    public List<FeedingMeal> list(@RequestParam(required = false) Long babyId) {
        return service.listMeals(babyId);
    }

    @PostMapping
    public FeedingMeal create(@RequestBody MealCreateRequest request) {
        return service.createMeal(request);
    }
}
