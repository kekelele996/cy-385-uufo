package com.babytracker.vo;

import lombok.Data;

/** 撤销误报结果：revoked=false 表示该反应此前已被撤销（重复撤销幂等返回） */
@Data
public class ReactionRevokeVO {
    private boolean revoked;
    private Long reactionId;
    private String message;
}
