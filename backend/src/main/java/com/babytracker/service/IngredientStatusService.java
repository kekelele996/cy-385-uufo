package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.AllergyConstants;
import com.babytracker.entity.AllergyReaction;
import com.babytracker.entity.BabyIngredientStatus;
import com.babytracker.entity.FeedingIngredient;
import com.babytracker.entity.FeedingMeal;
import com.babytracker.entity.IngredientStatusLog;
import com.babytracker.mapper.AllergyReactionMapper;
import com.babytracker.mapper.BabyIngredientStatusMapper;
import com.babytracker.mapper.FeedingIngredientMapper;
import com.babytracker.mapper.FeedingMealMapper;
import com.babytracker.mapper.IngredientStatusLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 食材安全状态规则引擎（单一职责：仅负责由“喂养记录 + 有效反应”推导食材状态）。
 *
 * 规则（按用餐时间顺序重放事件）：
 * 1) 餐后满 72 小时登记“无反应”：此前未验证的食材确认 SAFE；已 EXCLUDED 的保持排除；
 * 2) 登记“出现反应”：只排除该餐中“此前未安全验证”的食材；已确认 SAFE 的食材不受影响；
 * 3) 撤销误报后整体重算：只恢复不再被其他有效反应牵连的食材（回到 SAFE 或未验证）。
 *
 * 重算结果与 baby_ingredient_status 投影表比对写回，并对每次变化写状态流水。
 */
@Service
public class IngredientStatusService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeedingMealMapper mealMapper;
    private final FeedingIngredientMapper ingredientMapper;
    private final AllergyReactionMapper reactionMapper;
    private final BabyIngredientStatusMapper statusMapper;
    private final IngredientStatusLogMapper logMapper;

    public IngredientStatusService(FeedingMealMapper mealMapper,
                                   FeedingIngredientMapper ingredientMapper,
                                   AllergyReactionMapper reactionMapper,
                                   BabyIngredientStatusMapper statusMapper,
                                   IngredientStatusLogMapper logMapper) {
        this.mealMapper = mealMapper;
        this.ingredientMapper = ingredientMapper;
        this.reactionMapper = reactionMapper;
        this.statusMapper = statusMapper;
        this.logMapper = logMapper;
    }

    /** 重放过程中的食材中间状态 */
    private static class ReplayState {
        String status;
        String reason;
        /** 当前仍在排除该食材的有效反应（保留最早反应用于展示原因） */
        final Map<Long, FeedingMeal> implicated = new LinkedHashMap<>();
        FeedingMeal safeMeal;
    }

    /**
     * 在调用方事务内重算某宝宝全部食材状态并写回投影表。
     *
     * @param triggerReactionId 触发本次重算的反应 ID（写入流水）
     * @param actionNote        触发说明，如“登记反应#5”/“撤销误报反应#5”
     * @return 重算后的全部食材状态（EXCLUDED 优先、SAFE 在后）
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public List<BabyIngredientStatus> recompute(Long babyId, Long triggerReactionId, String actionNote) {
        // 1. 加载该宝宝全部用餐与食材
        List<FeedingMeal> meals = mealMapper.selectList(
                new QueryWrapper<FeedingMeal>().eq("baby_id", babyId));
        if (meals.isEmpty()) {
            return persist(babyId, new HashMap<>(), triggerReactionId, actionNote);
        }
        List<Long> mealIds = meals.stream().map(FeedingMeal::getId).collect(Collectors.toList());
        Map<Long, List<String>> mealIngredients = new HashMap<>();
        for (FeedingIngredient fi : ingredientMapper.selectList(
                new QueryWrapper<FeedingIngredient>().in("meal_id", mealIds))) {
            mealIngredients.computeIfAbsent(fi.getMealId(), k -> new ArrayList<>()).add(fi.getIngredientName());
        }
        Map<Long, AllergyReaction> activeReactions = new HashMap<>();
        for (AllergyReaction r : reactionMapper.selectList(
                new QueryWrapper<AllergyReaction>().in("meal_id", mealIds).eq("revoked", false))) {
            activeReactions.put(r.getMealId(), r);
        }

        // 2. 按用餐时间、ID 顺序重放
        meals.sort(Comparator.comparing(FeedingMeal::getMealTime).thenComparing(FeedingMeal::getId));
        Map<String, ReplayState> states = new LinkedHashMap<>();
        for (FeedingMeal meal : meals) {
            AllergyReaction reaction = activeReactions.get(meal.getId());
            List<String> ingredients = mealIngredients.getOrDefault(meal.getId(), List.of());
            if (reaction == null) {
                continue;
            }
            boolean positive = Boolean.TRUE.equals(reaction.getReacted());
            for (String ingredient : ingredients) {
                ReplayState state = states.computeIfAbsent(ingredient, k -> new ReplayState());
                if (positive) {
                    // 只排除此前未安全验证的食材；已确认安全的食材不受影响
                    if (!AllergyConstants.STATUS_SAFE.equals(state.status)) {
                        state.status = AllergyConstants.STATUS_EXCLUDED;
                        state.implicated.putIfAbsent(reaction.getId(), meal);
                        state.reason = buildExcludedReason(state);
                    }
                } else {
                    // 满72小时无反应：确认此前未验证的食材安全；已排除的不因后续阴性观察解除
                    if (state.status == null) {
                        state.status = AllergyConstants.STATUS_SAFE;
                        state.safeMeal = meal;
                        state.reason = "餐后满72小时无反应，确认安全（#" + meal.getId()
                                + " " + meal.getMealTime().format(FMT) + "）";
                    }
                }
            }
        }

        // 3. 与投影表比对写回
        return persist(babyId, states, triggerReactionId, actionNote);
    }

    private String buildExcludedReason(ReplayState state) {
        Map.Entry<Long, FeedingMeal> first = state.implicated.entrySet().iterator().next();
        StringBuilder sb = new StringBuilder();
        sb.append("在 #").append(first.getValue().getId()).append(" 餐后过敏反应中排除（")
          .append(first.getValue().getMealTime().format(FMT)).append("）");
        if (state.implicated.size() > 1) {
            sb.append("，另被 ").append(state.implicated.size() - 1).append(" 条有效反应牵连");
        }
        return sb.toString();
    }

    private List<BabyIngredientStatus> persist(Long babyId, Map<String, ReplayState> computed,
                                               Long triggerReactionId, String actionNote) {
        Map<String, BabyIngredientStatus> existing = statusMapper.selectList(
                        new QueryWrapper<BabyIngredientStatus>().eq("baby_id", babyId))
                .stream().collect(Collectors.toMap(BabyIngredientStatus::getIngredientName, s -> s,
                        (a, b) -> a, LinkedHashMap::new));

        java.time.LocalDateTime now = java.time.LocalDateTime.now()
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        // 删除：投影中存在、重算后已无状态（撤销后恢复为“未验证”）
        for (BabyIngredientStatus row : new ArrayList<>(existing.values())) {
            if (!computed.containsKey(row.getIngredientName())) {
                statusMapper.deleteById(row.getId());
                writeLog(babyId, row.getIngredientName(), row.getStatus(), null,
                        actionNote + "，该食材已不被任何有效反应牵连，恢复为未验证", triggerReactionId);
            }
        }

        // 新增 / 更新
        for (Map.Entry<String, ReplayState> entry : computed.entrySet()) {
            ReplayState target = entry.getValue();
            BabyIngredientStatus row = existing.get(entry.getKey());
            if (row == null) {
                BabyIngredientStatus created = new BabyIngredientStatus();
                created.setBabyId(babyId);
                created.setIngredientName(entry.getKey());
                created.setStatus(target.status);
                created.setReason(target.reason);
                created.setFirstConfirmedAt(now);
                created.setUpdatedAt(now);
                statusMapper.insert(created);
                writeLog(babyId, entry.getKey(), null, target.status, target.reason, triggerReactionId);
            } else if (!row.getStatus().equals(target.status) || !row.getReason().equals(target.reason)) {
                String oldStatus = row.getStatus();
                row.setStatus(target.status);
                row.setReason(target.reason);
                row.setUpdatedAt(now);
                statusMapper.updateById(row);
                String logReason = oldStatus.equals(target.status)
                        ? target.reason
                        : actionNote + "；" + target.reason;
                writeLog(babyId, entry.getKey(), oldStatus, target.status, logReason, triggerReactionId);
            }
        }

        return listStatuses(babyId);
    }

    private void writeLog(Long babyId, String ingredient, String oldStatus, String newStatus,
                          String reason, Long reactionId) {
        IngredientStatusLog log = new IngredientStatusLog();
        log.setBabyId(babyId);
        log.setIngredientName(ingredient);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setReason(reason);
        log.setRelatedReactionId(reactionId);
        logMapper.insert(log);
    }

    /** 查询宝宝全部食材状态（EXCLUDED 优先、SAFE 在后，名称排序） */
    public List<BabyIngredientStatus> listStatuses(Long babyId) {
        // 字母序 EXCLUDED < SAFE，升序即“已排除”在前
        return statusMapper.selectList(new QueryWrapper<BabyIngredientStatus>()
                .eq("baby_id", babyId)
                .orderByAsc("status")
                .orderByAsc("ingredient_name"));
    }

    /** 已排除食材名称集合，供食谱推荐屏蔽 */
    public List<String> listExcludedNames(Long babyId) {
        return statusMapper.selectList(new QueryWrapper<BabyIngredientStatus>()
                        .eq("baby_id", babyId)
                        .eq("status", AllergyConstants.STATUS_EXCLUDED))
                .stream().map(BabyIngredientStatus::getIngredientName).collect(Collectors.toList());
    }
}
