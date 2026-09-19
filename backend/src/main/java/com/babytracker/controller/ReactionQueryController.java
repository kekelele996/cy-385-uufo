package com.babytracker.controller;

import com.babytracker.service.ReactionService;
import com.babytracker.vo.ReactionVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
public class ReactionQueryController {

    private final ReactionService service;

    public ReactionQueryController(ReactionService service) {
        this.service = service;
    }

    /** 反应历史：默认仅有效反应，includeRevoked=true 时包含已撤销的误报审计记录 */
    @GetMapping
    public List<ReactionVO> list(@RequestParam Long babyId,
                                 @RequestParam(defaultValue = "false") boolean includeRevoked) {
        return service.listByBaby(babyId, includeRevoked);
    }
}
