<template>
  <div class="page">
    <div class="page-actions">
      <van-button type="primary" size="small" round icon="plus" @click="showForm = true">
        记录一餐
      </van-button>
      <van-button size="small" plain round @click="load">刷新</van-button>
    </div>

    <van-empty v-if="!loading && feedings.length === 0" description="还没有喂养记录，先记录一餐吧" />

    <FeedingCard
      v-for="feeding in feedings"
      :key="feeding.id"
      :feeding="feeding"
      @register-positive="openDialog('positive', $event)"
      @register-negative="openDialog('negative', $event)"
      @revoke="openDialog('revoke', $event)"
    />

    <FeedingForm v-model:show="showForm" :baby-id="babyId" @created="load" />
    <ReactionDialog
      v-model:show="showReaction"
      :mode="reactionMode"
      :feeding="activeFeeding"
      @done="load"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Button, Empty, showToast } from 'vant';
import { api } from '../api';
import type { Feeding } from '../types';
import FeedingCard from '../components/FeedingCard.vue';
import FeedingForm from '../components/FeedingForm.vue';
import ReactionDialog from '../components/ReactionDialog.vue';

const props = defineProps<{ babyId: number | null; active: boolean }>();

const feedings = ref<Feeding[]>([]);
const loading = ref(false);
const showForm = ref(false);
const showReaction = ref(false);
const reactionMode = ref<'positive' | 'negative' | 'revoke'>('positive');
const activeFeeding = ref<Feeding | null>(null);

async function load() {
  if (!props.babyId) {
    feedings.value = [];
    return;
  }
  loading.value = true;
  try {
    feedings.value = await api.listFeedings(props.babyId);
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    loading.value = false;
  }
}

function openDialog(mode: 'positive' | 'negative' | 'revoke', feeding: Feeding) {
  reactionMode.value = mode;
  activeFeeding.value = feeding;
  showReaction.value = true;
}

watch(() => [props.babyId, props.active], load, { immediate: true });
</script>

<style scoped>
.page-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
