<template>
  <div class="page">
    <div v-for="baby in store.state.babies" :key="baby.id" class="baby-card">
      <div class="b-main">
        <div class="b-name">
          {{ baby.name }}
          <van-tag v-if="baby.id === store.state.currentBabyId" type="primary">当前</van-tag>
        </div>
        <div class="b-meta">
          出生 {{ baby.birthday }} · {{ monthAgeOf(baby.birthday) }} 个月
          <template v-if="baby.bloodType"> · {{ baby.bloodType }}型</template>
        </div>
      </div>
      <van-button
        size="small"
        :type="baby.id === store.state.currentBabyId ? 'default' : 'primary'"
        plain
        round
        @click="selectBaby(baby.id)"
      >
        {{ baby.id === store.state.currentBabyId ? '使用中' : '切换' }}
      </van-button>
    </div>

    <van-divider>添加宝宝</van-divider>
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field v-model="name" label="姓名" placeholder="宝宝小名" :rules="[{ required: true }]" />
        <van-field
          v-model="birthday"
          label="出生日期"
          placeholder="YYYY-MM-DD"
          :rules="[{ required: true, message: '请填写出生日期' }]"
        />
        <van-field v-model="bloodType" label="血型" placeholder="可选，如 A" />
      </van-cell-group>
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit" :loading="submitting">
          创建档案
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Button, Cell, CellGroup, Divider, Field, Form, Tag, showToast } from 'vant';
import { api } from '../api';
import { monthAgeOf, store } from '../store';

const name = ref('');
const birthday = ref('');
const bloodType = ref('');
const submitting = ref(false);

function selectBaby(id: number) {
  store.setCurrentBaby(id);
  showToast({ type: 'success', message: '已切换宝宝' });
}

async function onSubmit() {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(birthday.value)) {
    showToast('出生日期格式应为 YYYY-MM-DD');
    return;
  }
  submitting.value = true;
  try {
    const baby = await api.createBaby({
      name: name.value.trim(),
      birthday: birthday.value,
      bloodType: bloodType.value.trim() || null,
    });
    await store.addBaby(baby);
    name.value = '';
    birthday.value = '';
    bloodType.value = '';
    showToast({ type: 'success', message: '档案已创建' });
  } catch (e) {
    showToast((e as Error).message);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.baby-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 14px rgba(128, 70, 45, 0.08);
}
.b-name {
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.b-meta {
  font-size: 13px;
  color: #969799;
  margin-top: 4px;
}
</style>
