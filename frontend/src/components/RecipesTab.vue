<template>
  <section class="card">
    <h2>辅食食谱推荐</h2>
    <van-field label="宝宝月龄">
      <template #input>
        <input v-model.number="monthAge" type="number" min="6" max="36" class="num-input" />
      </template>
    </van-field>
    <div class="muted tip">
      已自动屏蔽含已排除食材的食谱；当前排除：
      <van-tag v-if="excluded.length === 0" plain type="primary">无</van-tag>
      <van-tag
        v-for="name in excluded"
        :key="name"
        type="danger"
        plain
        style="margin-right: 4px"
        >{{ name }}</van-tag
      >
    </div>
    <div class="btn-row">
      <van-button size="small" type="primary" :loading="loading" @click="load"
        >获取推荐</van-button
      >
    </div>

    <van-empty v-if="loaded && recipes.length === 0" description="该月龄没有可安全推荐的食谱" />
    <div v-for="recipe in recipes" :key="recipe.id" class="recipe">
      <div class="recipe-head">
        <strong>{{ recipe.name }}</strong>
        <van-tag v-if="recipe.allergens" plain type="warning">{{ recipe.allergens }}</van-tag>
      </div>
      <div class="muted">食材：{{ recipe.ingredients }}</div>
      <div v-if="recipe.nutrition" class="muted">营养：{{ recipe.nutrition }}</div>
      <details><summary class="muted">做法</summary><p class="muted">{{ recipe.steps }}</p></details>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { showToast } from 'vant';
import { recommendRecipes, type Recipe } from '../api/allergy';

const props = defineProps<{ babyId: number; excluded: string[] }>();

const monthAge = ref(10);
const recipes = ref<Recipe[]>([]);
const loading = ref(false);
const loaded = ref(false);

async function load() {
  loading.value = true;
  try {
    recipes.value = await recommendRecipes(monthAge.value || 10, props.babyId);
    loaded.value = true;
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.babyId,
  () => load(),
);
</script>

<style scoped>
.num-input {
  width: 64px;
  border: 1px solid #dcdee0;
  border-radius: 4px;
  padding: 4px 6px;
}
.tip {
  margin-top: 8px;
  line-height: 1.8;
}
.btn-row {
  margin-top: 10px;
}
.recipe {
  border-top: 1px solid #f4e7e0;
  padding: 10px 0;
}
.recipe-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.muted {
  color: #9a8a80;
  font-size: 13px;
  margin: 2px 0;
}
summary {
  cursor: pointer;
}
</style>
