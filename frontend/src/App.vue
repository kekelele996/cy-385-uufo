<template>
  <main>
    <header>
      <h1>辅食过敏管理</h1>
      <div class="baby-bar">
        <select v-model.number="babyId" class="baby-select" @change="loadAll">
          <option v-for="b in babies" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>
        <van-button size="mini" plain @click="showAddBaby = true">+ 新宝宝</van-button>
      </div>
    </header>

    <van-tabs v-model:active="activeTab" sticky color="#ff8a65" title-active-color="#e0633f">
      <van-tab title="喂养与反应">
        <FeedingTab
          v-if="babyId"
          :baby-id="babyId"
          :meals="meals"
          @changed="loadAll"
          @refresh="loadAll"
        />
      </van-tab>
      <van-tab title="排除状态">
        <StatusTab v-if="babyId" :statuses="statuses" @refresh="loadStatuses" />
      </van-tab>
      <van-tab title="食谱推荐">
        <RecipesTab v-if="babyId" :baby-id="babyId" :excluded="excludedNames" />
      </van-tab>
    </van-tabs>

    <van-dialog
      v-model:show="showAddBaby"
      title="新建宝宝档案"
      show-cancel-button
      @confirm="addBaby"
    >
      <van-field v-model="newBaby.name" label="昵称" placeholder="宝宝小名" />
      <van-field label="出生日期" name="birthday">
        <template #input>
          <input type="date" v-model="newBaby.birthday" class="date-input" />
        </template>
      </van-field>
    </van-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { showToast } from 'vant';
import FeedingTab from './components/FeedingTab.vue';
import StatusTab from './components/StatusTab.vue';
import RecipesTab from './components/RecipesTab.vue';
import {
  createBaby,
  listBabies,
  listIngredientStatus,
  listMeals,
  type Baby,
  type IngredientStatus,
  type Meal,
} from './api/allergy';

const activeTab = ref(0);
const babies = ref<Baby[]>([]);
const babyId = ref<number | null>(null);
const meals = ref<Meal[]>([]);
const statuses = ref<IngredientStatus[]>([]);
const showAddBaby = ref(false);
const newBaby = ref({ name: '', birthday: '' });

const excludedNames = computed(() =>
  statuses.value.filter((s) => s.status === 'EXCLUDED').map((s) => s.ingredientName),
);

async function loadBabies() {
  babies.value = await listBabies();
  if (babyId.value == null || !babies.value.some((b) => b.id === babyId.value)) {
    babyId.value = babies.value[0]?.id ?? null;
  }
}

async function loadMeals() {
  if (babyId.value == null) return;
  meals.value = await listMeals(babyId.value);
}

async function loadStatuses() {
  if (babyId.value == null) return;
  statuses.value = await listIngredientStatus(babyId.value);
}

async function loadAll() {
  await Promise.all([loadMeals(), loadStatuses()]);
}

async function addBaby() {
  if (!newBaby.value.name.trim() || !newBaby.value.birthday) {
    showToast('请填写昵称和出生日期');
    return;
  }
  const baby = await createBaby({ ...newBaby.value });
  babies.value.push(baby);
  babyId.value = baby.id;
  newBaby.value = { name: '', birthday: '' };
  await loadAll();
  showToast('档案已创建');
}

onMounted(async () => {
  try {
    await loadBabies();
    await loadAll();
  } catch (e) {
    showToast((e as Error).message);
  }
});
</script>

<style scoped>
.baby-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.baby-select {
  flex: 1;
  border: none;
  border-radius: 6px;
  padding: 6px 8px;
  background: rgba(255, 255, 255, 255);
  color: #3b2b24;
}
.date-input {
  border: 1px solid #dcdee0;
  border-radius: 4px;
  padding: 4px 6px;
}
</style>
