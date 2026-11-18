<template>
  <div class="page">
    <div class="panel forbidden">
      <div class="code">403</div>
      <h2>无权限访问</h2>
      <p>你当前的角色没有查看该页面或数据的权限。请返回工作台，或联系地区经理确认数据范围。</p>
      <el-button type="primary" @click="router.replace('/dashboard')">返回工作台</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.forbidden {
  max-width: 520px;
  margin: 80px auto;
  text-align: center;
  padding: 48px 36px;
}
.code {
  font-size: 68px;
  font-weight: 800;
  color: #dbe7f5;
  letter-spacing: -3px;
  line-height: 1;
}
h2 {
  margin: 14px 0 8px;
  color: #173250;
}
p {
  color: var(--muted);
  font-size: 13.5px;
  line-height: 1.8;
  margin: 0 0 24px;
}
</style>
