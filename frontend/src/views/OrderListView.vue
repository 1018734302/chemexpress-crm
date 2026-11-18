<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>订单管理</h1>
        <p>草稿 → 待审批 → 待发货 → 已发货 → 回款完成，金额与回款全程可追溯</p>
      </div>
      <div class="page-actions">
        <el-button v-if="auth.canCreateOrder" type="primary" :icon="Plus" @click="router.push('/orders/new')">
          新建订单
        </el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          class="grow"
          placeholder="搜索订单编号、客户名称"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.status" placeholder="订单状态" clearable style="width: 130px" @change="search">
          <el-option v-for="(label, key) in meta.options.orderStatuses" :key="key" :label="label" :value="key" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="下单开始"
          end-placeholder="下单结束"
          style="width: 250px"
          @change="search"
        />
        <el-button :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default" @row-click="goDetail">
        <el-table-column prop="code" label="订单编号" width="155" />
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column prop="repName" label="代表" width="85" />
        <el-table-column prop="region" label="区域" width="72" />
        <el-table-column label="订单金额" width="130" align="right">
          <template #default="{ row }">
            <span class="money strong">{{ money(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已回款" width="130" align="right">
          <template #default="{ row }">
            <span class="money" :class="{ paid: row.paidAmount >= row.totalAmount }">{{ money(row.paidAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :value="row.status" :label="row.statusName" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="table-actions" @click.stop>
              <el-button link type="primary" @click="goDetail(row)">查看</el-button>
              <el-button v-if="canEdit(row)" link type="primary" @click="edit(row)">编辑</el-button>
              <el-button v-if="canSubmit(row)" link type="success" @click="submit(row)">提交</el-button>
              <el-button v-if="canDelete(row)" link type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">暂无订单，{{ auth.canCreateOrder ? '点击右上角新建订单。' : '可调整筛选条件。' }}</div>
        </template>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="pager.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { orderApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Order } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import { dateTime, money } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const rows = ref<Order[]>([])
const pager = reactive({ page: 1, size: 20, total: 0 })
const filters = reactive({ keyword: '', status: '' })
const dateRange = ref<[string, string] | null>(null)

function params() {
  return {
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    dateFrom: dateRange.value?.[0] || undefined,
    dateTo: dateRange.value?.[1] || undefined,
    page: pager.page,
    size: pager.size
  }
}

async function load() {
  loading.value = true
  try {
    const page = await orderApi.page(params())
    rows.value = page.records
    pager.total = page.total
  } finally {
    loading.value = false
  }
}

function search() {
  pager.page = 1
  load()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  dateRange.value = null
  search()
}

function goDetail(row: Order) {
  router.push(`/orders/${row.id}`)
}

function isEditor(row: Order) {
  return auth.isOps || (auth.isMr && row.repId === auth.user?.id)
}

function canEdit(row: Order) {
  return isEditor(row) && (row.status === 'DRAFT' || row.status === 'REJECTED')
}

function canSubmit(row: Order) {
  return canEdit(row)
}

function canDelete(row: Order) {
  return row.status === 'DRAFT' && (auth.isAdmin || auth.isOps || (auth.isMr && row.repId === auth.user?.id))
}

function edit(row: Order) {
  router.push(`/orders/${row.id}/edit`)
}

async function submit(row: Order) {
  await ElMessageBox.confirm(`确认提交订单「${row.code}」进入审批吗？`, '提交订单', {
    type: 'info',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  })
  await orderApi.submit(row.id)
  ElMessage.success('订单已提交，等待地区经理审批')
  load()
}

async function remove(row: Order) {
  await ElMessageBox.confirm(`确认删除草稿订单「${row.code}」吗？该操作不可恢复。`, '删除订单', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await orderApi.remove(row.id)
  ElMessage.success('订单已删除')
  load()
}

onMounted(async () => {
  await meta.load()
  load()
})
</script>

<style scoped>
.strong {
  font-weight: 700;
  color: #12324f;
}
.paid {
  color: var(--success);
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
