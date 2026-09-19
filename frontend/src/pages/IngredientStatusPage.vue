<template>
  <div class="page">
    <div class="summary">
      <van-tag type="danger" size="large">已排除 {{ counts.excluded }}</van-tag>
      <van-tag type="success" size="large">已确认安全 {{ counts.safe }}</van-tag>
      <van-tag type="warning" size="large">未验证 {{ counts.unverified }}</van-tag>
    </div>

    <van-dropdown-menu>
      <van-dropdown-item v-model="filter" :options="filterOptions" />
    </van-dropdown-menu>

    <van-empty v-if="filtered.length === 0" description="暂无食材状态，记录喂养并登记反应后出现" />

    <div v-for="item in filtered" :key="item.id" class="status-card" :class="item.status.toLowerCase()">
      <div class="s-head">
        <StatusTag :status="item.status" :label="item.statusLabel" />
        <span class="s-name">{{ item.ingredientName }}</span>
      </div>
      <div class="s-reason">{{ item.reason }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { DropdownItem, DropdownMenu, Empty, Tag, showToast } from 'vant';
import { api } from '../api';
import type { IngredientStatus } from '../types';
import StatusTag from '../components/StatusTag.vue';

const props = defineProps<{ babyId: number | null; active: boolean }>();

const list = ref<IngredientStatus[]>([]);
const filter = ref('ALL');
const filterOptions = [
  { text: '全部食材', value: 'ALL' },
  { text: '仅看已排除', value: 'EXCLUDED' },
  { text: '仅看已安全', value: 'SAFE' },
  { text: '仅看未验证', value: 'UNVERIFIED' },
];

const counts = computed(() => ({
  excluded: list.value.filter((i) => i.status === 'EXCLUDED').length,
  safe: list.value.filter((i) => i.status === 'SAFE').length,
  unverified: list.value.filter((i) => i.status === 'UNVERIFIED').length,
}));

const filtered = computed(() =>
  filter.value === 'ALL' ? list.value : list.value.filter((i) => i.status === filter.value),
);

async function load() {
  if (!props.babyId) {
    list.value = [];
    return;
  }
  try {
    list.value = await api.listIngredientStatus(props.babyId);
  } catch (e) {
    showToast((e as Error).message);
  }
}

watch(() => [props.babyId, props.active], load, { immediate: true });
defineExpose({ reload: load });
</script>

<style scoped>
.summary {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.status-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  box-shadow: 0 4px 14px rgba(128, 70, 45, 0.08);
  border-left: 4px solid #ffd01e;
}
.status-card.excluded {
  border-left-color: #ee0a24;
}
.status-card.safe {
  border-left-color: #07c160;
}
.s-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.s-name {
  font-weight: 600;
  font-size: 15px;
}
.s-reason {
  margin-top: 8px;
  font-size: 13px;
  color: #7a6a62;
  line-height: 1.6;
}
</style>
