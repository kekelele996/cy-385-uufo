<template>
  <van-popup v-model:show="visible" position="bottom" round :style="{ maxHeight: '85%' }">
    <div class="dialog-body">
      <h3>记录辅食喂养</h3>

      <van-field label="餐次" readonly is-link :model-value="mealTypeLabel" @click="showMealPicker = true" />
      <van-popup v-model:show="showMealPicker" position="bottom">
        <van-picker
          :columns="mealColumns"
          title="选择餐次"
          @confirm="onMealConfirm"
          @cancel="showMealPicker = false"
        />
      </van-popup>

      <van-field
        label="喂养时间"
        readonly
        is-link
        :model-value="eatenAtText"
        @click="showDateTimePicker = true"
      />
      <van-popup v-model:show="showDateTimePicker" position="bottom">
        <van-date-picker
          v-model="pickerDate"
          title="选择喂养日期"
          @confirm="onDateConfirm"
          @cancel="showDateTimePicker = false"
        />
      </van-popup>
      <van-popup v-model:show="showTimePicker" position="bottom">
        <van-time-picker
          v-model="pickerTime"
          title="选择喂养时间"
          @confirm="onTimeConfirm"
          @cancel="showTimePicker = false"
        />
      </van-popup>

      <van-field
        v-model="ingredientsText"
        label="食材"
        type="textarea"
        rows="2"
        autosize
        placeholder="多个食材用顿号或逗号分隔，如：鳕鱼、土豆"
      />
      <van-field
        v-model="note"
        label="备注"
        placeholder="可选"
      />

      <div v-if="error" class="error">{{ error }}</div>
      <div class="dialog-actions">
        <van-button block plain @click="visible = false">取消</van-button>
        <van-button block type="primary" :loading="submitting" @click="submit">保存</van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { Button, DatePicker, Field, Picker, Popup, TimePicker, showToast } from 'vant';
import { api } from '../api';

const props = defineProps<{ show: boolean; babyId: number | null }>();
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void;
  (e: 'created'): void;
}>();

const visible = ref(props.show);
watch(() => props.show, (v) => { visible.value = v; });
watch(visible, (v) => emit('update:show', v));

const mealColumns = [
  { text: '早餐', value: 'BREAKFAST' },
  { text: '午餐', value: 'LUNCH' },
  { text: '晚餐', value: 'DINNER' },
  { text: '加餐', value: 'SNACK' },
];
const mealType = ref('LUNCH');
const mealTypeLabel = ref('午餐');
const showMealPicker = ref(false);

const eatenAtText = ref('');
const showDateTimePicker = ref(false);
const showTimePicker = ref(false);
const pickerDate = ref<string[]>([]);
const pickerTime = ref<string[]>([]);

const ingredientsText = ref('');
const note = ref('');
const error = ref('');
const submitting = ref(false);

watch(() => props.show, (show) => {
  if (show) {
    error.value = '';
    ingredientsText.value = '';
    note.value = '';
    mealType.value = 'LUNCH';
    mealTypeLabel.value = '午餐';
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
    eatenAtText.value = `${pickerDate.value.join('-')} ${pickerTime.value.join(':')}`;
  }
});

function onMealConfirm({ selectedOptions }: { selectedOptions: { text: string; value: string }[] }) {
  mealType.value = selectedOptions[0].value;
  mealTypeLabel.value = selectedOptions[0].text;
  showMealPicker.value = false;
}

function onDateConfirm({ selectedValues }: { selectedValues: string[] }) {
  pickerDate.value = selectedValues;
  showDateTimePicker.value = false;
  showTimePicker.value = true;
}

function onTimeConfirm({ selectedValues }: { selectedValues: string[] }) {
  pickerTime.value = selectedValues;
  showTimePicker.value = false;
  eatenAtText.value = `${pickerDate.value.join('-')} ${pickerTime.value.join(':')}`;
}

async function submit() {
  error.value = '';
  if (!props.babyId) {
    error.value = '请先选择宝宝';
    return;
  }
  const ingredients = ingredientsText.value
    .split(/[、,，/;；\n]/)
    .map((s) => s.trim())
    .filter(Boolean);
  if (ingredients.length === 0) {
    error.value = '每餐至少记录一个食材';
    return;
  }
  submitting.value = true;
  try {
    await api.createFeeding({
      babyId: props.babyId,
      mealType: mealType.value,
      eatenAt: `${eatenAtText.value.replace(' ', 'T')}:00`,
      ingredients,
      note: note.value.trim() || undefined,
    });
    showToast({ type: 'success', message: '喂养记录已保存' });
    visible.value = false;
    emit('created');
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    submitting.value = false;
  }
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
.error {
  color: #ee0a24;
  font-size: 13px;
  margin: 8px 4px;
}
.dialog-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
</style>
