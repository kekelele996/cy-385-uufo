package com.babytracker.dto;

import lombok.Data;

/** 撤销误报请求 */
@Data
public class ReactionRevokeRequest {
    private String reason;
}
