package com.babytracker.controller;

import com.babytracker.service.IngredientStatusService;
import com.babytracker.vo.IngredientStatusVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 食材过敏排除状态查询：展示状态（未验证/已安全/已排除）与原因 */
@RestController
@RequestMapping("/api/ingredients/status")
public class IngredientStatusController {

    private final IngredientStatusService service;

    public IngredientStatusController(IngredientStatusService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientStatusVO> list(@RequestParam Long babyId) {
        return service.listStatus(babyId);
    }
}
