package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.IngredientStatusType;
import com.babytracker.constants.MealType;
import com.babytracker.constants.ReactionType;
import com.babytracker.entity.Feeding;
import com.babytracker.entity.IngredientStatus;
import com.babytracker.entity.MealReaction;
import com.babytracker.entity.ReactionIngredient;
import com.babytracker.mapper.FeedingMapper;
import com.babytracker.mapper.IngredientStatusMapper;
import com.babytracker.mapper.MealReactionMapper;
import com.babytracker.mapper.ReactionIngredientMapper;
import com.babytracker.utils.IngredientNames;
import com.babytracker.vo.IngredientStatusVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 食材排除状态的唯一计算入口：状态完全由“当前所有有效反应”推导，
 * 登记、撤销均在同一事务内重算，保证刷新后一致。
 */
@Service
public class IngredientStatusService {

    private static final Logger log = LoggerFactory.getLogger(IngredientStatusService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MealReactionMapper reactionMapper;
    private final ReactionIngredientMapper reactionIngredientMapper;
    private final IngredientStatusMapper statusMapper;
    private final FeedingMapper feedingMapper;

    public IngredientStatusService(MealReactionMapper reactionMapper,
                                   ReactionIngredientMapper reactionIngredientMapper,
                                   IngredientStatusMapper statusMapper,
                                   FeedingMapper feedingMapper) {
        this.reactionMapper = reactionMapper;
        this.reactionIngredientMapper = reactionIngredientMapper;
        this.statusMapper = statusMapper;
        this.feedingMapper = feedingMapper;
    }

    /**
     * 重新计算并落库指定宝宝的一组食材状态。调用方必须持有宝宝行锁。
     *
     * 规则：
     * 1. 被任意有效 POSITIVE 反应牵连（登记当时未安全验证，implicated=1）→ EXCLUDED；
     * 2. 否则出现在任意有效 NEGATIVE 餐次中 → SAFE；
     * 3. 两种事实都没有 → UNVERIFIED。
     * 已确认安全的食材不会被后来的 POSITIVE 反应排除，因为 POSITIVE 只牵连登记当时“未验证”的食材。
     */
    public void recompute(Long babyId, List<String> ingredientNames) {
        if (ingredientNames == null || ingredientNames.isEmpty()) {
            return;
        }

        List<MealReaction> activeReactions = reactionMapper.selectList(
                new QueryWrapper<MealReaction>().eq("baby_id", babyId).eq("revoked", 0));
        if (activeReactions.isEmpty()) {
            for (String name : ingredientNames) {
                statusMapper.upsertStatus(babyId, name,
                        IngredientStatusType.UNVERIFIED.name(), defaultReason(name));
            }
            log.debug("宝宝{}暂无有效反应，{}个食材回到未验证", babyId, ingredientNames.size());
            return;
        }

        List<Long> reactionIds = activeReactions.stream().map(MealReaction::getId).toList();
        List<ReactionIngredient> links = reactionIngredientMapper.selectList(
                new QueryWrapper<ReactionIngredient>().in("reaction_id", reactionIds));
        Map<Long, List<ReactionIngredient>> linksByReaction = new HashMap<>();
        for (ReactionIngredient link : links) {
            linksByReaction.computeIfAbsent(link.getReactionId(), k -> new ArrayList<>()).add(link);
        }

        for (String name : ingredientNames) {
            List<MealReaction> positiveHits = new ArrayList<>();
            List<MealReaction> negativeHits = new ArrayList<>();
            for (MealReaction reaction : activeReactions) {
                List<ReactionIngredient> reactionLinks =
                        linksByReaction.getOrDefault(reaction.getId(), List.of());
                boolean linked;
                if (ReactionType.POSITIVE.name().equals(reaction.getReactionType())) {
                    // 仅统计登记当时被牵连（implicated=1）的食材，已确认安全的不受影响
                    linked = reactionLinks.stream().anyMatch(link -> Boolean.TRUE.equals(link.getImplicated())
                            && IngredientNames.sameIngredient(link.getIngredientName(), name));
                } else {
                    // 无异常餐次中出现即视为一次安全验证
                    linked = reactionLinks.stream().anyMatch(link ->
                            IngredientNames.sameIngredient(link.getIngredientName(), name));
                }
                if (!linked) {
                    continue;
                }
                if (ReactionType.POSITIVE.name().equals(reaction.getReactionType())) {
                    positiveHits.add(reaction);
                } else {
                    negativeHits.add(reaction);
                }
            }

            IngredientStatusType newStatus;
            String reason;
            if (!positiveHits.isEmpty()) {
                newStatus = IngredientStatusType.EXCLUDED;
                reason = buildExcludedReason(positiveHits);
            } else if (!negativeHits.isEmpty()) {
                newStatus = IngredientStatusType.SAFE;
                reason = buildSafeReason(negativeHits);
            } else {
                newStatus = IngredientStatusType.UNVERIFIED;
                reason = defaultReason(name);
            }
            statusMapper.upsertStatus(babyId, name, newStatus.name(), reason);
        }
        log.info("宝宝{}重算食材状态完成，共{}个", babyId, ingredientNames.size());
    }

    /** 保持状态不变，仅覆盖原因说明（用于撤销误报后补充恢复来源） */
    public void updateReason(Long babyId, String ingredientName, String reason) {
        IngredientStatus current = getOrUnverified(babyId, ingredientName);
        statusMapper.upsertStatus(babyId, ingredientName, current.getStatus(), reason);
    }

    /** 查询宝宝全部食材状态（列表展示状态与原因） */
    public List<IngredientStatusVO> listStatus(Long babyId) {
        List<IngredientStatus> rows = statusMapper.selectList(
                new QueryWrapper<IngredientStatus>()
                        .eq("baby_id", babyId)
                        .orderByDesc("updated_at")
                        .orderByAsc("ingredient_name"));
        List<IngredientStatusVO> result = new ArrayList<>();
        for (IngredientStatus row : rows) {
            result.add(toVO(row));
        }
        return result;
    }

    /** 查询单个食材当前状态，无记录视为未验证 */
    public IngredientStatus getOrUnverified(Long babyId, String ingredientName) {
        IngredientStatus status = statusMapper.selectOne(
                new QueryWrapper<IngredientStatus>()
                        .eq("baby_id", babyId)
                        .eq("ingredient_name", ingredientName)
                        .last("LIMIT 1"));
        if (status != null) {
            return status;
        }
        IngredientStatus empty = new IngredientStatus();
        empty.setBabyId(babyId);
        empty.setIngredientName(ingredientName);
        empty.setStatus(IngredientStatusType.UNVERIFIED.name());
        empty.setReason(defaultReason(ingredientName));
        return empty;
    }

    /** 取宝宝当前所有已排除食材（供食谱屏蔽） */
    public List<String> listExcludedNames(Long babyId) {
        List<IngredientStatus> rows = statusMapper.selectList(
                new QueryWrapper<IngredientStatus>()
                        .eq("baby_id", babyId)
                        .eq("status", IngredientStatusType.EXCLUDED.name()));
        return rows.stream().map(IngredientStatus::getIngredientName).toList();
    }

    public static String defaultReason(String name) {
        return name + "尚未完成安全验证，请在食用后72小时内登记观察结果";
    }

    public static IngredientStatusVO toVO(IngredientStatus row) {
        IngredientStatusVO vo = new IngredientStatusVO();
        vo.setId(row.getId());
        vo.setBabyId(row.getBabyId());
        vo.setIngredientName(row.getIngredientName());
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(labelOf(row.getStatus()));
        vo.setReason(row.getReason());
        return vo;
    }

    public static String labelOf(String status) {
        try {
            return IngredientStatusType.valueOf(status).getLabel();
        } catch (IllegalArgumentException | NullPointerException e) {
            return status;
        }
    }

    private String buildExcludedReason(List<MealReaction> hits) {
        List<String> parts = new ArrayList<>();
        for (MealReaction reaction : hits) {
            Feeding feeding = feedingMapper.selectById(reaction.getFeedingId());
            String mealTime = feeding == null ? "未知餐次"
                    : feeding.getEatenAt().format(FMT)
                            + Optional.ofNullable(mealLabel(feeding.getMealType()))
                                    .map(l -> "（" + l + "）").orElse("");
            String symptom = reaction.getSymptoms() == null || reaction.getSymptoms().isBlank()
                    ? "出现疑似过敏反应" : reaction.getSymptoms();
            parts.add(mealTime + "餐后反应（" + symptom + "）");
        }
        return "被 " + parts.size() + " 条有效反应牵连排除：" + String.join("；", parts)
                + "。如确认为误报，请撤销对应反应";
    }

    private String buildSafeReason(List<MealReaction> hits) {
        MealReaction first = hits.get(0);
        Feeding feeding = feedingMapper.selectById(first.getFeedingId());
        String mealTime = feeding == null ? "一次" : feeding.getEatenAt().format(FMT) + " 的";
        return "已于 " + mealTime + "用餐后观察满72小时无异常，确认为安全食材"
                + (hits.size() > 1 ? "（共" + hits.size() + "次安全验证）" : "");
    }

    private String mealLabel(String mealType) {
        if (mealType == null) {
            return null;
        }
        try {
            return MealType.valueOf(mealType).getLabel();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
