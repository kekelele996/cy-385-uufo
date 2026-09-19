package com.babytracker;

import com.babytracker.constants.AllergyErrorCode;
import com.babytracker.constants.IngredientStatusType;
import com.babytracker.dto.FeedingCreateRequest;
import com.babytracker.dto.ReactionCreateRequest;
import com.babytracker.entity.Baby;
import com.babytracker.entity.IngredientStatus;
import com.babytracker.exception.BizException;
import com.babytracker.mapper.BabyMapper;
import com.babytracker.mapper.MealReactionMapper;
import com.babytracker.service.FeedingService;
import com.babytracker.service.FoodService;
import com.babytracker.service.IngredientStatusService;
import com.babytracker.service.ReactionService;
import com.babytracker.vo.FeedingVO;
import com.babytracker.vo.RecipeRecommendVO;
import com.babytracker.vo.ReactionVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.babytracker.entity.MealReaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AllergyClosedLoopIntegrationTest {

    @Autowired private BabyMapper babyMapper;
    @Autowired private FeedingService feedingService;
    @Autowired private ReactionService reactionService;
    @Autowired private IngredientStatusService statusService;
    @Autowired private FoodService foodService;
    @Autowired private MealReactionMapper mealReactionMapper;
    @SpyBean private IngredientStatusService statusServiceSpy;

    private Long babyId;

    @BeforeEach
    void setUp() {
        Baby baby = new Baby();
        baby.setName("测试宝宝");
        baby.setBirthday(LocalDate.now().minusMonths(10));
        babyMapper.insert(baby);
        babyId = baby.getId();
    }

    private FeedingVO createMeal(List<String> ingredients, int hoursAgo) {
        FeedingCreateRequest req = new FeedingCreateRequest();
        req.setBabyId(babyId);
        req.setMealType("LUNCH");
        req.setEatenAt(LocalDateTime.now().minusHours(hoursAgo));
        req.setIngredients(ingredients);
        return feedingService.create(req);
    }

    private ReactionCreateRequest reaction(String type, int observedHoursAgo, String symptoms) {
        ReactionCreateRequest req = new ReactionCreateRequest();
        req.setBabyId(babyId);
        req.setReactionType(type);
        req.setObservedAt(LocalDateTime.now().minusHours(observedHoursAgo));
        req.setSymptoms(symptoms);
        return req;
    }

    private String statusOf(String name) {
        return statusService.getOrUnverified(babyId, name).getStatus();
    }

    @Test
    void positive_reaction_excludes_only_unverified_ingredients_and_blocks_recipes() {
        // 第一餐：大米 已安全验证
        FeedingVO meal1 = createMeal(List.of("大米"), 5);
        reactionService.register(meal1.getId(), reaction("NEGATIVE", 4, null));
        assertEquals(IngredientStatusType.SAFE.name(), statusOf("大米"));

        // 第二餐：大米(已安全)、鳕鱼(未验证)、土豆(未验证)，出现反应
        FeedingVO meal2 = createMeal(List.of("大米", "鳕鱼", "土豆"), 2);
        ReactionVO rx = reactionService.register(meal2.getId(),
                reaction("POSITIVE", 1, "口周红疹"));

        // 只排除此前未安全验证的食材；已确认安全的大米不受影响
        assertEquals(IngredientStatusType.SAFE.name(), statusOf("大米"));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("鳕鱼"));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("土豆"));

        IngredientStatus cod = statusService.getOrUnverified(babyId, "鳕鱼");
        assertTrue(cod.getReason().contains("口周红疹"), "排除原因应包含症状：" + cod.getReason());
        assertTrue(rx.getIngredients().containsAll(List.of("大米", "鳕鱼", "土豆")));

        // 食谱推荐屏蔽含已排除食材的食谱并给出命中食材
        RecipeRecommendVO recommend = foodService.recommendForBaby(babyId, 10);
        assertTrue(recommend.getExcludedIngredients().contains("鳕鱼"));
        assertTrue(recommend.getBlockedRecipes().stream()
                .anyMatch(b -> b.getName().contains("鳕鱼") && b.getHitIngredients().contains("鳕鱼")));
        assertTrue(recommend.getRecipes().stream().noneMatch(r -> r.getName().contains("鳕鱼")));
    }

    @Test
    void revoke_false_positive_restores_only_ingredients_not_implicated_elsewhere() {
        // 第一餐反应：鳕鱼、土豆 被排除
        FeedingVO meal1 = createMeal(List.of("鳕鱼", "土豆"), 10);
        ReactionVO rx1 = reactionService.register(meal1.getId(), reaction("POSITIVE", 9, "腹泻"));

        // 第二餐又出现反应：鳕鱼 再次被牵连；胡萝卜首次被排除
        FeedingVO meal2 = createMeal(List.of("鳕鱼", "胡萝卜"), 5);
        ReactionVO rx2 = reactionService.register(meal2.getId(), reaction("POSITIVE", 4, "皮疹"));

        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("鳕鱼"));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("土豆"));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("胡萝卜"));

        // 撤销第一条误报：土豆/胡萝卜恢复为未验证（无其他牵连），鳕鱼仍被第二条有效反应牵连保持排除
        reactionService.revoke(rx1.getId(), "后来确认是病毒感染腹泻");
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("鳕鱼"),
                "鳕鱼仍被另一条有效反应牵连，应保持排除");
        assertEquals(IngredientStatusType.UNVERIFIED.name(), statusOf("土豆"),
                "土豆不再被任何有效反应牵连，应恢复未验证");
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("胡萝卜"));
        assertTrue(statusService.getOrUnverified(babyId, "鳕鱼").getReason().contains("皮疹"));
        assertTrue(statusService.getOrUnverified(babyId, "土豆").getReason().contains("撤销"),
                "恢复后的原因应说明撤销来源");

        // 撤销第二条：鳕鱼也恢复；反应记录保留为已撤销
        reactionService.revoke(rx2.getId(), "热疹误判");
        assertEquals(IngredientStatusType.UNVERIFIED.name(), statusOf("鳕鱼"));
        assertTrue(mealReactionMapper.selectById(rx1.getId()).getRevoked());
        assertTrue(mealReactionMapper.selectById(rx2.getId()).getRevoked());
    }

    @Test
    void negative_reaction_on_excluded_ingredient_does_not_clear_it() {
        FeedingVO meal1 = createMeal(List.of("虾"), 20);
        reactionService.register(meal1.getId(), reaction("POSITIVE", 19, "呕吐"));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("虾"));

        // 排除必须通过撤销翻案：一次无异常不能直接洗白
        FeedingVO meal2 = createMeal(List.of("虾"), 2);
        reactionService.register(meal2.getId(), reaction("NEGATIVE", 1, null));
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("虾"));
    }

    @Test
    void reaction_outside_72h_window_is_rejected() {
        FeedingVO meal = createMeal(List.of("芒果"), 80);
        BizException ex = assertThrows(BizException.class,
                () -> reactionService.register(meal.getId(), reaction("POSITIVE", 1, "红疹")));
        assertEquals(AllergyErrorCode.REACTION_WINDOW_EXPIRED, ex.getCode());
        // 未产生任何状态变化
        assertEquals(IngredientStatusType.UNVERIFIED.name(), statusOf("芒果"));
        assertEquals(0, mealReactionMapper.selectCount(
                new QueryWrapper<MealReaction>().eq("feeding_id", meal.getId())));
    }

    @Test
    void duplicate_submission_creates_only_one_active_reaction() {
        FeedingVO meal = createMeal(List.of("鳕鱼", "土豆"), 3);
        ReactionVO first = reactionService.register(meal.getId(),
                reaction("POSITIVE", 2, "红疹"));
        // 重复提交（哪怕类型不同）也只返回同一条有效反应
        ReactionVO duplicate = reactionService.register(meal.getId(),
                reaction("NEGATIVE", 2, null));

        assertEquals(first.getId(), duplicate.getId());
        long active = mealReactionMapper.selectCount(
                new QueryWrapper<MealReaction>().eq("feeding_id", meal.getId()).eq("revoked", 0));
        assertEquals(1, active);

        // 刷新后一致：喂养记录上挂的仍是同一条反应
        FeedingVO refreshed = feedingService.detail(meal.getId(), babyId);
        assertNotNull(refreshed.getReaction());
        assertEquals(first.getId(), refreshed.getReaction().getId());
        assertEquals("POSITIVE", refreshed.getReaction().getReactionType());
    }

    @Test
    void concurrent_submissions_create_only_one_active_reaction() throws Exception {
        FeedingVO meal = createMeal(List.of("鳕鱼", "土豆"), 3);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Long> reactionIds = java.util.Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    ReactionVO vo = reactionService.register(meal.getId(),
                            reaction("POSITIVE", 2, "红疹"));
                    reactionIds.add(vo.getId());
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        long active = mealReactionMapper.selectCount(
                new QueryWrapper<MealReaction>().eq("feeding_id", meal.getId()).eq("revoked", 0));
        assertEquals(1, active, "并发提交只允许一条有效反应");
        assertEquals(1, reactionIds.stream().distinct().count(), "所有成功请求返回同一条反应");
        assertEquals(0, errors.get());
        assertEquals(IngredientStatusType.EXCLUDED.name(), statusOf("鳕鱼"));
    }

    @Test
    void failure_mid_registration_rolls_back_reaction_links_and_statuses() {
        FeedingVO meal = createMeal(List.of("鳕鱼", "土豆"), 3);
        org.mockito.Mockito.doThrow(new RuntimeException("模拟状态重算失败"))
                .when(statusServiceSpy)
                .recompute(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyList());
        try {
            assertThrows(RuntimeException.class,
                    () -> reactionService.register(meal.getId(), reaction("POSITIVE", 2, "红疹")));
        } finally {
            org.mockito.Mockito.reset(statusServiceSpy);
        }
        // 事务回滚：反应、牵连食材均不存在，食材仍为未验证
        assertEquals(0, mealReactionMapper.selectCount(
                new QueryWrapper<MealReaction>().eq("feeding_id", meal.getId())));
        assertEquals(IngredientStatusType.UNVERIFIED.name(), statusOf("鳕鱼"));
        assertEquals(IngredientStatusType.UNVERIFIED.name(), statusOf("土豆"));
    }

    @Test
    void revoked_reaction_frees_unique_slot_but_keeps_audit_trail() {
        FeedingVO meal = createMeal(List.of("鳕鱼"), 3);
        ReactionVO rx = reactionService.register(meal.getId(), reaction("POSITIVE", 2, "红疹"));
        reactionService.revoke(rx.getId(), "误报");
        // 撤销后重新登记一条有效反应（窗口内）
        ReactionVO again = reactionService.register(meal.getId(),
                reaction("NEGATIVE", 1, null));
        assertNotEquals(rx.getId(), again.getId());

        List<ReactionVO> all = reactionService.listByBaby(babyId, true);
        assertEquals(2, all.size(), "已撤销记录保留用于审计");
        long active = mealReactionMapper.selectCount(
                new QueryWrapper<MealReaction>().eq("feeding_id", meal.getId()).eq("revoked", 0));
        assertEquals(1, active, "每餐仍至多一条有效反应");
        assertEquals(IngredientStatusType.SAFE.name(), statusOf("鳕鱼"));
    }

    @Test
    void double_revoke_is_rejected() {
        FeedingVO meal = createMeal(List.of("鳕鱼"), 3);
        ReactionVO rx = reactionService.register(meal.getId(), reaction("POSITIVE", 2, "红疹"));
        reactionService.revoke(rx.getId(), "误报");
        assertThrows(BizException.class, () -> reactionService.revoke(rx.getId(), "再撤销"));
    }

    @Test
    void concurrent_revokes_only_one_succeeds() throws Exception {
        FeedingVO meal = createMeal(List.of("鳕鱼", "土豆"), 3);
        ReactionVO rx = reactionService.register(meal.getId(), reaction("POSITIVE", 2, "红疹"));

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    reactionService.revoke(rx.getId(), "并发撤销");
                    success.incrementAndGet();
                } catch (BizException e) {
                    if (AllergyErrorCode.REACTION_ALREADY_REVOKED.equals(e.getCode())) {
                        rejected.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // 锁等待等异常不计入成功
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, success.get(), "并发撤销只有一个成功");
        assertEquals(threads - 1, rejected.get(), "其余请求被识别为重复撤销");
        assertTrue(mealReactionMapper.selectById(rx.getId()).getRevoked());
    }

    @Test
    void status_page_returns_statuses_with_reasons() {
        FeedingVO meal = createMeal(List.of("鳕鱼", "大米"), 3);
        reactionService.register(meal.getId(), reaction("POSITIVE", 2, "红疹"));
        Map<String, String> map = new java.util.HashMap<>();
        statusService.listStatus(babyId).forEach(s -> map.put(s.getIngredientName(), s.getStatus()));
        assertEquals("EXCLUDED", map.get("鳕鱼"));
        assertEquals("EXCLUDED", map.get("大米"));
    }
}
