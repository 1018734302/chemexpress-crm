<template>
  <el-tag :type="statusType(value)" effect="light" round size="small">{{ label || value || '—' }}</el-tag>
</template>

<script setup lang="ts">
import { statusType } from '@/utils/format'

defineProps<{
  value?: string
  label?: string
}>()
</script>
