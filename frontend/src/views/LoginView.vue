<template>
  <div class="login-page">
    <section class="brand-side">
      <div class="brand-inner">
        <img class="brand-logo" src="/logo-white.svg" alt="皓元医药 ChemExpress" />
        <div class="eyebrow">CHEMEXPRESS · MARKETING CRM</div>
        <h1>让每一次客户拜访<br />都有数据可循</h1>
        <p class="brand-desc">
          面向医药销售团队的客户、拜访与订单一体化工作台。从建档到回款，一条链路在网页内完成。
        </p>
        <div class="brand-points">
          <div class="point">
            <span class="point-icon">01</span>
            <div><b>客户 360°</b><small>医院、药店、商业公司统一档案</small></div>
          </div>
          <div class="point">
            <span class="point-icon">02</span>
            <div><b>拜访闭环</b><small>计划、签到、记录、审批全流程</small></div>
          </div>
          <div class="point">
            <span class="point-icon">03</span>
            <div><b>订单履约</b><small>审批、发货、回款自动结案</small></div>
          </div>
        </div>
        <div class="brand-note">演示环境 · 数据均为虚构 · 仅用于本地自测</div>
      </div>
    </section>

    <section class="form-side">
      <div class="login-card">
        <div class="mobile-logo">
          <img src="/logo-blue.svg" alt="皓元医药 ChemExpress" />
        </div>
        <h2>登录营销 CRM</h2>
        <p class="welcome">使用演示账号进入系统，所有业务数据可在页面内一键重置。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button class="login-btn" type="primary" size="large" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="demo-block">
          <div class="demo-title">演示账号 <span>密码均为 Chem@123</span></div>
          <div class="account-grid">
            <button
              v-for="account in accounts"
              :key="account.username"
              class="account-chip"
              type="button"
              @click="fill(account)"
            >
              <span class="avatar">{{ account.name.slice(0, 1) }}</span>
              <span class="account-text">
                <b>{{ account.name }}</b>
                <small>{{ account.roleName }} · {{ account.username }}</small>
              </span>
            </button>
          </div>
        </div>

        <p class="footer-note">皓元医药品牌仅用于界面演示，不代表真实业务或对外承诺。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'mr.chen', password: 'Chem@123' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { username: 'mr.chen', name: '陈晨', roleName: '医药代表' },
  { username: 'dsm.east', name: '李强', roleName: '地区经理' },
  { username: 'ops.zhou', name: '周琳', roleName: '商务运营' },
  { username: 'admin', name: '谢震宇', roleName: '系统管理员' }
]

function fill(account: { username: string }) {
  form.username = account.username
  form.password = 'Chem@123'
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login({ ...form })
    ElMessage.success(`欢迎回来，${auth.user?.name}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(460px, 1.1fr) minmax(460px, 0.9fr);
  background: #fff;
}
.brand-side {
  position: relative;
  overflow: hidden;
  background: linear-gradient(145deg, #071b33 0%, #0a3466 45%, #0b4f9e 72%, #0d7a94 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 56px 66px;
}
.brand-side::after {
  content: "";
  position: absolute;
  width: 520px;
  height: 520px;
  right: -160px;
  top: -180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(77, 163, 255, 0.35), transparent 65%);
}
.brand-side::before {
  content: "";
  position: absolute;
  width: 420px;
  height: 420px;
  left: -180px;
  bottom: -200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13, 155, 138, 0.3), transparent 65%);
}
.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 560px;
}
.brand-logo {
  height: 46px;
  margin-bottom: 48px;
}
.eyebrow {
  font-size: 11.5px;
  letter-spacing: 0.22em;
  color: #8fc4f5;
  margin-bottom: 18px;
}
h1 {
  font-size: 41px;
  line-height: 1.35;
  margin: 0 0 18px;
  letter-spacing: -0.5px;
  font-weight: 700;
}
.brand-desc {
  font-size: 15px;
  line-height: 1.9;
  color: #c4ddf6;
  max-width: 470px;
  margin: 0 0 38px;
}
.brand-points {
  display: grid;
  gap: 14px;
  max-width: 460px;
}
.point {
  display: flex;
  gap: 14px;
  align-items: center;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px 16px;
  backdrop-filter: blur(6px);
}
.point-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.12);
  color: #9fd0ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex: 0 0 34px;
}
.point b {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}
.point small {
  color: #a9c9e6;
  font-size: 12px;
}
.brand-note {
  margin-top: 38px;
  font-size: 12px;
  color: #7fa9cf;
}
.form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #f7f9fc;
}
.login-card {
  width: 100%;
  max-width: 430px;
}
.mobile-logo {
  display: none;
}
h2 {
  font-size: 28px;
  margin: 0 0 8px;
  color: #12263f;
  letter-spacing: -0.3px;
}
.welcome {
  font-size: 13.5px;
  color: var(--muted);
  line-height: 1.7;
  margin: 0 0 28px;
}
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 0.2em;
  margin-top: 6px;
  box-shadow: 0 8px 20px rgba(11, 79, 158, 0.22);
}
.demo-block {
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}
.demo-title {
  font-size: 13px;
  font-weight: 600;
  color: #31465e;
  margin-bottom: 12px;
}
.demo-title span {
  font-weight: 400;
  color: var(--muted);
  font-size: 12px;
  margin-left: 6px;
}
.account-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.account-chip {
  display: flex;
  gap: 10px;
  align-items: center;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s ease;
}
.account-chip:hover {
  border-color: #9dbde3;
  box-shadow: 0 4px 14px rgba(11, 79, 158, 0.08);
  transform: translateY(-1px);
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex: 0 0 32px;
}
.account-text b {
  display: block;
  font-size: 13px;
  color: #1b3651;
}
.account-text small {
  display: block;
  font-size: 11px;
  color: var(--muted);
  margin-top: 1px;
}
.footer-note {
  margin-top: 24px;
  font-size: 11.5px;
  color: #94a3b8;
  line-height: 1.6;
}
@media (max-width: 980px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-side { display: none; }
  .form-side { padding: 24px; background: #fff; }
  .mobile-logo { display: block; margin-bottom: 26px; }
  .mobile-logo img { height: 38px; }
}
</style>
