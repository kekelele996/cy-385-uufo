<template>
  <section class="card">
    <h2>记录一餐（可含多个食材）</h2>
    <van-field
      label="餐次"
      placeholder="如 早餐 / 加餐"
      v-model="mealType"
    />
    <van-field label="用餐时间" name="time">
      <template #input>
        <input type="datetime-local" v-model="mealTime" class="time-input" />
      </template>
    </van-field>
    <van-field
      label="食材"
      type="textarea"
      rows="2"
      autosize
      placeholder="多个食材用顿号或逗号分隔，如：鳕鱼、土豆"
      v-model="ingredientsText"
    />
    <div class="btn-row">
      <van-button type="primary" size="small" :loading="saving" @click="submit"
        >保存喂养记录</van-button
      >
    </div>
  </section>

  <section class="card">
    <div class="section-head">
      <h2>喂养与反应记录</h2>
      <van-button size="mini" plain @click="$emit('refresh')">刷新</van-button>
    </div>
    <van-empty v-if="meals.length === 0" description="还没有喂养记录" />
    <div v-for="meal in meals" :key="meal.id" class="meal">
      <div class="meal-head">
        <strong>#{{ meal.id }} {{ meal.mealType }}</strong>
        <span class="muted">{{ formatTime(meal.mealTime) }}</span>
      </div>
      <div class="ingredients">
        <van-tag plain type="primary" v-for="name in meal.ingredients" :key="name">{{ name }}</van-tag>
      </div>

      <template v-if="meal.reaction">
        <div class="reaction" v-if="meal.reaction.reacted">
          <van-tag type="danger">过敏反应 · 已登记</van-tag>
          <span class="muted">{{ formatTime(meal.reaction.reactedAt) }}</span>
        </div>
        <div class="reaction" v-else>
          <van-tag type="success">满72h无反应 · 已确认安全</van-tag>
        </div>
        <div v-if="meal.reaction.symptoms" class="muted">症状：{{ meal.reaction.symptoms }}</div>
        <div class="btn-row">
          <van-button
            size="mini"
            plain
            type="warning"
            :loading="revokingId === meal.reaction.id"
            @click="onRevoke(meal.reaction.id)"
            >撤销误报</van-button
          >
        </div>
      </template>

      <template v-else>
        <div v-if="expired(meal.mealTime)" class="muted tip">已超过餐后 72 小时观察窗口</div>
        <div class="btn-row">
          <van-button
            size="mini"
            type="danger"
            plain
            :disabled="!within72h(meal.mealTime)"
            @click="openForm(meal.id, true)"
            >出现过敏反应</van-button
          >
          <van-button
            size="mini"
            type="success"
            plain
            :disabled="!within72h(meal.mealTime)"
            @click="openForm(meal.id, false)"
            >满72h无反应</van-button
          >
        </div>

        <div v-if="activeMealId === meal.id" class="reaction-form">
          <template v-if="formReacted">
            <van-field
              label="症状"
              placeholder="如 皮疹、腹泻（选填）"
              v-model="symptoms"
            />
          </template>
          <van-field label="反应时间" name="time">
            <template #input>
              <input type="datetime-local" v-model="reactedAt" class="time-input" />
            </template>
          </van-field>
          <div class="btn-row">
            <van-button size="mini" type="primary" :loading="submitting" @click="submitReaction"
              >确认登记</van-button
            >
            <van-button size="mini" plain @click="activeMealId = null">取消</van-button>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { showConfirmDialog, showToast } from 'vant';
import {
  createMeal,
  registerReaction,
  revokeReaction,
  type Meal,
} from '../api/allergy';

const props = defineProps<{ babyId: number; meals: Meal[] }>();
const emit = defineEmits<{
  (e: 'changed'): void;
  (e: 'refresh'): void;
}>();

const mealType = ref('辅食');
const mealTime = ref(toLocalInput(new Date()));
const ingredientsText = ref('');
const saving = ref(false);

const activeMealId = ref<number | null>(null);
const formReacted = ref(true);
const symptoms = ref('');
const reactedAt = ref(toLocalInput(new Date()));
const submitting = ref(false);
const revokingId = ref<number | null>(null);

function toLocalInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`;
}

function formatTime(t: string): string {
  return t.replace('T', ' ').slice(0, 16);
}

function hoursSince(t: string): number {
  return (Date.now() - new Date(t).getTime()) / 3_600_000;
}
function within72h(t: string): boolean {
  return hoursSince(t) >= 0 && hoursSince(t) <= 72;
}
function expired(t: string): boolean {
  return hoursSince(t) > 72;
}

async function submit() {
  if (!ingredientsText.value.trim()) {
    showToast('每餐至少记录一个食材');
    return;
  }
  saving.value = true;
  try {
    await createMeal({
      babyId: props.babyId,
      mealType: mealType.value || '辅食',
      mealTime: mealTime.value,
      ingredientsText: ingredientsText.value,
    });
    ingredientsText.value = '';
    showToast('喂养记录已保存');
    emit('changed');
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    saving.value = false;
  }
}

function openForm(mealId: number, reacted: boolean) {
  activeMealId.value = mealId;
  formReacted.value = reacted;
  symptoms.value = '';
  reactedAt.value = toLocalInput(new Date());
}

async function submitReaction() {
  if (activeMealId.value == null) return;
  submitting.value = true;
  try {
    const result = await registerReaction(activeMealId.value, {
      reacted: formReacted.value,
      symptoms: formReacted.value ? symptoms.value || undefined : undefined,
      reactedAt: reactedAt.value,
    });
    activeMealId.value = null;
    showToast(
      result.duplicated
        ? '该餐已有有效反应，已返回既有登记'
        : '反应已登记，排除状态已更新',
    );
    emit('changed');
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    submitting.value = false;
  }
}

async function onRevoke(reactionId: number) {
  try {
    await showConfirmDialog({
      title: '撤销误报',
      message:
        '撤销后将重新计算排除状态：仅恢复不再被其他有效反应牵连的食材。确认撤销？',
    });
  } catch {
    return;
  }
  revokingId.value = reactionId;
  try {
    const res = await revokeReaction(reactionId, '家长标记为误报');
    showToast(res.message);
    emit('changed');
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    revokingId.value = null;
  }
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
.btn-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.meal {
  border-top: 1px solid #f4e7e0;
  padding: 12px 0;
}
.meal-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.ingredients {
  margin: 8px 0;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.reaction {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}
.reaction-form {
  margin-top: 10px;
  border: 1px dashed #f0c7b4;
  border-radius: 8px;
  padding: 8px;
}
.time-input {
  border: 1px solid #dcdee0;
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 13px;
}
.muted {
  color: #9a8a80;
  font-size: 12px;
}
.tip {
  margin-top: 6px;
}
</style>
