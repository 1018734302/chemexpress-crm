<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>客户管理</h1>
        <p>医院、连锁药店、单体药店与商业公司的统一客户档案</p>
      </div>
      <div class="page-actions">
        <el-button v-if="auth.canWriteCustomer" type="primary" :icon="Plus" @click="openCreate">
          新建客户
        </el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon blue"><el-icon><User /></el-icon></div>
        <div><div class="kpi-label">客户总数</div><div class="kpi-value">{{ stats.total || 0 }}</div></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon teal"><el-icon><CircleCheck /></el-icon></div>
        <div><div class="kpi-label">合作中</div><div class="kpi-value">{{ stats.active || 0 }}</div></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon amber"><el-icon><TrendCharts /></el-icon></div>
        <div><div class="kpi-label">本月新增</div><div class="kpi-value">{{ stats.newThisMonth || 0 }}</div></div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon green"><el-icon><Star /></el-icon></div>
        <div><div class="kpi-label">A 级客户</div><div class="kpi-value">{{ stats.levelA || 0 }}</div></div>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          class="grow"
          placeholder="搜索客户名称或编码"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.type" placeholder="客户类型" clearable style="width: 130px" @change="search">
          <el-option v-for="(label, key) in meta.options.customerTypes" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="filters.level" placeholder="等级" clearable style="width: 100px" @change="search">
          <el-option label="A 级" value="A" />
          <el-option label="B 级" value="B" />
          <el-option label="C 级" value="C" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px" @change="search">
          <el-option v-for="(label, key) in meta.options.customerStatuses" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select
          v-if="auth.isAdmin || auth.isOps"
          v-model="filters.region"
          placeholder="区域"
          clearable
          style="width: 110px"
          @change="search"
        >
          <el-option label="华东" value="华东" />
          <el-option label="华北" value="华北" />
        </el-select>
        <el-button :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default" @row-click="goDetail">
        <el-table-column prop="code" label="客户编码" width="105" />
        <el-table-column prop="name" label="客户名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="customer-name">{{ row.name }}</span>
            <span v-if="row.tags" class="tag-soft">{{ row.tags.split(',')[0] }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="类型" width="95" />
        <el-table-column label="等级" width="72">
          <template #default="{ row }">
            <span class="level" :class="`level-${row.level}`">{{ row.level }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="区域" width="72" />
        <el-table-column prop="ownerName" label="负责人" width="85" />
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <StatusTag :value="row.status" :label="row.statusName" />
          </template>
        </el-table-column>
        <el-table-column label="最近拜访" width="110">
          <template #default="{ row }">{{ row.lastVisitDate || '—' }}</template>
        </el-table-column>
        <el-table-column label="年潜力" width="120" align="right">
          <template #default="{ row }">
            <span class="money">{{ row.annualPotential ? money(row.annualPotential) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div class="table-actions" @click.stop>
              <el-button link type="primary" @click="goDetail(row)">查看</el-button>
              <el-button v-if="canEdit(row)" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="auth.isDsm || auth.isAdmin" link type="primary" @click="openTransfer(row)">
                转交
              </el-button>
              <el-button v-if="canEdit(row)" link type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">未找到匹配客户，试试调整筛选条件。</div>
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

    <CustomerFormDialog v-model="formVisible" :customer="editing" @saved="onSaved" />

    <el-dialog v-model="transferVisible" title="转交客户负责人" width="420px">
      <p class="muted small">将 <b>{{ transferTarget?.name }}</b> 转交给同区域的医药代表，历史单据不受影响。</p>
      <el-select v-model="transferOwnerId" placeholder="请选择新的负责人" style="width: 100%; margin-top: 10px">
        <el-option
          v-for="owner in transferOwners"
          :key="owner.id"
          :label="`${owner.name}（${owner.region}）`"
          :value="owner.id"
          :disabled="owner.id === transferTarget?.ownerId"
        />
      </el-select>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!transferOwnerId" @click="confirmTransfer">确认转交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Plus, Refresh, Search, Star, TrendCharts, User } from '@element-plus/icons-vue'
import { customerApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Customer } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import CustomerFormDialog from '@/components/CustomerFormDialog.vue'
import { money } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const rows = ref<Customer[]>([])
const stats = ref<Record<string, number>>({})
const pager = reactive({ page: 1, size: 20, total: 0 })
const filters = reactive({ keyword: '', type: '', level: '', status: '', region: '' })
const formVisible = ref(false)
const editing = ref<Customer | null>(null)
const transferVisible = ref(false)
const transferTarget = ref<Customer | null>(null)
const transferOwnerId = ref<number | null>(null)

const transferOwners = computed(() =>
  meta.options.owners.filter((owner) => owner.region === transferTarget.value?.region)
)

function params() {
  return {
    keyword: filters.keyword || undefined,
    type: filters.type || undefined,
    level: filters.level || undefined,
    status: filters.status || undefined,
    region: filters.region || undefined,
    page: pager.page,
    size: pager.size
  }
}

async function load() {
  loading.value = true
  try {
    const [page, stat] = await Promise.all([
      customerApi.page(params()),
      customerApi.stats({ ...params(), page: undefined, size: undefined })
    ])
    rows.value = page.records
    pager.total = page.total
    stats.value = stat
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
  filters.type = ''
  filters.level = ''
  filters.status = ''
  filters.region = ''
  search()
}

function canEdit(row: Customer) {
  return auth.canWriteCustomer && (auth.isDsm || row.ownerId === auth.user?.id)
}

function goDetail(row: Customer) {
  router.push(`/customers/${row.id}`)
}

function openCreate() {
  editing.value = null
  formVisible.value = true
}

function openEdit(row: Customer) {
  editing.value = row
  formVisible.value = true
}

function onSaved() {
  load()
}

function openTransfer(row: Customer) {
  transferTarget.value = row
  transferOwnerId.value = null
  transferVisible.value = true
}

async function confirmTransfer() {
  if (!transferTarget.value || !transferOwnerId.value) return
  await customerApi.transfer(transferTarget.value.id, transferOwnerId.value)
  ElMessage.success('客户负责人已转交')
  transferVisible.value = false
  load()
}

async function remove(row: Customer) {
  await ElMessageBox.confirm(
    `确认删除客户「${row.name}」吗？已有拜访或订单的客户不能删除。`,
    '删除客户',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  await customerApi.remove(row.id)
  ElMessage.success('客户已删除')
  load()
}

onMounted(async () => {
  await meta.load()
  load()
})
</script>

<style scoped>
.customer-name {
  font-weight: 600;
  color: #1b3651;
  margin-right: 6px;
}
.level {
  display: inline-flex;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}
.level-A { background: #fdeceb; color: #b03a2e; }
.level-B { background: #fff5e5; color: #a15c00; }
.level-C { background: #eef2f7; color: #5d7088; }
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
