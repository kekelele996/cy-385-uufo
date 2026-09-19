package com.babytracker.controller;

import com.babytracker.dto.FeedingCreateRequest;
import com.babytracker.service.FeedingService;
import com.babytracker.vo.FeedingVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedings")
public class FeedingController {

    private final FeedingService service;

    public FeedingController(FeedingService service) {
        this.service = service;
    }

    /** 记录一餐（多个食材），喂养记录与食材明细在同一事务内生效 */
    @PostMapping
    public FeedingVO create(@Valid @RequestBody FeedingCreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<FeedingVO> list(@RequestParam Long babyId,
                                @RequestParam(defaultValue = "50") int limit) {
        return service.listByBaby(babyId, limit);
    }

    @GetMapping("/{id}")
    public FeedingVO detail(@PathVariable("id") Long id,
                            @RequestParam(value = "babyId", required = false) Long babyId) {
        return service.detail(id, babyId);
    }
}
