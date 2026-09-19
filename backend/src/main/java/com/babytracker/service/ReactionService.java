package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.AllergyErrorCode;
import com.babytracker.constants.IngredientStatusType;
import com.babytracker.constants.ReactionType;
import com.babytracker.dto.ReactionCreateRequest;
import com.babytracker.entity.Baby;
import com.babytracker.entity.Feeding;
import com.babytracker.entity.FeedingIngredient;
import com.babytracker.entity.IngredientStatus;
import com.babytracker.entity.MealReaction;
import com.babytracker.entity.ReactionIngredient;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.FeedingIngredientMapper;
import com.babytracker.mapper.FeedingMapper;
import com.babytracker.mapper.MealReactionMapper;
import com.babytracker.mapper.ReactionIngredientMapper;
import com.babytracker.vo.ReactionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactionService.class);

    /** 餐后观察窗口：72 小时 */
    public static final long OBSERVE_WINDOW_HOURS = 72;

    private final MealReactionMapper reactionMapper;
    private final ReactionIngredientMapper reactionIngredientMapper;
    private final FeedingMapper feedingMapper;
    private final FeedingIngredientMapper feedingIngredientMapper;
    private final BabyMapper babyMapper;
    private final IngredientStatusService ingredientStatusService;

    public ReactionService(MealReactionMapper reactionMapper,
                           ReactionIngredientMapper reactionIngredientMapper,
                           FeedingMapper feedingMapper,
                           FeedingIngredientMapper feedingIngredientMapper,
                           BabyMapper babyMapper,
                           IngredientStatusService ingredientStatusService) {
        this.reactionMapper = reactionMapper;
        this.reactionIngredientMapper = reactionIngredientMapper;
        this.feedingMapper = feedingMapper;
        this.feedingIngredientMapper = feedingIngredientMapper;
        this.babyMapper = babyMapper;
        this.ingredientStatusService = ingredientStatusService;
    }

    /**
     * 登记餐后 72 小时反应。反应、牵连食材、食材排除状态在一个事务内生效，任一步失败全部回滚。
     *
     * 幂等：同一餐重复或并发提交只生成一条有效反应——
     * 先锁宝宝行（串行化状态重算），再锁餐次行；已存在有效反应时原样返回，不重复生成。
     * 数据库 uk_reaction_active_feeding 唯一约束是最后防线。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReactionVO register(Long feedingId, ReactionCreateRequest request) {
        ReactionType reactionType = parseReactionType(request.getReactionType());
        LocalDateTime observedAt = request.getObservedAt() == null ? LocalDateTime.now() : request.getObservedAt();
        LocalDateTime now = LocalDateTime.now();
        if (observedAt.isAfter(now.plusMinutes(1))) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "观察时间不能晚于当前时间");
        }

        Baby baby = babyMapper.selectByIdForUpdate(request.getBabyId());
        if (baby == null) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "宝宝不存在：" + request.getBabyId());
        }
        Feeding feeding = feedingMapper.selectByIdForUpdate(feedingId);
        if (feeding == null) {
            throw new BizException(AllergyErrorCode.FEEDING_NOT_FOUND, "喂养记录不存在：" + feedingId);
        }
        if (!feeding.getBabyId().equals(baby.getId())) {
            throw new BizException(AllergyErrorCode.BABY_MISMATCH, "该喂养记录不属于当前宝宝");
        }

        MealReaction existing = findActiveReaction(feedingId);
        if (existing != null) {
            if (!existing.getBabyId().equals(baby.getId())) {
                throw new BizException(AllergyErrorCode.BABY_MISMATCH, "该喂养记录不属于当前宝宝");
            }
            // 重复提交：幂等返回同一条有效反应，刷新后看到的结果一致
            log.info("餐次{}已存在有效反应{}，重复提交直接返回", feedingId, existing.getId());
            return toReactionVO(existing, loadLinkNames(existing.getId()));
        }

        long elapsedHours = Duration.between(feeding.getEatenAt(), observedAt).toHours();
        if (observedAt.isBefore(feeding.getEatenAt().minusMinutes(1))) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "观察时间不能早于用餐时间");
        }
        if (Duration.between(feeding.getEatenAt(), observedAt).compareTo(Duration.ofHours(OBSERVE_WINDOW_HOURS)) > 0) {
            throw new BizException(AllergyErrorCode.REACTION_WINDOW_EXPIRED,
                    "已超过餐后" + OBSERVE_WINDOW_HOURS + "小时观察窗口（距用餐约" + elapsedHours + "小时），无法登记反应");
        }
        if (reactionType == ReactionType.POSITIVE
                && (request.getSymptoms() == null || request.getSymptoms().isBlank())) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "登记有反应时请填写症状表现");
        }

        MealReaction reaction = new MealReaction();
        reaction.setFeedingId(feeding.getId());
        reaction.setBabyId(baby.getId());
        reaction.setReactionType(reactionType.name());
        reaction.setObservedAt(observedAt);
        reaction.setSymptoms(request.getSymptoms());
        reaction.setNote(request.getNote());
        reaction.setRevoked(false);
        reactionMapper.insert(reaction);

        List<FeedingIngredient> mealIngredients = feedingIngredientMapper.selectList(
                new QueryWrapper<FeedingIngredient>()
                        .eq("feeding_id", feedingId)
                        .orderByAsc("id"));

        List<String> affectedNames = new ArrayList<>();
        for (FeedingIngredient ingredient : mealIngredients) {
            String name = ingredient.getName();
            ReactionIngredient link = new ReactionIngredient();
            link.setReactionId(reaction.getId());
            link.setIngredientName(name);
            if (reactionType == ReactionType.POSITIVE) {
                // 只牵连该餐中“未完成安全验证”的食材（未验证或已被其他反应排除）；
                // 已确认安全的食材不受影响。已排除食材继续留证：旧反应被撤销时本反应仍可支撑排除
                IngredientStatus current = ingredientStatusService.getOrUnverified(baby.getId(), name);
                link.setImplicated(!IngredientStatusType.SAFE.name().equals(current.getStatus()));
            } else {
                // NEGATIVE：留存本次无异常的验证快照；已排除食材是否翻案由撤销流程决定
                link.setImplicated(false);
            }
            reactionIngredientMapper.insert(link);
            affectedNames.add(name);
        }

        // 依据“全部有效反应”事实重算本餐食材状态并落库
        ingredientStatusService.recompute(baby.getId(),
                mealIngredients.stream().map(FeedingIngredient::getName).toList());

        log.info("宝宝{}餐次{}登记{}反应{}，影响食材{}个",
                baby.getId(), feedingId, reactionType.getLabel(), reaction.getId(), affectedNames.size());
        return toReactionVO(reaction, affectedNames);
    }

    /**
     * 撤销误报反应：只恢复不再被其他有效反应牵连的食材，并重新计算状态与原因；
     * 反应记录保留为已撤销审计状态。反应、食材状态一次生效，失败全部回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReactionVO revoke(Long reactionId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "撤销原因不能为空");
        }
        MealReaction reaction = reactionMapper.selectById(reactionId);
        if (reaction == null) {
            throw new BizException(AllergyErrorCode.REACTION_NOT_FOUND, "反应记录不存在：" + reactionId);
        }
        if (Boolean.TRUE.equals(reaction.getRevoked())) {
            throw new BizException(AllergyErrorCode.REACTION_ALREADY_REVOKED, "该反应已撤销，不能重复撤销");
        }

        // 与登记相同的加锁顺序：先宝宝后餐次
        Baby baby = babyMapper.selectByIdForUpdate(reaction.getBabyId());
        Feeding feeding = feedingMapper.selectByIdForUpdate(reaction.getFeedingId());

        List<String> touchedNames = loadLinkNames(reactionId);

        // CAS 抢占：并发撤销时只有一个请求能把 revoked 0→1，其余按重复撤销拒绝
        int claimed = reactionMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<MealReaction>()
                        .eq("id", reactionId)
                        .eq("revoked", 0)
                        .set("revoked", 1)
                        .set("revoked_reason", reason)
                        .set("revoked_at", LocalDateTime.now()));
        if (claimed == 0) {
            throw new BizException(AllergyErrorCode.REACTION_ALREADY_REVOKED, "该反应已撤销，不能重复撤销");
        }
        reaction.setRevoked(true);
        reaction.setRevokedReason(reason);
        reaction.setRevokedAt(LocalDateTime.now());

        // 重算：仍被其他有效 POSITIVE 牵连的食材保持排除，其余恢复到安全/未验证
        ingredientStatusService.recompute(baby.getId(), touchedNames);

        // 对恢复的食材补充撤销来源说明，覆盖“未验证/安全”的默认原因，便于界面展示
        appendRevocationContext(reaction, touchedNames, reason, baby.getId());

        log.info("反应{}被标记为误报撤销，原因：{}，重算食材{}个", reactionId, reason, touchedNames.size());
        return toReactionVO(reaction, touchedNames);
    }

    @Transactional(readOnly = true)
    public List<ReactionVO> listByBaby(Long babyId, boolean includeRevoked) {
        QueryWrapper<MealReaction> query = new QueryWrapper<MealReaction>()
                .eq("baby_id", babyId)
                .orderByDesc("observed_at");
        if (!includeRevoked) {
            query.eq("revoked", 0);
        }
        List<MealReaction> reactions = reactionMapper.selectList(query);
        List<ReactionVO> result = new ArrayList<>();
        for (MealReaction reaction : reactions) {
            result.add(toReactionVO(reaction, loadLinkNames(reaction.getId())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ReactionVO detail(Long reactionId) {
        MealReaction reaction = reactionMapper.selectById(reactionId);
        if (reaction == null) {
            throw new BizException(AllergyErrorCode.REACTION_NOT_FOUND, "反应记录不存在：" + reactionId);
        }
        return toReactionVO(reaction, loadLinkNames(reactionId));
    }

    private void appendRevocationContext(MealReaction revokedReaction, List<String> names,
                                         String revokeReason, Long babyId) {
        for (String name : names) {
            IngredientStatus status = ingredientStatusService.getOrUnverified(babyId, name);
            if (IngredientStatusType.EXCLUDED.name().equals(status.getStatus())) {
                continue; // 仍被其他有效反应牵连，保持排除与原有原因
            }
            String prefix;
            if (IngredientStatusType.SAFE.name().equals(status.getStatus())) {
                prefix = "误报反应已撤销（" + revokeReason + "），食材仍为已确认安全。";
            } else {
                prefix = "原排除依据反应#" + revokedReaction.getId() + "已撤销为误报（" + revokeReason
                        + "），且无其他有效反应牵连，恢复为未验证。";
            }
            ingredientStatusService.updateReason(babyId, name, prefix + status.getReason());
        }
    }

    private MealReaction findActiveReaction(Long feedingId) {
        return reactionMapper.selectOne(new QueryWrapper<MealReaction>()
                .eq("feeding_id", feedingId)
                .eq("revoked", 0)
                .last("LIMIT 1"));
    }

    private List<String> loadLinkNames(Long reactionId) {
        return reactionIngredientMapper.selectList(
                        new QueryWrapper<ReactionIngredient>()
                                .eq("reaction_id", reactionId)
                                .orderByAsc("id"))
                .stream().map(ReactionIngredient::getIngredientName).toList();
    }

    private ReactionType parseReactionType(String raw) {
        if (raw == null) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "反应类型不能为空");
        }
        try {
            return ReactionType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "不支持的反应类型：" + raw);
        }
    }

    static ReactionVO toReactionVO(MealReaction reaction, List<String> ingredientNames) {
        ReactionVO vo = new ReactionVO();
        vo.setId(reaction.getId());
        vo.setFeedingId(reaction.getFeedingId());
        vo.setBabyId(reaction.getBabyId());
        vo.setReactionType(reaction.getReactionType());
        try {
            vo.setReactionTypeLabel(ReactionType.valueOf(reaction.getReactionType()).getLabel());
        } catch (IllegalArgumentException e) {
            vo.setReactionTypeLabel(reaction.getReactionType());
        }
        vo.setObservedAt(reaction.getObservedAt());
        vo.setSymptoms(reaction.getSymptoms());
        vo.setNote(reaction.getNote());
        vo.setRevoked(reaction.getRevoked());
        vo.setRevokedReason(reaction.getRevokedReason());
        vo.setCreatedAt(reaction.getCreatedAt());
        vo.setRevokedAt(reaction.getRevokedAt());
        vo.setIngredients(ingredientNames);
        return vo;
    }
}
