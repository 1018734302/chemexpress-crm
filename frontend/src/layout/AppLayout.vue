<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="logo-wrap">
        <img src="/logo-white.svg" alt="皓元医药 ChemExpress" />
        <span>营销 CRM</span>
      </div>
      <div class="menu-caption">工作空间</div>
      <nav class="nav">
        <button
          v-for="item in menus"
          :key="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          type="button"
          @click="router.push(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-foot">
        <div class="demo-chip"><i></i>演示环境</div>
        <div class="version">MVP v1.8</div>
      </div>
    </aside>

    <div class="main-wrap">
      <header class="topbar">
        <div class="page-title">
          <span class="crumb">皓元医药 /</span>
          <b>{{ route.meta.title || '工作台' }}</b>
        </div>
        <div class="top-actions">
          <span class="today">{{ todayText }}</span>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-chip" type="button">
              <span class="avatar">{{ auth.user?.name?.slice(0, 1) || 'U' }}</span>
              <span class="user-meta">
                <b>{{ auth.user?.name || '—' }}</b>
                <small>{{ auth.roleName }} · {{ auth.user?.region || '总部' }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="auth.isAdmin" command="reset">
                  <el-icon><RefreshRight /></el-icon> 演示数据重置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>

    <el-dialog v-model="resetVisible" title="重置演示数据" width="460px" :close-on-click-modal="false">
      <div class="reset-body">
        <el-icon class="reset-icon"><WarningFilled /></el-icon>
        <div>
          <p><b>该操作会清空并重建全部演示数据。</b></p>
          <p class="muted small">客户、联系人、拜访、订单、回款与账号将恢复到初始状态，所有在线会话立即失效。</p>
        </div>
      </div>
      <el-form label-position="top" class="reset-form">
        <el-form-item label="请输入 RESET 确认">
          <el-input v-model="resetConfirm" placeholder="RESET" @keyup.enter="doReset" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="danger" :loading="resetting" :disabled="resetConfirm !== 'RESET'" @click="doReset">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  Box,
  Calendar,
  Document,
  Odometer,
  RefreshRight,
  SwitchButton,
  User,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { systemApi } from '@/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const resetVisible = ref(false)
const resetConfirm = ref('')
const resetting = ref(false)

const menus = computed(() => {
  const items = [
    { path: '/dashboard', label: '工作台', icon: Odometer },
    { path: '/customers', label: '客户管理', icon: User },
    { path: '/visits', label: '拜访管理', icon: Calendar },
    { path: '/orders', label: '订单管理', icon: Document },
    { path: '/products', label: '产品中心', icon: Box }
  ]
  if (auth.isAdmin) {
    items.push({ path: '/users', label: '用户管理', icon: UserFilled })
  }
  return items
})

const todayText = computed(() => {
  const date = new Date()
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    router.replace('/login')
  }
  if (command === 'reset') {
    resetConfirm.value = ''
    resetVisible.value = true
  }
}

async function doReset() {
  if (resetConfirm.value !== 'RESET') return
  resetting.value = true
  try {
    await systemApi.reset('RESET')
    ElMessage.success('演示数据已重置，请重新登录')
    resetVisible.value = false
    auth.logout()
    router.replace('/login')
  } finally {
    resetting.value = false
  }
}

onMounted(async () => {
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      // 401 已由请求拦截器处理
    }
  }
})
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}
.sidebar {
  width: 232px;
  flex: 0 0 232px;
  background: linear-gradient(180deg, #081a30 0%, #0a2342 55%, #0a2b50 100%);
  color: #c7d5e6;
  padding: 24px 16px 18px;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
}
.logo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}
.logo-wrap img {
  height: 30px;
  max-width: 132px;
  object-fit: contain;
}
.logo-wrap span {
  font-size: 12px;
  color: #8fb0d0;
  border-left: 1px solid rgba(255, 255, 255, 0.16);
  padding-left: 10px;
  line-height: 1.3;
}
.menu-caption {
  font-size: 10.5px;
  letter-spacing: 0.18em;
  color: #607d9c;
  padding: 22px 10px 10px;
}
.nav {
  display: grid;
  gap: 4px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  height: 42px;
  padding: 0 12px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #a9bed4;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.18s ease;
  text-align: left;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.nav-item.active {
  background: linear-gradient(90deg, rgba(22, 104, 196, 0.42), rgba(22, 104, 196, 0.16));
  color: #fff;
  box-shadow: inset 3px 0 0 #4da3ff;
}
.nav-item .el-icon {
  font-size: 17px;
}
.sidebar-foot {
  margin-top: auto;
  padding: 16px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.09);
}
.demo-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: #8fd8c9;
  background: rgba(13, 155, 138, 0.14);
  border: 1px solid rgba(13, 155, 138, 0.28);
  border-radius: 20px;
  padding: 3px 10px;
}
.demo-chip i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #4fd1b8;
  display: inline-block;
}
.version {
  margin-top: 10px;
  font-size: 10.5px;
  color: #5e7b99;
}
.main-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.topbar {
  height: 66px;
  flex: 0 0 66px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 10;
}
.page-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.page-title .crumb {
  font-size: 12px;
  color: #94a3b8;
}
.page-title b {
  font-size: 15px;
  color: #173250;
}
.top-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}
.today {
  font-size: 12px;
  color: var(--muted);
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 30px;
  padding: 4px 12px 4px 5px;
  cursor: pointer;
  transition: all 0.18s ease;
}
.user-chip:hover {
  border-color: #b9cfe8;
  box-shadow: 0 4px 12px rgba(11, 79, 158, 0.08);
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
}
.user-meta {
  text-align: left;
  line-height: 1.25;
}
.user-meta b {
  display: block;
  font-size: 12.5px;
  color: #1d3752;
}
.user-meta small {
  display: block;
  font-size: 10.5px;
  color: var(--muted);
}
.content {
  flex: 1;
  min-height: 0;
}
.reset-body {
  display: flex;
  gap: 14px;
  background: #fff7f6;
  border: 1px solid #f5d5d0;
  border-radius: 10px;
  padding: 14px 16px;
}
.reset-icon {
  color: var(--danger);
  font-size: 22px;
  margin-top: 2px;
}
.reset-body p {
  margin: 2px 0;
  color: #6f2b24;
}
.reset-form {
  margin-top: 16px;
}
@media (max-width: 1000px) {
  .sidebar { width: 76px; flex-basis: 76px; padding: 20px 10px; }
  .logo-wrap span, .menu-caption, .sidebar-foot, .nav-item span { display: none; }
  .logo-wrap img { height: 26px; }
  .nav-item { justify-content: center; padding: 0; }
  .topbar { padding: 0 16px; }
  .today { display: none; }
}
</style>
