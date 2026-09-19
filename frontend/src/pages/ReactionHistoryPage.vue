<template>
  <div class="page">
    <div class="filter">
      <van-radio-group v-model="includeRevoked" direction="horizontal">
        <van-radio :value="false">仅看有效反应</van-radio>
        <van-radio :value="true">包含已撤销</van-radio>
      </van-radio-group>
      <van-button size="mini" plain round @click="load">刷新</van-button>
    </div>

    <van-empty v-if="reactions.length === 0" description="暂无反应登记记录" />

    <div
      v-for="reaction in reactions"
      :key="reaction.id"
      class="reaction-card"
      :class="{ revoked: reaction.revoked }"
    >
      <div class="r-head">
        <van-tag :type="reaction.revoked ? 'default' : reaction.reactionType === 'POSITIVE' ? 'danger' : 'success'">
          {{ reaction.revoked ? '已撤销' : reaction.reactionTypeLabel }}
        </van-tag>
        <span class="r-time">{{ formatDateTime(reaction.observedAt) }}</span>
      </div>
      <div class="r-line">
        {{ reaction.reactionType === 'POSITIVE' ? '牵连食材' : '验证食材' }}：
        {{ reaction.ingredients.join('、') || '—' }}
      </div>
      <div v-if="reaction.symptoms" class="r-line">症状：{{ reaction.symptoms }}</div>
      <div v-if="reaction.revoked" class="r-line revoke">
        撤销原因：{{ reaction.revokedReason }}（{{ formatDateTime(reaction.revokedAt) }}）
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Button, Empty, Radio, RadioGroup, showToast } from 'vant';
import { api } from '../api';
import type { Reaction } from '../types';

const props = defineProps<{ babyId: number | null; active: boolean }>();

const reactions = ref<Reaction[]>([]);
const includeRevoked = ref(false);

async function load() {
  if (!props.babyId) {
    reactions.value = [];
    return;
  }
  try {
    reactions.value = await api.listReactions(props.babyId, includeRevoked.value);
  } catch (e) {
    showToast((e as Error).message);
  }
}

watch(() => [props.babyId, props.active, includeRevoked.value], load, { immediate: true });

function pad(n: number) {
  return String(n).padStart(2, '0');
}
function formatDateTime(raw?: string | null) {
  if (!raw) return '';
  const d = new Date(raw);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style scoped>
.filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
}
.reaction-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  box-shadow: 0 4px 14px rgba(128, 70, 45, 0.08);
}
.reaction-card.revoked {
  opacity: 0.65;
}
.r-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.r-time {
  color: #969799;
  font-size: 12px;
}
.r-line {
  font-size: 13px;
  margin-top: 6px;
  color: #5c4a42;
}
.r-line.revoke {
  color: #969799;
}
</style>
