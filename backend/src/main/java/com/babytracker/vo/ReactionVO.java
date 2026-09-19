package com.babytracker.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReactionVO {
    private Long id;
    private Long feedingId;
    private Long babyId;
    private String reactionType;
    private String reactionTypeLabel;
    private LocalDateTime observedAt;
    private String symptoms;
    private String note;
    private Boolean revoked;
    private String revokedReason;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    /** 该反应牵连排除或验证安全的食材 */
    private List<String> ingredients;
}
