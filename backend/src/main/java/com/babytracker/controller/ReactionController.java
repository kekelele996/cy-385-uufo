package com.babytracker.controller;

import com.babytracker.dto.ReactionCreateRequest;
import com.babytracker.dto.ReactionRevokeRequest;
import com.babytracker.service.ReactionService;
import com.babytracker.vo.ReactionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedings/{feedingId}/reactions")
public class ReactionController {

    private final ReactionService service;

    public ReactionController(ReactionService service) {
        this.service = service;
    }

    /**
     * 餐后72小时内登记反应。同一餐重复/并发提交幂等，只生成一条有效反应；
     * 反应、牵连食材、排除状态同一事务生效。
     */
    @PostMapping
    public ReactionVO register(@PathVariable("feedingId") Long feedingId,
                               @Valid @RequestBody ReactionCreateRequest request) {
        return service.register(feedingId, request);
    }

    /** 撤销误报：仅恢复不再被其他有效反应牵连的食材 */
    @PostMapping("/{reactionId}/revoke")
    public ReactionVO revoke(@PathVariable("feedingId") Long feedingId,
                             @PathVariable("reactionId") Long reactionId,
                             @Valid @RequestBody ReactionRevokeRequest request) {
        return service.revoke(reactionId, request.getReason());
    }
}
