<template>
  <van-popup v-model:show="visible" position="bottom" round :style="{ maxHeight: '80%' }">
    <div class="dialog-body">
      <h3 v-if="mode !== 'revoke'">
        登记餐后反应 · {{ feeding?.mealTypeLabel }}（{{ feeding ? formatDateTime(feeding.eatenAt) : '' }}）
      </h3>
      <h3 v-else>撤销误报反应</h3>

      <template v-if="mode === 'positive'">
        <p class="tip danger">
          仅会排除本餐中此前“未验证”的食材；已确认安全的食材不受影响。
        </p>
        <van-field
          v-model="symptoms"
          label="症状表现"
          type="textarea"
          rows="2"
          autosize
          placeholder="如：口周红疹、腹泻、呕吐等"
        />
      </template>
      <p v-else-if="mode === 'negative'" class="tip safe">
        本餐未验证的食材将标记为“已确认安全”；已被其他反应排除的食材需走撤销流程。
      </p>
      <template v-else>
        <p class="tip">撤销后，仅恢复不再被其他有效反应牵连的食材。</p>
        <van-field
          v-model="revokeReason"
          label="撤销原因"
          type="textarea"
          rows="2"
          autosize
          placeholder="如：后来确认是热疹，与食物无关"
        />
      </template>

      <van-field
        v-if="mode !== 'revoke'"
        v-model="observedAtText"
        label="观察时间"
        readonly
        is-link
        @click="showDatePicker = true"
        placeholder="默认为现在"
      />
      <van-popup v-model:show="showDatePicker" position="bottom">
        <van-date-picker
          v-model="pickerDate"
          title="选择观察日期"
          @confirm="onDateConfirm"
          @cancel="showDatePicker = false"
        />
      </van-popup>
      <van-popup v-model:show="showTimePicker" position="bottom">
        <van-time-picker
          v-model="pickerTime"
          title="选择观察时间"
          @confirm="onTimeConfirm"
          @cancel="showTimePicker = false"
        />
      </van-popup>

      <div v-if="error" class="error">{{ error }}</div>
      <div class="dialog-actions">
        <van-button block plain @click="visible = false">取消</van-button>
        <van-button
          block
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          确认提交
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Button, DatePicker, Field, Popup, TimePicker, showToast } from 'vant';
import type { Feeding } from '../types';
import { api } from '../api';

type Mode = 'positive' | 'negative' | 'revoke';

const props = defineProps<{
  show: boolean;
  mode: Mode;
  feeding: Feeding | null;
}>();
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void;
  (e: 'done'): void;
}>();

const visible = ref(props.show);
watch(() => props.show, (v) => { visible.value = v; });
watch(visible, (v) => emit('update:show', v));

const symptoms = ref('');
const revokeReason = ref('');
const observedAtText = ref('');
const pickerDate = ref<string[]>([]);
const pickerTime = ref<string[]>([]);
const showDatePicker = ref(false);
const showTimePicker = ref(false);
const submitting = ref(false);
const error = ref('');

watch(() => props.show, (show) => {
  if (show) {
    symptoms.value = '';
    revokeReason.value = '';
    observedAtText.value = '';
    error.value = '';
    const now = new Date();
    pickerDate.value = [
      String(now.getFullYear()),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
    ];
    pickerTime.value = [
      String(now.getHours()).padStart(2, '0'),
      String(now.getMinutes()).padStart(2, '0'),
    ];
  }
});

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  pickerDate.value = selectedValues;
  showDatePicker.value = false;
  showTimePicker.value = true;
}

function onTimeConfirm({ selectedValues }: { selectedValues: string[] }) {
  pickerTime.value = selectedValues;
  showTimePicker.value = false;
  observedAtText.value = `${pickerDate.value.join('-')} ${pickerTime.value.join(':')}`;
}

async function submit() {
  if (!props.feeding) return;
  error.value = '';
  if (props.mode === 'positive' && !symptoms.value.trim()) {
    error.value = '请填写症状表现';
    return;
  }
  if (props.mode === 'revoke' && !revokeReason.value.trim()) {
    error.value = '请填写撤销原因';
    return;
  }
  submitting.value = true;
  try {
    const observedAt = observedAtText.value
      ? `${observedAtText.value.replace(' ', 'T')}:00`
      : undefined;
    if (props.mode === 'positive' || props.mode === 'negative') {
      await api.registerReaction(props.feeding.id, {
        babyId: props.feeding.babyId,
        reactionType: props.mode === 'positive' ? 'POSITIVE' : 'NEGATIVE',
        symptoms: props.mode === 'positive' ? symptoms.value.trim() : undefined,
        observedAt,
      });
      showToast({
        type: 'success',
        message: props.mode === 'positive' ? '反应已登记，相关食材已排除' : '已标记为安全食材',
      });
    } else if (props.feeding.reaction) {
      await api.revokeReaction(
        props.feeding.id,
        props.feeding.reaction.id,
        revokeReason.value.trim(),
      );
      showToast({ type: 'success', message: '已撤销并重新计算食材状态' });
    }
    visible.value = false;
    emit('done');
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    submitting.value = false;
  }
}

function pad(n: number) {
  return String(n).padStart(2, '0');
}
function formatDateTime(raw: string) {
  const d = new Date(raw);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
</script>

<style scoped>
.dialog-body {
  padding: 18px 16px 24px;
}
h3 {
  margin: 0 0 12px;
  font-size: 16px;
}
.tip {
  font-size: 13px;
  border-radius: 6px;
  padding: 8px 10px;
  margin: 0 0 12px;
  background: #f7f8fa;
  color: #646566;
}
.tip.danger {
  background: #fef0f0;
  color: #ee0a24;
}
.tip.safe {
  background: #f0f9eb;
  color: #07c160;
}
.error {
  color: #ee0a24;
  font-size: 13px;
  margin: 8px 0;
}
.dialog-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>
