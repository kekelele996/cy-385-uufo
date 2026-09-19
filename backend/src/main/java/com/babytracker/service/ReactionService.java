package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.AllergyConstants;
import com.babytracker.constants.ErrorCode;
import com.babytracker.dto.ReactionRegisterRequest;
import com.babytracker.entity.AllergyReaction;
import com.babytracker.entity.BabyIngredientStatus;
import com.babytracker.entity.FeedingMeal;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.AllergyReactionMapper;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.FeedingMealMapper;
import com.babytracker.vo.ReactionResultVO;
import com.babytracker.vo.ReactionRevokeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 餐后反应登记与撤销。
 *
 * 一致性保证：
 * - “插入反应 + 重算食材排除状态 + 写状态流水”在同一事务内提交，任一步失败全部回滚；
 * - uk_reaction_active_meal 唯一索引在数据库层保证同一餐只有一条有效反应，
 *   重复/并发提交只生成一条，先到请求生效，后到请求幂等返回既有反应；
 * - 对宝宝行加锁（SELECT ... FOR UPDATE）串行化同宝宝的登记与撤销，刷新后结果一致。
 */
@Service
public class ReactionService {

    private static final Logger log = LoggerFactory.getLogger(ReactionService.class);

    private final AllergyReactionMapper reactionMapper;
    private final FeedingMealMapper mealMapper;
    private final BabyMapper babyMapper;
    private final IngredientStatusService statusService;
    private final FeedingService feedingService;
    /** 自注入代理，保证 @Transactional 方法经代理调用（避免同类自调用事务失效） */
    private final ReactionService self;

    public ReactionService(AllergyReactionMapper reactionMapper,
                           FeedingMealMapper mealMapper,
                           BabyMapper babyMapper,
                           IngredientStatusService statusService,
                           FeedingService feedingService,
                           @Lazy ReactionService self) {
        this.reactionMapper = reactionMapper;
        this.mealMapper = mealMapper;
        this.babyMapper = babyMapper;
        this.statusService = statusService;
        this.feedingService = feedingService;
        this.self = self;
    }

    /** 登记餐后反应（餐后 72 小时内） */
    public ReactionResultVO register(Long mealId, ReactionRegisterRequest request) {
        if (request == null || request.getReacted() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "请选择是否出现过敏反应");
        }
        FeedingMeal meal = feedingService.requireMeal(mealId);
        if (feedingService.ingredientsOf(mealId).isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "该餐没有食材记录，无法登记反应");
        }
        LocalDateTime reactedAt = request.getReactedAt() != null ? request.getReactedAt() : LocalDateTime.now();
        LocalDateTime windowEnd = meal.getMealTime().plusHours(AllergyConstants.REACTION_WINDOW_HOURS);
        if (reactedAt.isBefore(meal.getMealTime()) || reactedAt.isAfter(windowEnd)) {
            throw new BizException(ErrorCode.VALIDATION_FAILED,
                    "反应需在餐后 72 小时内登记（" + meal.getMealTime() + " 至 " + windowEnd + "）");
        }

        // 幂等快路径：同一餐已有有效反应时直接返回，不重复生成
        AllergyReaction existing = findActiveByMeal(mealId);
        if (existing != null) {
            log.info("餐 #{} 已存在有效反应 #{}，重复提交幂等返回", mealId, existing.getId());
            return buildResult(true, existing, statusService.listStatuses(meal.getBabyId()));
        }

        try {
            return self.doRegister(meal, request.getReacted(), request.getSymptoms(), reactedAt);
        } catch (DuplicateKeyException e) {
            // 并发提交：唯一索引拦截，只保留一条有效反应，返回先到者。
            // 在独立新事务中读取，避免当前事务快照看不到对方已提交的行。
            log.warn("餐 #{} 反应登记并发冲突，返回既有有效反应", mealId);
            AllergyReaction winner = self.findActiveByMealFresh(mealId);
            if (winner == null) {
                throw new BizException(ErrorCode.CONFLICT, "反应登记冲突，请刷新后重试");
            }
            return buildResult(true, winner, statusService.listStatuses(meal.getBabyId()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ReactionResultVO doRegister(FeedingMeal meal, boolean reacted, String symptoms, LocalDateTime reactedAt) {
        // 行锁串行化同宝宝的登记/撤销
        babyMapper.selectIdForUpdate(meal.getBabyId());
        AllergyReaction existing = findActiveByMeal(meal.getId());
        if (existing != null) {
            return buildResult(true, existing, statusService.listStatuses(meal.getBabyId()));
        }
        AllergyReaction reaction = new AllergyReaction();
        reaction.setMealId(meal.getId());
        reaction.setReacted(reacted);
        reaction.setSymptoms(reacted ? trimToNull(symptoms) : null);
        reaction.setReactedAt(reactedAt);
        reaction.setRevoked(false);
        reactionMapper.insert(reaction);

        // 同一事务内重算排除状态：只排除此前未安全验证的食材
        List<BabyIngredientStatus> statuses = statusService.recompute(
                meal.getBabyId(), reaction.getId(), "登记反应 #" + reaction.getId());
        log.info("餐 #{} 登记{}反应 #{}，宝宝 #{} 食材状态已重算",
                meal.getId(), reacted ? "阳性" : "阴性（满72小时无反应）", reaction.getId(), meal.getBabyId());
        return buildResult(false, reaction, statuses);
    }

    /** 撤销误报：只恢复不再被其他有效反应牵连的食材 */
    @Transactional(rollbackFor = Exception.class)
    public ReactionRevokeVO revoke(Long reactionId, String reason) {
        AllergyReaction reaction = reactionMapper.selectById(reactionId);
        if (reaction == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "反应记录不存在");
        }
        FeedingMeal meal = mealMapper.selectById(reaction.getMealId());
        if (meal == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "关联喂养记录不存在");
        }
        // 与登记操作互斥，保证撤销后刷新看到的状态即重算后的一致状态
        babyMapper.selectIdForUpdate(meal.getBabyId());

        reaction = reactionMapper.selectById(reactionId);
        ReactionRevokeVO vo = new ReactionRevokeVO();
        vo.setReactionId(reactionId);
        if (Boolean.TRUE.equals(reaction.getRevoked())) {
            // 重复撤销幂等：不再次触发恢复
            vo.setRevoked(false);
            vo.setMessage("该反应此前已撤销，无需重复操作");
            return vo;
        }

        reaction.setRevoked(true);
        reaction.setRevokedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        reaction.setRevokeReason(trimToNull(reason) != null ? reason.trim() : "误报撤销");
        reactionMapper.updateById(reaction);

        statusService.recompute(meal.getBabyId(), reactionId, "撤销误报反应 #" + reactionId);
        log.info("反应 #{} 已标记为误报撤销，宝宝 #{} 食材状态已重算", reactionId, meal.getBabyId());
        vo.setRevoked(true);
        vo.setMessage("已撤销，仅恢复不被其他有效反应牵连的食材");
        return vo;
    }

    private AllergyReaction findActiveByMeal(Long mealId) {
        return reactionMapper.selectOne(new QueryWrapper<AllergyReaction>()
                .eq("meal_id", mealId)
                .eq("revoked", false)
                .last("LIMIT 1"));
    }

    /** 在独立新事务中读取，供唯一索引冲突后看到并发请求已提交的反应 */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, readOnly = true)
    public AllergyReaction findActiveByMealFresh(Long mealId) {
        return findActiveByMeal(mealId);
    }

    private ReactionResultVO buildResult(boolean duplicated, AllergyReaction reaction,
                                         List<BabyIngredientStatus> statuses) {
        ReactionResultVO vo = new ReactionResultVO();
        vo.setDuplicated(duplicated);
        vo.setReaction(reaction);
        vo.setStatuses(statuses);
        return vo;
    }

    private String trimToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
