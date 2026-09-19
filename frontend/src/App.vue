<template>
  <main>
    <header>
      <h1>辅食过敏管理</h1>
      <div class="header-sub">
        <span v-if="currentBaby">{{ currentBaby.name }} · {{ monthAge }} 个月</span>
        <span v-else>请先创建宝宝档案</span>
      </div>
    </header>

    <van-tabs v-model:active="active" sticky offset-top="0" color="#ff8a65" title-active-color="#ff8a65">
      <van-tab title="喂养记录">
        <FeedingPage :baby-id="currentBabyId" :active="active === 0" />
      </van-tab>
      <van-tab title="反应历史">
        <ReactionHistoryPage :baby-id="currentBabyId" :active="active === 1" />
      </van-tab>
      <van-tab title="食材状态">
        <IngredientStatusPage :baby-id="currentBabyId" :active="active === 2" />
      </van-tab>
      <van-tab title="食谱推荐">
        <RecipePage :baby-id="currentBabyId" :active="active === 3" />
      </van-tab>
      <van-tab title="宝宝">
        <BabyPage />
      </van-tab>
    </van-tabs>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Tab, Tabs, showToast } from 'vant';
import { store, monthAgeOf } from './store';
import FeedingPage from './pages/FeedingPage.vue';
import ReactionHistoryPage from './pages/ReactionHistoryPage.vue';
import IngredientStatusPage from './pages/IngredientStatusPage.vue';
import RecipePage from './pages/RecipePage.vue';
import BabyPage from './pages/BabyPage.vue';

const active = ref(0);

const currentBabyId = computed(() => store.state.currentBabyId);
const currentBaby = computed(() => store.currentBaby());
const monthAge = computed(() => (currentBaby.value ? monthAgeOf(currentBaby.value.birthday) : 0));

onMounted(async () => {
  try {
    await store.loadBabies();
  } catch (e) {
    showToast((e as Error).message);
  }
});
</script>

<style scoped>
.header-sub {
  font-size: 13px;
  opacity: 0.9;
  margin-top: 4px;
}
</style>
