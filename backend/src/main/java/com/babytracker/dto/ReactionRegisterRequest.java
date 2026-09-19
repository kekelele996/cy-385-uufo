package com.babytracker.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 餐后 72 小时反应登记请求。
 * reacted=true 表示出现过敏反应；reacted=false 表示观察满 72 小时无异常。
 */
@Data
public class ReactionRegisterRequest {
    private Boolean reacted;
    private String symptoms;
    private LocalDateTime reactedAt;
}
