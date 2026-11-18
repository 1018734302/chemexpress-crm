<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="back-row">
          <el-button link :icon="ArrowLeft" @click="router.push('/orders')">返回订单列表</el-button>
        </div>
        <h1 class="title-row">
          {{ order?.code || '订单详情' }}
          <StatusTag v-if="order" :value="order.status" :label="order.statusName" />
        </h1>
        <p v-if="order">{{ order.customerName }} · 代表 {{ order.repName }} · {{ order.region }}</p>
      </div>
      <div v-if="order" class="page-actions">
        <el-button v-if="canEdit" :icon="Edit" @click="router.push(`/orders/${order.id}/edit`)">编辑</el-button>
        <el-button v-if="canSubmit" type="primary" :icon="Promotion" @click="doSubmit">提交审批</el-button>
        <el-button v-if="canApprove" type="success" :icon="CircleCheck" @click="doApprove">审批通过</el-button>
        <el-button v-if="canApprove" type="danger" plain :icon="CircleClose" @click="openReason('reject')">驳回</el-button>
        <el-button v-if="canShip" type="primary" :icon="Van" @click="shipVisible = true">登记发货</el-button>
        <el-button v-if="canPay" type="primary" :icon="Wallet" @click="openPayment">登记回款</el-button>
        <el-button v-if="canCancel" plain :icon="Close" @click="openReason('cancel')">取消订单</el-button>
        <el-button v-if="canDelete" type="danger" plain :icon="Delete" @click="doDelete">删除</el-button>
      </div>
    </div>

    <template v-if="order">
      <el-alert
        v-if="order.status === 'REJECTED' && order.rejectReason"
        type="error"
        :closable="false"
        show-icon
        class="tip"
        :title="`驳回原因：${order.rejectReason}`"
      />
      <el-alert
        v-if="order.status === 'CANCELLED' && order.cancelReason"
        type="warning"
        :closable="false"
        show-icon
        class="tip"
        :title="`取消原因：${order.cancelReason}`"
      />

      <div class="split-grid">
        <div class="panel">
          <div class="panel-title">
            <h3>订单信息</h3>
            <span class="sub">下单于 {{ dateTime(order.createdAt) }}</span>
          </div>
          <div class="detail-grid two">
            <div class="detail-item">
              <div class="label">下单客户</div>
              <div class="value">{{ order.customerName }}</div>
            </div>
            <div class="detail-item">
              <div class="label">负责代表</div>
              <div class="value">{{ order.repName }}（{{ order.region }}）</div>
            </div>
            <div class="detail-item">
              <div class="label">期望发货日期</div>
              <div class="value">{{ order.expectedDate || '—' }}</div>
            </div>
            <div class="detail-item">
              <div class="label">审批时间</div>
              <div class="value">{{ dateTime(order.approvedAt) }}</div>
            </div>
            <div class="detail-item span-2">
              <div class="label">收货地址</div>
              <div class="value">{{ order.receiverAddress || '—' }}</div>
            </div>
            <template v-if="order.logisticsCompany">
              <div class="detail-item">
                <div class="label">物流公司</div>
                <div class="value">{{ order.logisticsCompany }}</div>
              </div>
              <div class="detail-item">
                <div class="label">物流单号</div>
                <div class="value">{{ order.trackingNo }}</div>
              </div>
              <div class="detail-item span-2">
                <div class="label">发货时间</div>
                <div class="value">{{ dateTime(order.shippedAt) }}</div>
              </div>
            </template>
            <div v-if="order.remark" class="detail-item span-2">
              <div class="label">备注</div>
              <div class="value">{{ order.remark }}</div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>金额概览</h3>
          </div>
          <div class="amount-cards">
            <div class="amount-card">
              <div class="label">订单总额</div>
              <div class="value">{{ money(order.totalAmount) }}</div>
            </div>
            <div class="amount-card teal">
              <div class="label">已回款</div>
              <div class="value">{{ money(order.paidAmount) }}</div>
            </div>
            <div class="amount-card amber">
              <div class="label">未回款</div>
              <div class="value">{{ money(remaining) }}</div>
            </div>
          </div>
          <el-progress
            :percentage="payPercent"
            :stroke-width="10"
            :color="payPercent >= 100 ? '#1f7a4d' : '#0b4f9e'"
            class="pay-progress"
          />
          <div class="form-tip">{{ payPercent >= 100 ? '已全额回款，订单自动结案。' : '全额回款后订单将自动完成。' }}</div>
        </div>
      </div>

      <div class="panel section-gap">
        <div class="panel-title">
          <h3>产品明细</h3>
          <span class="sub">共 {{ order.items?.length || 0 }} 个产品</span>
        </div>
        <el-table :data="order.items" size="default">
          <el-table-column prop="productName" label="产品" min-width="160" />
          <el-table-column prop="productSpec" label="规格" min-width="140" />
          <el-table-column label="单价" width="120" align="right">
            <template #default="{ row }"><span class="money">{{ money(row.unitPrice) }}</span></template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="right" />
          <el-table-column label="折扣" width="90" align="right">
            <template #default="{ row }">{{ Number(row.discount) }}%</template>
          </el-table-column>
          <el-table-column label="小计" width="130" align="right">
            <template #default="{ row }"><span class="money strong">{{ money(row.amount) }}</span></template>
          </el-table-column>
          <template #empty>
            <div class="empty-tip">暂无产品明细</div>
          </template>
        </el-table>
        <div class="order-summary">
          <div class="summary-line">
            <span class="muted">订单总额</span>
            <b class="total">{{ money(order.totalAmount) }}</b>
          </div>
        </div>
      </div>

      <div class="split-grid section-gap">
        <div class="panel">
          <div class="panel-title">
            <h3>回款记录</h3>
            <span class="sub">{{ order.payments?.length || 0 }} 笔</span>
          </div>
          <el-table :data="order.payments" size="default">
            <el-table-column prop="payDate" label="回款日期" width="105" />
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }"><span class="money">{{ money(row.amount) }}</span></template>
            </el-table-column>
            <el-table-column prop="methodName" label="方式" width="80" />
            <el-table-column prop="referenceNo" label="流水号" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.referenceNo || '—' }}</template>
            </el-table-column>
            <el-table-column prop="createdByName" label="登记人" width="90" />
            <template #empty>
              <div class="empty-tip">暂无回款记录</div>
            </template>
          </el-table>
        </div>

        <div class="panel">
          <div class="panel-title">
            <h3>操作时间线</h3>
            <span class="sub">{{ order.events?.length || 0 }} 条</span>
          </div>
          <el-timeline v-if="order.events?.length">
            <el-timeline-item
              v-for="event in order.events"
              :key="event.id"
              :timestamp="dateTime(event.createdAt)"
              placement="top"
            >
              <div class="event-line">
                <b>{{ actionLabel(event.action) }}</b>
                <span>{{ event.detail }}</span>
              </div>
              <div class="muted small">{{ event.operatorName }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="empty-tip">暂无操作记录</div>
        </div>
      </div>
    </template>

    <el-dialog v-model="shipVisible" title="登记发货" width="460px" :close-on-click-modal="false">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select v-model="shipForm.logisticsCompany" filterable allow-create placeholder="请选择或输入">
            <el-option label="顺丰医药" value="顺丰医药" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="中集冷云" value="中集冷云" />
            <el-option label="华润医药物流" value="华润医药物流" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" maxlength="30" placeholder="6-30 位字母、数字或短横线" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paymentVisible" title="登记回款" width="480px" :close-on-click-modal="false">
      <el-form ref="paymentFormRef" :model="paymentForm" :rules="paymentRules" label-width="90px">
        <el-form-item label="回款金额" prop="amount">
          <el-input-number
            v-model="paymentForm.amount"
            :min="0.01"
            :max="remaining"
            :precision="2"
            :controls="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="回款日期" prop="payDate">
          <el-date-picker
            v-model="paymentForm.payDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="(date: Date) => date > new Date()"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="回款方式" prop="method">
          <el-select v-model="paymentForm.method">
            <el-option v-for="(label, key) in meta.options.paymentMethods" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="流水号">
          <el-input v-model="paymentForm.referenceNo" maxlength="60" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="2" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <div class="form-tip">当前未回款余额 {{ money(remaining) }}，本次登记后{{ paymentForm.amount >= remaining ? '将全额回款并自动结案' : '仍为部分回款' }}。</div>
      <template #footer>
        <el-button @click="paymentVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPayment">确认登记</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reasonVisible" :title="reasonMode === 'reject' ? '驳回订单' : '取消订单'" width="440px">
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
  Delete,
  Edit,
  Promotion,
  Van,
  Wallet
} from '@element-plus/icons-vue'
import { orderApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Order } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import { dateTime, money, today } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const saving = ref(false)
const order = ref<Order | null>(null)

const shipVisible = ref(false)
const shipFormRef = ref<FormInstance>()
const shipForm = reactive({ logisticsCompany: '顺丰医药', trackingNo: '' })
const shipRules = {
  logisticsCompany: [{ required: true, message: '请填写物流公司', trigger: 'change' }],
  trackingNo: [
    { required: true, message: '请填写物流单号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]{6,30}$/, message: '需为 6-30 位字母、数字或短横线', trigger: 'blur' }
  ]
}

const paymentVisible = ref(false)
const paymentFormRef = ref<FormInstance>()
const paymentForm = reactive<any>({ amount: 0.01, payDate: today(), method: 'TRANSFER', referenceNo: '', remark: '' })
const paymentRules = {
  amount: [{ required: true, message: '请输入回款金额', trigger: 'blur' }],
  payDate: [{ required: true, message: '请选择回款日期', trigger: 'change' }],
  method: [{ required: true, message: '请选择回款方式', trigger: 'change' }]
}

const reasonVisible = ref(false)
const reasonMode = ref<'reject' | 'cancel'>('reject')
const reasonText = ref('')

const orderId = computed(() => Number(route.params.id))
const remaining = computed(() => {
  if (!order.value) return 0
  return Math.max(0, Number(order.value.totalAmount) - Number(order.value.paidAmount))
})
const payPercent = computed(() => {
  if (!order.value || !order.value.totalAmount) return 0
  return Math.min(100, Math.round((Number(order.value.paidAmount) / Number(order.value.totalAmount)) * 100))
})
const isEditor = computed(
  () => Boolean(order.value && (auth.isOps || (auth.isMr && order.value.repId === auth.user?.id)))
)
const canEdit = computed(
  () =>
    isEditor.value &&
    (order.value?.status === 'DRAFT' || order.value?.status === 'REJECTED')
)
const canSubmit = computed(() => canEdit.value)
const canApprove = computed(
  () => Boolean(auth.isDsm && order.value?.status === 'PENDING' && order.value?.repId !== auth.user?.id)
)
const canShip = computed(() => auth.isOps && order.value?.status === 'APPROVED')
const canPay = computed(() => auth.isOps && order.value?.status === 'SHIPPED' && remaining.value > 0)
const canCancel = computed(() => {
  if (!order.value) return false
  if (!['DRAFT', 'PENDING', 'APPROVED'].includes(order.value.status)) return false
  return (
    auth.isAdmin ||
    auth.isOps ||
    (auth.isMr && order.value.repId === auth.user?.id) ||
    (auth.isDsm && order.value.region === auth.user?.region)
  )
})
const canDelete = computed(
  () =>
    Boolean(
      order.value?.status === 'DRAFT' &&
        (auth.isAdmin || auth.isOps || (auth.isMr && order.value.repId === auth.user?.id))
    )
)

function actionLabel(action: string) {
  const map: Record<string, string> = {
    CREATE: '创建订单',
    UPDATE: '更新订单',
    SUBMIT: '提交订单',
    APPROVE: '审批通过',
    REJECT: '审批驳回',
    SHIP: '登记发货',
    PAYMENT: '登记回款',
    COMPLETE: '订单结案',
    CANCEL: '取消订单',
    DELETE: '删除订单'
  }
  return map[action] || action
}

async function load() {
  loading.value = true
  try {
    order.value = await orderApi.detail(orderId.value)
  } finally {
    loading.value = false
  }
}

async function doSubmit() {
  if (!order.value) return
  await ElMessageBox.confirm('确认提交该订单进入审批吗？', '提交订单', {
    type: 'info',
    confirmButtonText: '提交',
    cancelButtonText: '取消'
  })
  await orderApi.submit(order.value.id)
  ElMessage.success('订单已提交，等待地区经理审批')
  await load()
}

async function doApprove() {
  if (!order.value) return
  await ElMessageBox.confirm('确认审批通过该订单吗？通过后将进入待发货。', '审批通过', {
    type: 'success',
    confirmButtonText: '通过',
    cancelButtonText: '取消'
  })
  await orderApi.approve(order.value.id)
  ElMessage.success('订单已审批通过')
  await load()
}

async function doShip() {
  if (!order.value || !shipFormRef.value) return
  const valid = await shipFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await orderApi.ship(order.value.id, { ...shipForm })
    ElMessage.success('发货登记成功')
    shipVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openPayment() {
  paymentForm.amount = remaining.value
  paymentForm.payDate = today()
  paymentForm.method = 'TRANSFER'
  paymentForm.referenceNo = ''
  paymentForm.remark = ''
  paymentVisible.value = true
}

async function doPayment() {
  if (!order.value || !paymentFormRef.value) return
  const valid = await paymentFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await orderApi.payment(order.value.id, { ...paymentForm })
    ElMessage.success(paymentForm.amount >= remaining.value ? '回款登记完成，订单已结案' : '回款登记成功')
    paymentVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openReason(mode: 'reject' | 'cancel') {
  reasonMode.value = mode
  reasonText.value = ''
  reasonVisible.value = true
}

async function saveReason() {
  if (!order.value || reasonText.value.trim().length < 5) return
  saving.value = true
  try {
    if (reasonMode.value === 'reject') {
      await orderApi.reject(order.value.id, reasonText.value.trim())
      ElMessage.success('订单已驳回')
    } else {
      await orderApi.cancel(order.value.id, reasonText.value.trim())
      ElMessage.success('订单已取消')
    }
    reasonVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  if (!order.value) return
  await ElMessageBox.confirm(`确认删除草稿订单「${order.value.code}」吗？该操作不可恢复。`, '删除订单', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await orderApi.remove(order.value.id)
  ElMessage.success('订单已删除')
  router.replace('/orders')
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
.amount-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}
.amount-card {
  background: #f7fafd;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px;
}
.amount-card.teal {
  background: var(--teal-soft);
  border-color: #c7eae3;
}
.amount-card.amber {
  background: #fff8ec;
  border-color: #f3e1c2;
}
.amount-card .label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}
.amount-card .value {
  font-size: 17px;
  font-weight: 700;
  color: #12324f;
  font-variant-numeric: tabular-nums;
}
.pay-progress {
  margin-top: 16px;
}
.order-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}
.summary-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.summary-line .total {
  font-size: 20px;
  color: var(--brand);
  font-variant-numeric: tabular-nums;
}
.event-line {
  font-size: 13px;
  color: var(--ink-2);
}
.event-line b {
  color: #173250;
  margin-right: 8px;
}
.strong {
  font-weight: 700;
  color: #12324f;
}
</style>
