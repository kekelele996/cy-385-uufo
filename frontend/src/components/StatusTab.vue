<template>
  <section class="card">
    <div class="section-head">
      <h2>食材排除状态</h2>
      <van-button size="mini" plain @click="$emit('refresh')">刷新</van-button>
    </div>
    <p class="muted">
      登记过敏反应时，只排除该餐中此前未安全验证的食材；已确认安全的食材不受影响。
      撤销误报后，仅恢复不再被其他有效反应牵连的食材。
    </p>
    <van-empty v-if="statuses.length === 0" description="还没有已验证的食材" />
    <div v-for="item in statuses" :key="item.id" class="status-row">
      <div class="status-head">
        <strong>{{ item.ingredientName }}</strong>
        <van-tag :type="item.status === 'EXCLUDED' ? 'danger' : 'success'">
          {{ item.status === 'EXCLUDED' ? '已排除' : '已确认安全' }}
        </van-tag>
      </div>
      <div class="reason">{{ item.reason }}</div>
      <div class="muted">最近更新：{{ formatTime(item.updatedAt) }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { IngredientStatus } from '../api/allergy';

defineProps<{ statuses: IngredientStatus[] }>();
defineEmits<{ (e: 'refresh'): void }>();

function formatTime(t: string): string {
  return t.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.section-head h2 {
  margin: 0;
}
.status-row {
  border-top: 1px solid #f4e7e0;
  padding: 10px 0;
}
.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.reason {
  margin: 6px 0 2px;
  font-size: 13px;
  color: #6b5245;
}
.muted {
  color: #9a8a80;
  font-size: 12px;
}
</style>
