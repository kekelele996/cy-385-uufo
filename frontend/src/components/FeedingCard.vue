<template>
  <div class="feeding-card">
    <div class="feeding-head">
      <span class="meal-label">{{ feeding.mealTypeLabel }}</span>
      <span class="meal-time">{{ formatDateTime(feeding.eatenAt) }}</span>
      <van-tag v-if="within72h" type="primary" round size="medium">观察期内</van-tag>
      <van-tag v-else-if="!feeding.reaction" type="default" round size="medium">已过72h窗口</van-tag>
    </div>
    <div v-if="feeding.note" class="feeding-note">备注：{{ feeding.note }}</div>

    <div class="ingredient-list">
      <div v-for="ing in feeding.ingredients" :key="ing.name" class="ingredient-row">
        <StatusTag :status="ing.status" :label="ing.statusLabel" />
        <span class="ingredient-name" :class="{ excluded: ing.status === 'EXCLUDED', safe: ing.status === 'SAFE' }">
          {{ ing.name }}
        </span>
      </div>
    </div>

    <div v-if="feeding.reaction" class="reaction-box" :class="feeding.reaction.reactionType.toLowerCase()">
      <template v-if="!feeding.reaction.revoked">
        <div class="reaction-title">
          <van-tag :type="feeding.reaction.reactionType === 'POSITIVE' ? 'danger' : 'success'" plain>
            {{ feeding.reaction.reactionTypeLabel }}
          </van-tag>
          <span class="reaction-time">登记于 {{ formatDateTime(feeding.reaction.observedAt) }}</span>
        </div>
        <div v-if="feeding.reaction.symptoms" class="reaction-text">症状：{{ feeding.reaction.symptoms }}</div>
        <div class="reaction-text">
          {{ feeding.reaction.reactionType === 'POSITIVE' ? '牵连食材' : '验证食材' }}：
          {{ feeding.reaction.ingredients.join('、') }}
        </div>
        <div class="reaction-actions">
          <van-button size="small" plain type="danger" @click="emit('revoke', feeding)">
            撤销误报
          </van-button>
        </div>
      </template>
      <template v-else>
        <van-tag type="default">反应已撤销（误报）</van-tag>
        <span class="revoke-reason">原因：{{ feeding.reaction.revokedReason }}</span>
      </template>
    </div>

    <div v-else-if="within72h" class="reaction-actions">
      <van-button size="small" type="danger" plain @click="emit('register-positive', feeding)">
        登记有反应
      </van-button>
      <van-button size="small" type="success" plain @click="emit('register-negative', feeding)">
        72小时无异常
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Button, Tag } from 'vant';
import StatusTag from './StatusTag.vue';
import type { Feeding } from '../types';

const props = defineProps<{ feeding: Feeding }>();
const emit = defineEmits<{
  (e: 'register-positive', feeding: Feeding): void;
  (e: 'register-negative', feeding: Feeding): void;
  (e: 'revoke', feeding: Feeding): void;
}>();

const within72h = computed(() => {
  const eatenAt = new Date(props.feeding.eatenAt).getTime();
  return Date.now() - eatenAt <= 72 * 60 * 60 * 1000;
});

function pad(n: number) {
  return String(n).padStart(2, '0');
}

function formatDateTime(raw: string) {
  const d = new Date(raw);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style scoped>
.feeding-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
  box-shadow: 0 4px 14px rgba(128, 70, 45, 0.08);
}
.feeding-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.meal-label {
  font-weight: 600;
  font-size: 16px;
}
.meal-time {
  color: #9b8a82;
  font-size: 13px;
  flex: 1;
}
.feeding-note {
  color: #8a7a72;
  font-size: 13px;
  margin-top: 6px;
}
.ingredient-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.ingredient-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
.ingredient-name.excluded {
  color: #ee0a24;
  text-decoration: line-through;
}
.ingredient-name.safe {
  color: #07c160;
}
.reaction-box {
  margin-top: 10px;
  border-radius: 8px;
  padding: 10px;
  background: #f7f8fa;
}
.reaction-box.positive {
  background: #fef0f0;
}
.reaction-box.negative {
  background: #f0f9eb;
}
.reaction-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.reaction-time {
  font-size: 12px;
  color: #969799;
}
.reaction-text {
  font-size: 13px;
  margin-top: 6px;
  color: #5c4a42;
}
.reaction-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}
.revoke-reason {
  margin-left: 8px;
  font-size: 13px;
  color: #969799;
}
</style>
