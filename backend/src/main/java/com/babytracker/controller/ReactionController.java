package com.babytracker.controller;

import com.babytracker.dto.ReactionRegisterRequest;
import com.babytracker.dto.ReactionRevokeRequest;
import com.babytracker.service.ReactionService;
import com.babytracker.vo.ReactionResultVO;
import com.babytracker.vo.ReactionRevokeVO;
import org.springframework.web.bind.annotation.*;

/** 餐后 72 小时反应登记与误报撤销 */
@RestController
@RequestMapping("/api/reactions")
public class ReactionController {

    private final ReactionService service;

    public ReactionController(ReactionService service) {
        this.service = service;
    }

    /** 为某一餐登记反应；重复/并发提交幂等返回同一条有效反应 */
    @PostMapping("/meal/{mealId}")
    public ReactionResultVO register(@PathVariable Long mealId, @RequestBody ReactionRegisterRequest request) {
        return service.register(mealId, request);
    }

    /** 撤销误报：只恢复不再被其他有效反应牵连的食材 */
    @PostMapping("/{reactionId}/revoke")
    public ReactionRevokeVO revoke(@PathVariable Long reactionId,
                                   @RequestBody(required = false) ReactionRevokeRequest request) {
        String reason = request == null ? null : request.getReason();
        return service.revoke(reactionId, reason);
    }
}
