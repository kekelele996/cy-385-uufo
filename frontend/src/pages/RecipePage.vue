<template>
  <div class="page">
    <div class="month-row">
      <span>宝宝月龄：</span>
      <van-stepper v-model="monthAge" min="6" max="36" integer @change="load" />
      <span class="month-unit">个月</span>
      <van-button size="mini" plain round @click="load">刷新</van-button>
    </div>

    <van-notice-bar
      v-if="data && data.excludedIngredients.length"
      left-icon="warning-o"
      :text="`当前已排除食材：${data.excludedIngredients.join('、')}；含这些食材的食谱已自动屏蔽`"
    />

    <div v-if="loading">加载中…</div>

    <template v-else-if="data">
      <h3 class="section-title">可推荐（{{ data.recipes.length }}）</h3>
      <van-empty v-if="data.recipes.length === 0" description="当前月龄暂无不含排除食材的食谱" />
      <van-collapse v-model="activeNames">
        <van-collapse-item
          v-for="recipe in data.recipes"
          :key="recipe.id"
          :title="recipe.name"
          :name="recipe.id"
        >
          <div class="recipe-meta">适合 {{ recipe.monthAgeMin }}-{{ recipe.monthAgeMax }} 个月</div>
          <div class="recipe-line">食材：{{ recipe.ingredients }}</div>
          <div v-if="recipe.allergens" class="recipe-line">过敏原：{{ recipe.allergens }}</div>
          <div class="recipe-line">做法：{{ recipe.steps }}</div>
          <div class="recipe-line">营养：{{ recipe.nutrition }}</div>
        </van-collapse-item>
      </van-collapse>

      <template v-if="data.blockedRecipes.length">
        <h3 class="section-title blocked">已屏蔽（{{ data.blockedRecipes.length }}）</h3>
        <div v-for="blocked in data.blockedRecipes" :key="blocked.id" class="blocked-card">
          <span class="blocked-name">{{ blocked.name }}</span>
          <van-tag type="danger">含已排除：{{ blocked.hitIngredients.join('、') }}</van-tag>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  Button,
  Collapse,
  CollapseItem,
  Empty,
  NoticeBar,
  Stepper,
  showToast,
} from 'vant';
import { api } from '../api';
import type { RecipeRecommend } from '../types';
import { monthAgeOf, store } from '../store';

const props = defineProps<{ babyId: number | null; active: boolean }>();

const monthAge = ref(10);
const data = ref<RecipeRecommend | null>(null);
const loading = ref(false);
const activeNames = ref<number[]>([]);

async function load() {
  if (!props.babyId) {
    data.value = null;
    return;
  }
  loading.value = true;
  try {
    data.value = await api.recommendSafe(props.babyId, monthAge.value);
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.babyId, props.active],
  () => {
    const baby = store.currentBaby();
    if (baby) monthAge.value = monthAgeOf(baby.birthday);
    load();
  },
  { immediate: true },
);
</script>

<style scoped>
.month-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
}
.month-unit {
  font-size: 13px;
  color: #969799;
  flex: 1;
}
.section-title {
  font-size: 15px;
  margin: 14px 4px 8px;
}
.section-title.blocked {
  color: #ee0a24;
}
.recipe-meta {
  font-size: 12px;
  color: #969799;
  margin-bottom: 6px;
}
.recipe-line {
  font-size: 13px;
  color: #5c4a42;
  line-height: 1.7;
}
.blocked-card {
  background: #fff;
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  opacity: 0.85;
}
.blocked-name {
  text-decoration: line-through;
  color: #969799;
}
</style>
