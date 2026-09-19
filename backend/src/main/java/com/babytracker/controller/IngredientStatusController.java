package com.babytracker.controller;

import com.babytracker.entity.BabyIngredientStatus;
import com.babytracker.service.IngredientStatusService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 食材安全/排除状态查询：展示状态与原因 */
@RestController
@RequestMapping("/api/ingredients")
public class IngredientStatusController {

    private final IngredientStatusService service;

    public IngredientStatusController(IngredientStatusService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public List<BabyIngredientStatus> status(@RequestParam Long babyId) {
        return service.listStatuses(babyId);
    }
}
