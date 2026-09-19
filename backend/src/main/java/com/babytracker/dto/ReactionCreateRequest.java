package com.babytracker.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReactionCreateRequest {
    @NotNull(message = "宝宝ID不能为空")
    private Long babyId;
    @NotNull(message = "反应类型不能为空")
    private String reactionType;
    /** 观察时间，缺省为服务端当前时间；必须在餐后72小时以内 */
    private LocalDateTime observedAt;
    /** POSITIVE 时建议填写症状描述，如皮疹、呕吐、腹泻 */
    private String symptoms;
    private String note;
}
