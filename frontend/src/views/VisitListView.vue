<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>拜访管理</h1>
        <p>计划 → 签到 → 记录 → 审批，全流程留痕，重复拜访自动拦截</p>
      </div>
      <div class="page-actions">
        <el-button v-if="auth.isMr" type="primary" :icon="Plus" @click="openCreate">新建拜访</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          class="grow"
          placeholder="搜索拜访编号、客户名称"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.status" placeholder="拜访状态" clearable style="width: 130px" @change="search">
          <el-option v-for="(label, key) in meta.options.visitStatuses" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="filters.purpose" placeholder="拜访目的" clearable style="width: 130px" @change="search">
          <el-option v-for="(label, key) in meta.options.visitPurposes" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select
          v-if="auth.isDsm || auth.isAdmin"
          v-model="filters.repId"
          placeholder="医药代表"
          clearable
          style="width: 130px"
          @change="search"
        >
          <el-option v-for="owner in meta.options.owners" :key="owner.id" :label="owner.name" :value="owner.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
          @change="search"
        />
        <el-button :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default" @row-click="goDetail">
        <el-table-column prop="code" label="拜访编号" width="145" />
        <el-table-column prop="customerName" label="客户" min-width="170" show-overflow-tooltip />
        <el-table-column prop="visitDate" label="拜访日期" width="110" />
        <el-table-column prop="purposeName" label="拜访目的" width="110" />
        <el-table-column prop="contactName" label="联系人" width="95">
          <template #default="{ row }">{{ row.contactName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="repName" label="代表" width="85" />
        <el-table-column prop="region" label="区域" width="72" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :value="row.status" :label="row.statusName" />
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="150">
          <template #default="{ row }">{{ dateTime(row.checkinTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">暂无拜访记录，{{ auth.isMr ? '点击右上角新建拜访计划。' : '可调整筛选条件。' }}</div>
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

    <el-dialog v-model="createVisible" title="新建拜访计划" width="560px" :close-on-click-modal="false">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="90px">
        <el-form-item label="拜访客户" prop="customerId">
          <el-select
            v-model="planForm.customerId"
            filterable
            placeholder="请选择客户（已流失客户不可拜访）"
            @change="onCustomerChange"
          >
            <el-option
              v-for="item in visitCustomers"
              :key="item.id"
              :label="`${item.name}（${item.statusName}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-select v-model="planForm.contactId" clearable placeholder="可不选">
            <el-option v-for="item in customerContacts" :key="item.id" :label="`${item.name}${item.title ? ' · ' + item.title : ''}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="拜访日期" prop="visitDate">
          <el-date-picker
            v-model="planForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="今天起 30 天内"
            :disabled-date="disableDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="拜访目的" prop="purpose">
          <el-select v-model="planForm.purpose" placeholder="请选择拜访目的">
            <el-option v-for="(label, key) in meta.options.visitPurposes" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="3" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <div class="form-tip">同一客户当天只允许存在一条未结束的拜访；计划中的拜访可在详情页修改或取消。</div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">创建计划</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { contactApi, customerApi, visitApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Contact, Customer, Visit } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import { dateTime, today } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const rows = ref<Visit[]>([])
const pager = reactive({ page: 1, size: 20, total: 0 })
const filters = reactive({ keyword: '', status: '', purpose: '', repId: null as number | null })
const dateRange = ref<[string, string] | null>(null)

const createVisible = ref(false)
const saving = ref(false)
const planFormRef = ref<FormInstance>()
const customers = ref<Customer[]>([])
const customerContacts = ref<Contact[]>([])
const planForm = reactive<any>({
  customerId: null,
  contactId: null,
  visitDate: '',
  purpose: 'MAINTAIN',
  remark: ''
})

const visitCustomers = computed(() => customers.value.filter((item) => item.status !== 'CHURNED'))

const planRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  visitDate: [{ required: true, message: '请选择拜访日期', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择拜访目的', trigger: 'change' }]
}

function params() {
  return {
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
    purpose: filters.purpose || undefined,
    repId: filters.repId || undefined,
    dateFrom: dateRange.value?.[0] || undefined,
    dateTo: dateRange.value?.[1] || undefined,
    page: pager.page,
    size: pager.size
  }
}

async function load() {
  loading.value = true
  try {
    const page = await visitApi.page(params())
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
  filters.purpose = ''
  filters.repId = null
  dateRange.value = null
  search()
}

function goDetail(row: Visit) {
  router.push(`/visits/${row.id}`)
}

function disableDate(date: Date) {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const end = new Date(start)
  end.setDate(end.getDate() + 30)
  return date < start || date > end
}

async function openCreate() {
  planForm.customerId = null
  planForm.contactId = null
  planForm.visitDate = today()
  planForm.purpose = 'MAINTAIN'
  planForm.remark = ''
  customerContacts.value = []
  createVisible.value = true
  if (!customers.value.length) {
    const page = await customerApi.page({ page: 1, size: 100 })
    customers.value = page.records
  }
  const preset = Number(route.query.customerId)
  if (preset && customers.value.some((item) => item.id === preset)) {
    planForm.customerId = preset
    await onCustomerChange(preset)
  }
}

async function onCustomerChange(customerId: number) {
  planForm.contactId = null
  customerContacts.value = customerId ? await contactApi.list(customerId) : []
}

async function savePlan() {
  if (!planFormRef.value) return
  const valid = await planFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      ...planForm,
      contactId: planForm.contactId || null,
      remark: planForm.remark || undefined
    }
    const visit = await visitApi.create(payload)
    ElMessage.success('拜访计划创建成功')
    createVisible.value = false
    router.push(`/visits/${visit.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await meta.load()
  await load()
  if (route.query.create === '1' && auth.isMr) {
    openCreate()
  }
})
</script>
