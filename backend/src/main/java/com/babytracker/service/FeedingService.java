package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.AllergyErrorCode;
import com.babytracker.constants.MealType;
import com.babytracker.dto.FeedingCreateRequest;
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
import com.babytracker.utils.IngredientNames;
import com.babytracker.vo.FeedingIngredientVO;
import com.babytracker.vo.FeedingVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedingService {

    private static final Logger log = LoggerFactory.getLogger(FeedingService.class);

    private final FeedingMapper feedingMapper;
    private final FeedingIngredientMapper ingredientMapper;
    private final MealReactionMapper reactionMapper;
    private final ReactionIngredientMapper reactionIngredientMapper;
    private final BabyMapper babyMapper;
    private final IngredientStatusService ingredientStatusService;

    public FeedingService(FeedingMapper feedingMapper,
                          FeedingIngredientMapper ingredientMapper,
                          MealReactionMapper reactionMapper,
                          ReactionIngredientMapper reactionIngredientMapper,
                          BabyMapper babyMapper,
                          IngredientStatusService ingredientStatusService) {
        this.feedingMapper = feedingMapper;
        this.ingredientMapper = ingredientMapper;
        this.reactionMapper = reactionMapper;
        this.reactionIngredientMapper = reactionIngredientMapper;
        this.babyMapper = babyMapper;
        this.ingredientStatusService = ingredientStatusService;
    }

    /**
     * 记录一餐：喂养记录与食材明细一次生效，任一步失败全部回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public FeedingVO create(FeedingCreateRequest request) {
        validateMealType(request.getMealType());
        List<String> ingredients = IngredientNames.normalizeBatch(request.getIngredients());
        if (ingredients.isEmpty()) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "每餐至少记录一个有效食材");
        }
        if (request.getEatenAt() == null || request.getEatenAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "喂养时间不能为空且不能晚于当前时间");
        }
        Baby baby = babyMapper.selectById(request.getBabyId());
        if (baby == null) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "宝宝不存在：" + request.getBabyId());
        }

        Feeding feeding = new Feeding();
        feeding.setBabyId(baby.getId());
        feeding.setMealType(request.getMealType());
        feeding.setEatenAt(request.getEatenAt());
        feeding.setNote(request.getNote());
        feedingMapper.insert(feeding);

        for (String name : ingredients) {
            FeedingIngredient row = new FeedingIngredient();
            row.setFeedingId(feeding.getId());
            row.setName(name);
            ingredientMapper.insert(row);
        }
        log.info("宝宝{}记录喂养餐次{}，食材{}个", baby.getId(), feeding.getId(), ingredients.size());
        return toVO(feeding);
    }

    /** 按时间倒序查询宝宝的喂养记录（含食材当前状态与有效反应） */
    @Transactional(readOnly = true)
    public List<FeedingVO> listByBaby(Long babyId, int limit) {
        List<Feeding> feedings = feedingMapper.selectList(
                new QueryWrapper<Feeding>()
                        .eq("baby_id", babyId)
                        .orderByDesc("eaten_at")
                        .last("LIMIT " + Math.max(1, Math.min(limit, 200))));
        List<FeedingVO> result = new ArrayList<>();
        for (Feeding feeding : feedings) {
            result.add(toVO(feeding));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public FeedingVO detail(Long feedingId, Long babyId) {
        Feeding feeding = feedingMapper.selectById(feedingId);
        if (feeding == null) {
            throw new BizException(AllergyErrorCode.FEEDING_NOT_FOUND, "喂养记录不存在：" + feedingId);
        }
        if (babyId != null && !feeding.getBabyId().equals(babyId)) {
            throw new BizException(AllergyErrorCode.BABY_MISMATCH, "该喂养记录不属于当前宝宝");
        }
        return toVO(feeding);
    }

    FeedingVO toVO(Feeding feeding) {
        FeedingVO vo = new FeedingVO();
        vo.setId(feeding.getId());
        vo.setBabyId(feeding.getBabyId());
        vo.setMealType(feeding.getMealType());
        vo.setMealTypeLabel(mealLabel(feeding.getMealType()));
        vo.setEatenAt(feeding.getEatenAt());
        vo.setNote(feeding.getNote());

        List<FeedingIngredient> rows = ingredientMapper.selectList(
                new QueryWrapper<FeedingIngredient>().eq("feeding_id", feeding.getId()).orderByAsc("id"));
        Map<String, IngredientStatus> statusCache = new HashMap<>();
        List<FeedingIngredientVO> ingredientVOs = new ArrayList<>();
        for (FeedingIngredient row : rows) {
            FeedingIngredientVO ingVO = new FeedingIngredientVO();
            ingVO.setName(row.getName());
            IngredientStatus status = statusCache.computeIfAbsent(
                    row.getName(), n -> ingredientStatusService.getOrUnverified(feeding.getBabyId(), n));
            ingVO.setStatus(status.getStatus());
            ingVO.setStatusLabel(IngredientStatusService.labelOf(status.getStatus()));
            ingVO.setReason(status.getReason());
            ingredientVOs.add(ingVO);
        }
        vo.setIngredients(ingredientVOs);

        MealReaction active = reactionMapper.selectOne(new QueryWrapper<MealReaction>()
                .eq("feeding_id", feeding.getId())
                .eq("revoked", 0)
                .last("LIMIT 1"));
        if (active != null) {
            List<String> names = reactionIngredientMapper.selectList(
                            new QueryWrapper<ReactionIngredient>()
                                    .eq("reaction_id", active.getId())
                                    .orderByAsc("id"))
                    .stream().map(ReactionIngredient::getIngredientName).toList();
            vo.setReaction(ReactionService.toReactionVO(active, names));
        }
        return vo;
    }

    private String mealLabel(String mealType) {
        if (mealType == null) {
            return null;
        }
        try {
            return MealType.valueOf(mealType).getLabel();
        } catch (IllegalArgumentException e) {
            return mealType;
        }
    }

    private void validateMealType(String mealType) {
        if (mealType == null) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "餐次类型不能为空");
        }
        try {
            MealType.valueOf(mealType);
        } catch (IllegalArgumentException e) {
            throw new BizException(AllergyErrorCode.VALIDATION_FAILED, "不支持的餐次类型：" + mealType);
        }
    }
}
