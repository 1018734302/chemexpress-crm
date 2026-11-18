<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="back-row">
          <el-button link :icon="ArrowLeft" @click="router.push('/visits')">返回拜访列表</el-button>
        </div>
        <h1 class="title-row">
          {{ visit?.customerName || '拜访详情' }}
          <StatusTag v-if="visit" :value="visit.status" :label="visit.statusName" />
        </h1>
        <p>{{ visit?.code }} · {{ visit?.visitDate }} · {{ visit?.purposeName }} · 代表 {{ visit?.repName }}</p>
      </div>
      <div v-if="visit" class="page-actions">
        <el-button v-if="canEditPlan" :icon="Edit" @click="openEdit">修改计划</el-button>
        <el-button v-if="canCheckin" type="primary" :icon="Location" @click="doCheckin">签到</el-button>
        <el-button v-if="canSubmit" type="primary" :icon="DocumentChecked" @click="openSubmit">
          {{ visit.status === 'REJECTED' ? '重新提交记录' : '提交拜访记录' }}
        </el-button>
        <el-button v-if="canApprove" type="success" :icon="CircleCheck" @click="doApprove">审批通过</el-button>
        <el-button v-if="canApprove" type="danger" plain :icon="CircleClose" @click="openReason('reject')">
          驳回
        </el-button>
        <el-button v-if="canCancel" plain :icon="Close" @click="openReason('cancel')">取消拜访</el-button>
      </div>
    </div>

    <template v-if="visit">
      <el-alert
        v-if="visit.status === 'PLANNED' && !canCheckin"
        type="info"
        :closable="false"
        show-icon
        class="tip"
        title="签到仅在拜访当日开放，且只有拜访负责人可以操作。"
      />
      <el-alert
        v-if="visit.status === 'REJECTED' && visit.rejectReason"
        type="error"
        :closable="false"
        show-icon
        class="tip"
        :title="`驳回原因：${visit.rejectReason}`"
      />
      <el-alert
        v-if="visit.status === 'CANCELLED' && visit.cancelReason"
        type="warning"
        :closable="false"
        show-icon
        class="tip"
        :title="`取消原因：${visit.cancelReason}`"
      />

      <div class="split-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>拜访信息</h3>
            <span class="sub">{{ visit.checkinTime ? `签到于 ${dateTime(visit.checkinTime)}` : '尚未签到' }}</span>
          </div>
          <div class="detail-grid two">
            <div class="detail-item">
              <div class="label">拜访日期</div>
              <div class="value">{{ visit.visitDate }}</div>
            </div>
            <div class="detail-item">
              <div class="label">拜访目的</div>
              <div class="value">{{ visit.purposeName }}</div>
            </div>
            <div class="detail-item">
              <div class="label">联系人</div>
              <div class="value">{{ visit.contactName || '—' }}</div>
            </div>
            <div class="detail-item">
              <div class="label">所属区域</div>
              <div class="value">{{ visit.region }}</div>
            </div>
            <div class="detail-item">
              <div class="label">签到时间</div>
              <div class="value">{{ dateTime(visit.checkinTime) }}</div>
            </div>
            <div class="detail-item">
              <div class="label">签到距离</div>
              <div class="value">{{ visit.checkinDistance != null ? visit.checkinDistance + ' 米' : '—' }}</div>
            </div>
            <div class="detail-item span-2">
              <div class="label">计划备注</div>
              <div class="value">{{ visit.remark || '—' }}</div>
            </div>
            <div v-if="visit.approvedAt" class="detail-item span-2">
              <div class="label">审批时间</div>
              <div class="value">{{ dateTime(visit.approvedAt) }}</div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>拜访记录</h3>
            <span class="sub">提交后由地区经理审批</span>
          </div>
          <template v-if="visit.status === 'SUBMITTED' || visit.status === 'APPROVED' || visit.status === 'REJECTED'">
            <div class="record-block">
              <div class="record-label">拜访结果</div>
              <el-tag :type="resultType(visit.result)" effect="light">
                {{ meta.options.visitResults[visit.result || ''] || visit.result || '—' }}
              </el-tag>
            </div>
            <div class="record-block">
              <div class="record-label">客户反馈</div>
              <p class="record-text">{{ visit.feedback || '—' }}</p>
            </div>
            <div class="record-block">
              <div class="record-label">下一步计划</div>
              <p class="record-text">{{ visit.nextStep || '—' }}</p>
            </div>
            <div class="record-block">
              <div class="record-label">关联产品</div>
              <div v-if="visit.products?.length" class="product-chips">
                <span v-for="product in visit.products" :key="product.id" class="product-chip">
                  {{ product.tradeName }}
                  <small>{{ product.spec }}</small>
                </span>
              </div>
              <span v-else class="muted">—</span>
            </div>
          </template>
          <div v-else class="empty-tip">尚未提交拜访记录</div>
        </div>
      </div>

      <div class="panel section-gap">
        <div class="panel-title">
          <h3>操作时间线</h3>
          <span class="sub">{{ visit.events?.length || 0 }} 条记录</span>
        </div>
        <el-timeline v-if="visit.events?.length">
          <el-timeline-item
            v-for="event in visit.events"
            :key="event.id"
            :timestamp="dateTime(event.createdAt)"
            placement="top"
          >
            <div class="event-line">
              <b>{{ actionLabel(event.action) }}</b>
              <span>{{ event.detail }}</span>
              <span class="muted">· {{ event.operatorName }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="empty-tip">暂无操作记录</div>
      </div>
    </template>

    <el-dialog v-model="editVisible" title="修改拜访计划" width="540px" :close-on-click-modal="false">
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="90px">
        <el-form-item label="拜访客户">
          <el-input :model-value="visit?.customerName" disabled />
        </el-form-item>
        <el-form-item label="联系人">
          <el-select v-model="planForm.contactId" clearable placeholder="可不选">
            <el-option
              v-for="item in contacts"
              :key="item.id"
              :label="`${item.name}${item.title ? ' · ' + item.title : ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="拜访日期" prop="visitDate">
          <el-date-picker
            v-model="planForm.visitDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="拜访目的" prop="purpose">
          <el-select v-model="planForm.purpose">
            <el-option v-for="(label, key) in meta.options.visitPurposes" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="3" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submitVisible" title="提交拜访记录" width="600px" :close-on-click-modal="false">
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="90px">
        <el-form-item label="拜访结果" prop="result">
          <el-radio-group v-model="submitForm.result">
            <el-radio-button v-for="(label, key) in meta.options.visitResults" :key="key" :value="key">
              {{ label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="客户反馈" prop="feedback">
          <el-input v-model="submitForm.feedback" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="至少 10 个字，记录客户原话与关注点" />
        </el-form-item>
        <el-form-item label="下一步计划" prop="nextStep">
          <el-input v-model="submitForm.nextStep" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="至少 5 个字，如：两周内补充产品资料并约见药剂科主任" />
        </el-form-item>
        <el-form-item label="关联产品" prop="productIds">
          <el-select v-model="submitForm.productIds" multiple filterable collapse-tags placeholder="至少选择一个产品">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="`${product.tradeName} · ${product.spec}`"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSubmit">提交审批</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reasonVisible" :title="reasonMode === 'reject' ? '驳回拜访' : '取消拜访'" width="440px">
      <el-input
        v-model="reasonText"
        type="textarea"
        :rows="3"
        maxlength="300"
        show-word-limit
        :placeholder="reasonMode === 'reject' ? '请填写驳回原因（至少 5 个字）' : '请填写取消原因（至少 5 个字）'"
      />
      <template #footer>
        <el-button @click="reasonVisible = false">返回</el-button>
        <el-button type="danger" :loading="saving" :disabled="reasonText.trim().length < 5" @click="saveReason">
          确认{{ reasonMode === 'reject' ? '驳回' : '取消' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  ArrowLeft,
  CircleCheck,
  CircleClose,
  Close,
  DocumentChecked,
  Edit,
  Location
} from '@element-plus/icons-vue'
import { contactApi, productApi, visitApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Contact, Product, Visit } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import { dateTime, today } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const saving = ref(false)
const visit = ref<Visit | null>(null)
const contacts = ref<Contact[]>([])
const products = ref<Product[]>([])

const editVisible = ref(false)
const planFormRef = ref<FormInstance>()
const planForm = reactive<any>({ contactId: null, visitDate: '', purpose: 'MAINTAIN', remark: '' })

const submitVisible = ref(false)
const submitFormRef = ref<FormInstance>()
const submitForm = reactive<any>({ result: 'SUCCESS', feedback: '', nextStep: '', productIds: [] })

const reasonVisible = ref(false)
const reasonMode = ref<'reject' | 'cancel'>('reject')
const reasonText = ref('')

const visitId = computed(() => Number(route.params.id))
const isOwner = computed(() => Boolean(visit.value && auth.isMr && visit.value.repId === auth.user?.id))
const canEditPlan = computed(() => isOwner.value && visit.value?.status === 'PLANNED')
const canCheckin = computed(
  () => isOwner.value && visit.value?.status === 'PLANNED' && visit.value?.visitDate === today()
)
const canSubmit = computed(
  () => isOwner.value && (visit.value?.status === 'CHECKED_IN' || visit.value?.status === 'REJECTED')
)
const canApprove = computed(
  () =>
    Boolean(auth.isDsm && visit.value?.status === 'SUBMITTED' && visit.value?.repId !== auth.user?.id)
)
const canCancel = computed(
  () => isOwner.value && (visit.value?.status === 'PLANNED' || visit.value?.status === 'CHECKED_IN')
)

const planRules = {
  visitDate: [{ required: true, message: '请选择拜访日期', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择拜访目的', trigger: 'change' }]
}

const submitRules = {
  result: [{ required: true, message: '请选择拜访结果', trigger: 'change' }],
  feedback: [
    { required: true, message: '请填写客户反馈', trigger: 'blur' },
    { min: 10, max: 1000, message: '客户反馈需为 10-1000 个字符', trigger: 'blur' }
  ],
  nextStep: [
    { required: true, message: '请填写下一步计划', trigger: 'blur' },
    { min: 5, max: 500, message: '下一步计划需为 5-500 个字符', trigger: 'blur' }
  ],
  productIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个关联产品', trigger: 'change' }]
}

function resultType(result?: string) {
  if (result === 'SUCCESS') return 'success'
  if (result === 'PARTIAL') return 'warning'
  return 'danger'
}

function actionLabel(action: string) {
  const map: Record<string, string> = {
    PLAN: '创建计划',
    UPDATE: '修改计划',
    CHECKIN: '完成签到',
    SUBMIT: '提交记录',
    APPROVE: '审批通过',
    REJECT: '审批驳回',
    CANCEL: '取消拜访'
  }
  return map[action] || action
}

async function load() {
  loading.value = true
  try {
    visit.value = await visitApi.detail(visitId.value)
    const [contactList, productList] = await Promise.all([
      contactApi.list(visit.value.customerId),
      productApi.options()
    ])
    contacts.value = contactList
    products.value = productList
  } finally {
    loading.value = false
  }
}

function disableDate(date: Date) {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const end = new Date(start)
  end.setDate(end.getDate() + 30)
  return date < start || date > end
}

function openEdit() {
  if (!visit.value) return
  planForm.contactId = visit.value.contactId || null
  planForm.visitDate = visit.value.visitDate
  planForm.purpose = visit.value.purpose
  planForm.remark = visit.value.remark || ''
  editVisible.value = true
}

async function saveEdit() {
  if (!visit.value || !planFormRef.value) return
  const valid = await planFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await visitApi.update(visit.value.id, {
      customerId: visit.value.customerId,
      contactId: planForm.contactId || null,
      visitDate: planForm.visitDate,
      purpose: planForm.purpose,
      remark: planForm.remark || undefined
    })
    ElMessage.success('拜访计划已更新')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doCheckin() {
  if (!visit.value) return
  await ElMessageBox.confirm(
    `确认在「${visit.value.customerName}」完成签到吗？演示环境将模拟 60-180 米的定位距离。`,
    '拜访签到',
    { type: 'info', confirmButtonText: '确认签到', cancelButtonText: '取消' }
  )
  const updated = await visitApi.checkin(visit.value.id)
  visit.value = updated
  ElMessage.success('签到成功，请补充拜访记录')
  await load()
}

function openSubmit() {
  if (!visit.value) return
  submitForm.result = visit.value.result || 'SUCCESS'
  submitForm.feedback = visit.value.feedback || ''
  submitForm.nextStep = visit.value.nextStep || ''
  submitForm.productIds = visit.value.products?.map((item) => item.id) || []
  submitVisible.value = true
}

async function saveSubmit() {
  if (!visit.value || !submitFormRef.value) return
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await visitApi.submit(visit.value.id, { ...submitForm })
    ElMessage.success('拜访记录已提交，等待地区经理审批')
    submitVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doApprove() {
  if (!visit.value) return
  await ElMessageBox.confirm('确认审批通过该拜访记录吗？', '审批通过', {
    type: 'success',
    confirmButtonText: '通过',
    cancelButtonText: '取消'
  })
  await visitApi.approve(visit.value.id)
  ElMessage.success('拜访已审批通过')
  await load()
}

function openReason(mode: 'reject' | 'cancel') {
  reasonMode.value = mode
  reasonText.value = ''
  reasonVisible.value = true
}

async function saveReason() {
  if (!visit.value || reasonText.value.trim().length < 5) return
  saving.value = true
  try {
    if (reasonMode.value === 'reject') {
      await visitApi.reject(visit.value.id, reasonText.value.trim())
      ElMessage.success('拜访已驳回，代表可修改后重新提交')
    } else {
      await visitApi.cancel(visit.value.id, reasonText.value.trim())
      ElMessage.success('拜访已取消')
    }
    reasonVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await meta.load()
  await load()
})
</script>

<style scoped>
.back-row {
  margin-bottom: 6px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.tip {
  margin-bottom: 14px;
}
.detail-grid.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.span-2 {
  grid-column: span 2;
}
.record-block {
  margin-bottom: 16px;
}
.record-label {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 6px;
}
.record-text {
  margin: 0;
  color: var(--ink);
  line-height: 1.7;
  white-space: pre-wrap;
}
.product-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.product-chip {
  background: var(--brand-soft);
  color: var(--brand-dark);
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12.5px;
}
.product-chip small {
  color: #5c7a9d;
  margin-left: 4px;
}
.event-line {
  font-size: 13px;
  color: var(--ink-2);
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: baseline;
}
.event-line b {
  color: #173250;
}
</style>
