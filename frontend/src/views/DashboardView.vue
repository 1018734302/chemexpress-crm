<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>{{ greeting }}，{{ auth.user?.name || '同事' }}</h1>
        <p>
          {{ auth.roleName }} · {{ auth.user?.region || '总部' }}
          <span class="dot">·</span>
          今天有 {{ todayVisits.length }} 条拜访安排
        </p>
      </div>
      <div class="page-actions">
        <el-button v-if="auth.canWriteCustomer" :icon="Plus" @click="router.push('/customers')">
          新建客户
        </el-button>
        <el-button v-if="auth.isMr" type="primary" :icon="Calendar" @click="router.push('/visits')">
          新建拜访
        </el-button>
        <el-button v-if="auth.canCreateOrder" type="primary" :icon="Document" @click="router.push('/orders/new')">
          新建订单
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="load-error"
      title="工作台数据加载失败"
    >
      <div class="load-error-body">
        <span>请确认后端服务（8080）已启动，或重新登录后再试。</span>
        <el-button size="small" type="primary" plain @click="loadData">重新加载</el-button>
      </div>
    </el-alert>

    <div class="kpi-grid">
      <div v-for="kpi in data?.kpis || []" :key="kpi.key" class="kpi-card">
        <div class="kpi-icon" :class="iconClass(kpi.key)">
          <el-icon><component :is="iconOf(kpi.key)" /></el-icon>
        </div>
        <div>
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">{{ kpi.value }}</div>
          <div class="kpi-hint">{{ kpi.hint }}</div>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel">
        <div class="panel-title">
          <h3>近 6 个月销售趋势</h3>
          <span class="sub">按发货时间统计 · 单位：元</span>
        </div>
        <div ref="trendRef" class="chart-box"></div>
      </div>
      <div class="panel">
        <div class="panel-title">
          <h3>客户结构</h3>
          <span class="sub">当前数据范围</span>
        </div>
        <div ref="pieRef" class="chart-box"></div>
      </div>
    </div>

    <div class="split-grid">
      <div class="panel">
        <div class="panel-title">
          <h3>{{ auth.isOps || auth.isAdmin ? '最近订单' : '今日拜访' }}</h3>
          <span class="link small" @click="router.push(auth.isOps || auth.isAdmin ? '/orders' : '/visits')">
            查看全部
          </span>
        </div>
        <el-table
          v-if="auth.isOps || auth.isAdmin"
          :data="data?.recentOrders || []"
          size="small"
          @row-click="(row: any) => router.push(`/orders/${row.id}`)"
        >
          <el-table-column prop="code" label="订单号" width="150" />
          <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }"><span class="money">{{ money(row.totalAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :value="row.status" :label="row.statusName" />
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <el-table
          v-else
          :data="data?.todayVisits || []"
          size="small"
          empty-text="今天暂无拜访安排"
          @row-click="(row: any) => router.push(`/visits/${row.id}`)"
        >
          <el-table-column prop="customerName" label="客户" min-width="160" show-overflow-tooltip />
          <el-table-column prop="purposeName" label="目的" width="100" />
          <el-table-column prop="repName" label="代表" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :value="row.status" :label="row.statusName" />
            </template>
          </el-table-column>
          <el-table-column label="签到" width="110">
            <template #default="{ row }">
              <span class="muted small">{{ row.checkinTime ? dateTime(row.checkinTime).slice(11) : '未签到' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-title">
          <h3>最近动态</h3>
          <span class="sub">跨模块操作记录</span>
        </div>
        <div v-if="data?.recentEvents?.length" class="event-list">
          <div v-for="event in data.recentEvents" :key="event.id" class="event-item">
            <span class="timeline-dot"></span>
            <div class="event-body">
              <div class="event-line">{{ event.detail }}</div>
              <div class="event-meta">{{ event.operatorName || '系统' }} · {{ dateTime(event.createdAt) }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">
          <p>暂无可展示的动态</p>
          <p class="small">完成拜访审批或订单流转后，这里会出现记录。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Bell,
  Box,
  Calendar,
  Document,
  Money,
  Plus,
  UserFilled,
  Wallet
} from '@element-plus/icons-vue'
import { dashboardApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { DashboardData } from '@/types'
import { dateTime, money } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const loadError = ref('')
const data = ref<DashboardData>()
const trendRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | undefined
let pieChart: echarts.ECharts | undefined

const todayVisits = computed(() => data.value?.todayVisits || [])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const iconMap: Record<string, any> = {
  sales: Wallet,
  customers: UserFilled,
  visits: Calendar,
  pending: Bell,
  ship: Box,
  receivable: Money,
  received: Wallet
}

const colorMap: Record<string, string> = {
  sales: 'blue',
  customers: 'teal',
  visits: 'amber',
  pending: 'green',
  ship: 'blue',
  receivable: 'amber',
  received: 'green'
}

function iconOf(key: string) {
  return iconMap[key] || Wallet
}

function iconClass(key: string) {
  return colorMap[key] || 'blue'
}

function renderCharts() {
  if (!data.value) return
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      grid: { left: 16, right: 20, top: 30, bottom: 12, containLabel: true },
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value: number) => money(value)
      },
      xAxis: {
        type: 'category',
        data: data.value.months,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#dbe3ee' } },
        axisLabel: { color: '#7a8ca2', fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#eef2f7' } },
        axisLabel: {
          color: '#8a9bb0',
          fontSize: 11,
          formatter: (value: number) => (value >= 10000 ? `${value / 10000}万` : `${value}`)
        }
      },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          data: data.value.salesTrend,
          lineStyle: { width: 3, color: '#0b4f9e' },
          itemStyle: { color: '#0b4f9e', borderColor: '#fff', borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(11,79,158,0.22)' },
              { offset: 1, color: 'rgba(11,79,158,0.01)' }
            ])
          }
        }
      ]
    })
  }
  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 家（{d}%）' },
      legend: {
        bottom: 0,
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: { color: '#5d7088', fontSize: 11 }
      },
      color: ['#0b4f9e', '#0d9b8a', '#4da3ff', '#b9cfe8'],
      series: [
        {
          type: 'pie',
          radius: ['48%', '70%'],
          center: ['50%', '44%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 4 },
          label: { show: false },
          data: data.value.customerTypes
        }
      ]
    })
  }
}

function resize() {
  trendChart?.resize()
  pieChart?.resize()
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    data.value = await dashboardApi.get()
    await nextTick()
    renderCharts()
  } catch {
    loadError.value = '工作台数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dot {
  margin: 0 6px;
  color: #b6c2d2;
}
.load-error {
  margin-bottom: 16px;
}
.load-error-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
}
.event-list {
  display: grid;
  gap: 12px;
  max-height: 310px;
  overflow-y: auto;
  padding-right: 4px;
}
.event-item {
  display: flex;
  align-items: flex-start;
  gap: 2px;
  font-size: 12.5px;
}
.event-item .timeline-dot {
  margin-top: 6px;
  flex: 0 0 8px;
}
.event-body {
  min-width: 0;
}
.event-line {
  color: #2c4159;
  line-height: 1.5;
}
.event-meta {
  color: #94a3b8;
  font-size: 11.5px;
  margin-top: 2px;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
