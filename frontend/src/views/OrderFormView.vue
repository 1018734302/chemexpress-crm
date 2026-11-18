<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="back-row">
          <el-button link :icon="ArrowLeft" @click="router.push('/orders')">返回订单列表</el-button>
        </div>
        <h1>{{ isEdit ? '编辑订单' : '新建订单' }}</h1>
        <p>{{ isEdit ? `订单 ${orderCode}` : '订单金额由产品单价 × 数量 ×（1 - 折扣）自动计算' }}</p>
      </div>
    </div>

    <el-alert
      v-if="isEdit && !editable"
      type="warning"
      :closable="false"
      show-icon
      title="只有草稿或已驳回的订单可以编辑，当前订单不可编辑。"
      class="tip"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="92px" :disabled="!editable">
      <div class="panel">
        <div class="panel-title">
          <h3>订单信息</h3>
          <span class="sub">仅合作中的客户可以下单</span>
        </div>
        <el-row :gutter="18">
          <el-col :span="8">
            <el-form-item label="下单客户" prop="customerId">
              <div class="customer-field">
                <el-select
                  v-model="form.customerId"
                  filterable
                  placeholder="请选择合作中的客户"
                  :disabled="isEdit"
                  style="width: 100%"
                  @change="onCustomerChange"
                >
                  <el-option
                    v-for="item in customers"
                    :key="item.id"
                    :label="`${item.name}（${item.region}）`"
                    :value="item.id"
                    :disabled="item.status !== 'ACTIVE'"
                  >
                    <div class="customer-option">
                      <span>{{ item.name }}（{{ item.region }}）</span>
                      <small>{{ item.statusName }}</small>
                    </div>
                  </el-option>
                </el-select>
                <div class="form-tip">
                  仅「合作中」客户可下单；新建客户默认为「潜在客户」，请先在客户详情将状态改为「合作中」。
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望发货">
              <el-date-picker
                v-model="form.expectedDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="今天起 90 天内"
                :disabled-date="disableDate"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货地址">
              <el-input v-model="form.receiverAddress" maxlength="120" placeholder="留空则使用客户地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="订单备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="panel section-gap">
        <div class="panel-title">
          <h3>产品明细</h3>
          <el-button size="small" type="primary" plain :icon="Plus" @click="addItem">添加产品</el-button>
        </div>
        <el-table :data="form.items" size="default">
          <el-table-column label="产品" min-width="250">
            <template #default="{ row }">
              <el-select v-model="row.productId" filterable placeholder="请选择产品" @change="(id: number) => onProductChange(row, id)">
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="`${product.tradeName} · ${product.spec}`"
                  :value="product.id"
                >
                  <div class="product-option">
                    <span>{{ product.tradeName }} · {{ product.spec }}</span>
                    <small>{{ money(product.price) }}</small>
                  </div>
                </el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="单价（元）" width="150">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="99999" :precision="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="折扣（%）" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.discount" :min="0" :max="100" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="140" align="right">
            <template #default="{ row }">
              <span class="money strong">{{ money(rowAmount(row)) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ $index }">
              <el-button link type="danger" @click="removeItem($index)">移除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="empty-tip">请点击“添加产品”选择订单产品。</div>
          </template>
        </el-table>

        <div class="order-summary">
          <div class="summary-line">
            <span class="muted">产品行数</span>
            <b>{{ form.items.length }}</b>
          </div>
          <div class="summary-line">
            <span class="muted">订单总额</span>
            <b class="total">{{ money(totalAmount) }}</b>
          </div>
        </div>
      </div>
    </el-form>

    <div class="footer-actions">
      <el-button @click="router.push('/orders')">取消</el-button>
      <el-button :disabled="!editable" :loading="saving" @click="save(false)">保存草稿</el-button>
      <el-button type="primary" :disabled="!editable" :loading="saving" @click="save(true)">保存并提交</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { customerApi, orderApi, productApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { Customer, Product } from '@/types'
import { money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const customers = ref<Customer[]>([])
const products = ref<Product[]>([])
const editable = ref(true)
const orderCode = ref('')

const isEdit = computed(() => Boolean(route.params.id))
const orderId = computed(() => Number(route.params.id))

const form = reactive<any>({
  customerId: null,
  receiverAddress: '',
  expectedDate: '',
  remark: '',
  items: [] as any[]
})

const rules = {
  customerId: [{ required: true, message: '请选择下单客户', trigger: 'change' }]
}

const totalAmount = computed(() =>
  form.items.reduce((sum: number, item: any) => sum + rowAmount(item), 0)
)

function rowAmount(row: any) {
  const gross = Number(row.unitPrice || 0) * Number(row.quantity || 0)
  const amount = gross * (1 - Number(row.discount || 0) / 100)
  return Math.round(amount * 100) / 100
}

function disableDate(date: Date) {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const end = new Date(start)
  end.setDate(end.getDate() + 90)
  return date < start || date > end
}

function addItem() {
  form.items.push({ productId: null, quantity: 1, unitPrice: 0, discount: 0 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function onProductChange(row: any, productId: number) {
  const product = products.value.find((item) => item.id === productId)
  if (product) {
    row.unitPrice = Number(product.price)
    row.quantity = row.quantity || 1
    row.discount = row.discount ?? 0
  }
}

function onCustomerChange(customerId: number) {
  const customer = customers.value.find((item) => item.id === customerId)
  if (customer && !form.receiverAddress) {
    form.receiverAddress = customer.address
  }
}

async function load() {
  loading.value = true
  try {
    const [customerPage, productList] = await Promise.all([
      customerApi.page({ page: 1, size: 100 }),
      productApi.options()
    ])
    customers.value = customerPage.records
    products.value = productList

    if (isEdit.value) {
      const order = await orderApi.detail(orderId.value)
      orderCode.value = order.code
      editable.value = order.status === 'DRAFT' || order.status === 'REJECTED'
      form.customerId = order.customerId
      form.receiverAddress = order.receiverAddress || ''
      form.expectedDate = order.expectedDate || ''
      form.remark = order.remark || ''
      form.items = (order.items || []).map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: Number(item.unitPrice),
        discount: Number(item.discount)
      }))
      if (!form.items.length) addItem()
      if (!customers.value.some((item) => item.id === order.customerId)) {
        customers.value.unshift(await customerApi.detail(order.customerId))
      }
    } else {
      const preset = Number(route.query.customerId)
      if (preset) {
        form.customerId = preset
        onCustomerChange(preset)
      }
      addItem()
    }
  } finally {
    loading.value = false
  }
}

function validateItems(): string | null {
  if (!form.items.length) return '请至少添加一个产品'
  const ids = new Set<number>()
  for (const item of form.items) {
    if (!item.productId) return '请为每一行选择产品'
    if (ids.has(item.productId)) return '同一产品不能重复添加'
    ids.add(item.productId)
    if (!item.quantity || item.quantity < 1) return '产品数量至少为 1'
    if (!item.unitPrice || item.unitPrice < 0.01) return '产品单价至少为 0.01'
    if (item.discount < 0 || item.discount > 100) return '折扣需在 0-100 之间'
  }
  return null
}

async function save(submit: boolean) {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const itemError = validateItems()
  if (itemError) {
    ElMessage.warning(itemError)
    return
  }
  saving.value = true
  try {
    const payload = {
      customerId: form.customerId,
      items: form.items.map((item: any) => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discount: item.discount
      })),
      receiverAddress: form.receiverAddress || undefined,
      expectedDate: form.expectedDate || undefined,
      remark: form.remark || undefined,
      submit
    }
    const result = isEdit.value
      ? await orderApi.update(orderId.value, payload)
      : await orderApi.create(payload)
    ElMessage.success(submit ? '订单已提交审批' : '订单已保存为草稿')
    router.replace(`/orders/${result.id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.back-row {
  margin-bottom: 6px;
}
.tip {
  margin-bottom: 14px;
}
.product-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.product-option small {
  color: var(--muted);
}
.customer-field {
  width: 100%;
}
.customer-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.customer-option small {
  color: var(--muted);
}
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #8a99ab;
}
.order-summary {
  display: flex;
  justify-content: flex-end;
  gap: 36px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}
.summary-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.summary-line b {
  font-size: 16px;
  color: #14324f;
  font-variant-numeric: tabular-nums;
}
.summary-line .total {
  font-size: 22px;
  color: var(--brand);
}
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 18px;
}
.strong {
  font-weight: 700;
  color: #12324f;
}
:deep(.el-form-item) {
  margin-bottom: 16px;
}
</style>
