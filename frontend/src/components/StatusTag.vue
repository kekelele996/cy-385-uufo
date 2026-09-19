<template>
  <van-tag :type="tagType" round>{{ label }}</van-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Tag } from 'vant';

const props = defineProps<{
  status: 'UNVERIFIED' | 'SAFE' | 'EXCLUDED' | string;
  label?: string;
}>();

const labelMap: Record<string, string> = {
  UNVERIFIED: '未验证',
  SAFE: '已确认安全',
  EXCLUDED: '已排除',
};

const label = computed(() => props.label ?? labelMap[props.status] ?? props.status);

const tagType = computed(() => {
  if (props.status === 'SAFE') return 'success';
  if (props.status === 'EXCLUDED') return 'danger';
  return 'warning';
});
</script>
