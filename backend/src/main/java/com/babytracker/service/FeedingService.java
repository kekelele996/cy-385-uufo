package com.babytracker.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.constants.ErrorCode;
import com.babytracker.dto.MealCreateRequest;
import com.babytracker.entity.AllergyReaction;
import com.babytracker.entity.Baby;
import com.babytracker.entity.FeedingIngredient;
import com.babytracker.entity.FeedingMeal;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.AllergyReactionMapper;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.FeedingIngredientMapper;
import com.babytracker.mapper.FeedingMealMapper;
import com.babytracker.utils.IngredientNameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 喂养记录：每餐可记录多个食材，餐与食材在同一事务内一次生效 */
@Service
public class FeedingService {

    private final FeedingMealMapper mealMapper;
    private final FeedingIngredientMapper ingredientMapper;
    private final AllergyReactionMapper reactionMapper;
    private final BabyMapper babyMapper;

    public FeedingService(FeedingMealMapper mealMapper,
                          FeedingIngredientMapper ingredientMapper,
                          AllergyReactionMapper reactionMapper,
                          BabyMapper babyMapper) {
        this.mealMapper = mealMapper;
        this.ingredientMapper = ingredientMapper;
        this.reactionMapper = reactionMapper;
        this.babyMapper = babyMapper;
    }

    /** 创建喂养记录；餐主记录或任一食材写入失败时整体回滚 */
    @Transactional(rollbackFor = Exception.class)
    public FeedingMeal createMeal(MealCreateRequest request) {
        if (request.getBabyId() == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "请选择宝宝");
        }
        Baby baby = babyMapper.selectById(request.getBabyId());
        if (baby == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "宝宝档案不存在");
        }
        List<String> ingredients = IngredientNameUtils.mergeDistinct(
                request.getIngredients(), request.getIngredientsText());
        if (ingredients.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "每餐至少记录一个食材");
        }
        LocalDateTime mealTime = request.getMealTime() != null
                ? request.getMealTime()
                : LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        if (mealTime.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "用餐时间不能晚于当前时间");
        }

        FeedingMeal meal = new FeedingMeal();
        meal.setBabyId(request.getBabyId());
        meal.setMealType(request.getMealType() == null || request.getMealType().isBlank()
                ? "辅食" : request.getMealType().trim());
        meal.setMealTime(mealTime);
        mealMapper.insert(meal);

        for (String name : ingredients) {
            FeedingIngredient fi = new FeedingIngredient();
            fi.setMealId(meal.getId());
            fi.setIngredientName(name);
            ingredientMapper.insert(fi);
        }

        meal.setIngredients(ingredients);
        return meal;
    }

    /** 喂养记录列表（含食材与有效反应），时间倒序 */
    public List<FeedingMeal> listMeals(Long babyId) {
        QueryWrapper<FeedingMeal> query = new QueryWrapper<FeedingMeal>().orderByDesc("meal_time").orderByDesc("id");
        if (babyId != null) {
            query.eq("baby_id", babyId);
        }
        List<FeedingMeal> meals = mealMapper.selectList(query);
        if (meals.isEmpty()) {
            return meals;
        }
        List<Long> mealIds = meals.stream().map(FeedingMeal::getId).collect(Collectors.toList());

        Map<Long, List<String>> ingredientsMap = ingredientMapper.selectList(
                        new QueryWrapper<FeedingIngredient>().in("meal_id", mealIds))
                .stream().collect(Collectors.groupingBy(FeedingIngredient::getMealId,
                        Collectors.mapping(FeedingIngredient::getIngredientName, Collectors.toList())));

        Map<Long, AllergyReaction> reactionMap = reactionMapper.selectList(
                        new QueryWrapper<AllergyReaction>().in("meal_id", mealIds))
                .stream()
                // 有效（未撤销）反应优先，其次才是已撤销的记录
                .sorted((a, b) -> Boolean.compare(
                        Boolean.TRUE.equals(a.getRevoked()), Boolean.TRUE.equals(b.getRevoked())))
                .collect(Collectors.toMap(AllergyReaction::getMealId, r -> r, (a, b) -> a));

        for (FeedingMeal meal : meals) {
            meal.setIngredients(ingredientsMap.getOrDefault(meal.getId(), new ArrayList<>()));
            AllergyReaction reaction = reactionMap.get(meal.getId());
            meal.setReaction(Boolean.FALSE.equals(reaction == null ? null : reaction.getRevoked()) ? reaction : null);
        }
        return meals;
    }

    /** 带宝宝归属校验地取一餐 */
    public FeedingMeal requireMeal(Long mealId) {
        FeedingMeal meal = mealMapper.selectById(mealId);
        if (meal == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "喂养记录不存在");
        }
        return meal;
    }

    public List<String> ingredientsOf(Long mealId) {
        List<FeedingIngredient> rows = ingredientMapper.selectList(
                new QueryWrapper<FeedingIngredient>().eq("meal_id", mealId));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(FeedingIngredient::getIngredientName).collect(Collectors.toList());
    }
}
