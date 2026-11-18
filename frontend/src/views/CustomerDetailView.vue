<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="back-row">
          <el-button link :icon="ArrowLeft" @click="router.push('/customers')">返回客户列表</el-button>
        </div>
        <h1 class="title-row">
          {{ customer?.name || '客户详情' }}
          <span v-if="customer" class="level" :class="`level-${customer.level}`">{{ customer.level }}</span>
          <StatusTag v-if="customer" :value="customer.status" :label="customer.statusName" />
        </h1>
        <p>
          {{ customer?.code }} · {{ customer?.typeName }} · {{ customer?.region }} · 负责人
          {{ customer?.ownerName }}
        </p>
      </div>
      <div class="page-actions">
        <el-button v-if="customer && canWrite" type="primary" :icon="Edit" @click="editVisible = true">
          编辑客户
        </el-button>
        <el-button v-if="customer && (auth.isDsm || auth.isAdmin)" :icon="Switch" @click="openTransfer">
          转交
        </el-button>
        <el-button v-if="customer && auth.isMr" :icon="Calendar" @click="createVisit">新建拜访</el-button>
        <el-button
          v-if="customer && auth.canCreateOrder && customer.status === 'ACTIVE'"
          :icon="Document"
          @click="createOrder"
        >
          新建订单
        </el-button>
      </div>
    </div>

    <template v-if="customer">
      <div class="panel">
        <div class="panel-title">
          <h3>客户档案</h3>
          <span class="sub">最近拜访 {{ customer.lastVisitDate || '暂无记录' }}</span>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <div class="label">客户类型</div>
            <div class="value">{{ customer.typeName }}</div>
          </div>
          <div class="detail-item">
            <div class="label">客户等级</div>
            <div class="value">{{ customer.level }} 级</div>
          </div>
          <div class="detail-item">
            <div class="label">年采购潜力</div>
            <div class="value money">{{ customer.annualPotential ? money(customer.annualPotential) : '—' }}</div>
          </div>
          <div class="detail-item">
            <div class="label">客户来源</div>
            <div class="value">{{ customer.source || '—' }}</div>
          </div>
          <div class="detail-item">
            <div class="label">客户标签</div>
            <div class="value">
              <template v-if="customer.tags">
                <span v-for="tag in customer.tags.split(',')" :key="tag" class="tag-soft">{{ tag }}</span>
              </template>
              <template v-else>—</template>
            </div>
          </div>
          <div class="detail-item">
            <div class="label">创建时间</div>
            <div class="value">{{ dateTime(customer.createdAt) }}</div>
          </div>
          <div class="detail-item span-3">
            <div class="label">客户地址</div>
            <div class="value">{{ customer.address }}</div>
          </div>
          <div v-if="customer.remark" class="detail-item span-3">
            <div class="label">备注</div>
            <div class="value">{{ customer.remark }}</div>
          </div>
        </div>
      </div>

      <div class="panel section-gap">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="联系人" name="contacts">
            <div class="tab-toolbar">
              <span class="muted small">共 {{ contacts.length }} 位联系人，首位为主联系人</span>
              <el-button v-if="canWrite" size="small" type="primary" :icon="Plus" @click="openContact()">
                新增联系人
              </el-button>
            </div>
            <el-table :data="contacts" size="default">
              <el-table-column prop="name" label="姓名" width="120">
                <template #default="{ row }">
                  <b>{{ row.name }}</b>
                  <el-tag v-if="row.primaryContact" size="small" type="success" effect="plain" class="ml6">
                    主联系人
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="职务" width="130" />
              <el-table-column prop="roleName" label="角色" width="100" />
              <el-table-column label="手机号" width="140">
                <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
              </el-table-column>
              <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                    {{ row.status === 'ACTIVE' ? '在职' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
              <el-table-column v-if="canWrite" label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" @click="openContact(row)">编辑</el-button>
                    <el-button link type="danger" @click="removeContact(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-tip">暂无联系人，请先补充关键决策人与采购联系人。</div>
              </template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`拜访记录（${visitTotal}）`" name="visits">
            <el-table :data="visits" size="default" @row-click="(row: Visit) => router.push(`/visits/${row.id}`)">
              <el-table-column prop="code" label="拜访编号" width="140" />
              <el-table-column prop="visitDate" label="拜访日期" width="110" />
              <el-table-column prop="purposeName" label="拜访目的" width="110" />
              <el-table-column prop="contactName" label="联系人" width="100">
                <template #default="{ row }">{{ row.contactName || '—' }}</template>
              </el-table-column>
              <el-table-column prop="repName" label="代表" width="90" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label="row.statusName" />
                </template>
              </el-table-column>
              <el-table-column label="签到时间" width="150">
                <template #default="{ row }">{{ dateTime(row.checkinTime) }}</template>
              </el-table-column>
              <template #empty>
                <div class="empty-tip">该客户暂无拜访记录。</div>
              </template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`订单记录（${orderTotal}）`" name="orders">
            <el-table :data="orders" size="default" @row-click="(row: Order) => router.push(`/orders/${row.id}`)">
              <el-table-column prop="code" label="订单编号" width="150" />
              <el-table-column label="订单金额" width="130" align="right">
                <template #default="{ row }">
                  <span class="money">{{ money(row.totalAmount) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="已回款" width="130" align="right">
                <template #default="{ row }">
                  <span class="money">{{ money(row.paidAmount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="repName" label="代表" width="90" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag :value="row.status" :label="row.statusName" />
                </template>
              </el-table-column>
              <el-table-column label="创建时间" width="150">
                <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
              </el-table-column>
              <template #empty>
                <div class="empty-tip">该客户暂无订单记录。</div>
              </template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <CustomerFormDialog v-model="editVisible" :customer="customer" @saved="onSaved" />

    <el-dialog v-model="transferVisible" title="转交客户负责人" width="420px">
      <p class="muted small">将 <b>{{ customer?.name }}</b> 转交给同区域的医药代表，历史单据不受影响。</p>
      <el-select v-model="transferOwnerId" placeholder="请选择新的负责人" style="width: 100%; margin-top: 10px">
        <el-option
          v-for="owner in transferOwners"
          :key="owner.id"
          :label="`${owner.name}（${owner.region}）`"
          :value="owner.id"
          :disabled="owner.id === customer?.ownerId"
        />
      </el-select>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!transferOwnerId" @click="confirmTransfer">确认转交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="contactVisible"
      :title="editingContact ? '编辑联系人' : '新增联系人'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="contactForm.name" maxlength="40" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="职务">
          <el-input v-model="contactForm.title" maxlength="40" placeholder="如：药剂科主任" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="contactForm.role" placeholder="请选择">
            <el-option v-for="(label, key) in meta.options.contactRoles" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="contactForm.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="contactForm.email" maxlength="80" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="contactForm.status">
            <el-radio-button value="ACTIVE">在职</el-radio-button>
            <el-radio-button value="INACTIVE">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="contactForm.primaryContact" active-text="设为主联系人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="contactForm.remark" type="textarea" :rows="2" maxlength="300" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactSaving" @click="saveContact">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { ArrowLeft, Calendar, Document, Edit, Plus, Switch } from '@element-plus/icons-vue'
import { contactApi, customerApi, orderApi, visitApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Contact, Customer, Order, Visit } from '@/types'
import StatusTag from '@/components/StatusTag.vue'
import CustomerFormDialog from '@/components/CustomerFormDialog.vue'
import { dateTime, maskPhone, money } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const meta = useMetaStore()
const loading = ref(false)
const customer = ref<Customer | null>(null)
const contacts = ref<Contact[]>([])
const visits = ref<Visit[]>([])
const orders = ref<Order[]>([])
const visitTotal = ref(0)
const orderTotal = ref(0)
const activeTab = ref('contacts')
const editVisible = ref(false)
const transferVisible = ref(false)
const transferOwnerId = ref<number | null>(null)

const contactVisible = ref(false)
const contactSaving = ref(false)
const contactFormRef = ref<FormInstance>()
const editingContact = ref<Contact | null>(null)
const contactForm = reactive<any>({
  name: '',
  title: '',
  role: 'DECISION',
  phone: '',
  email: '',
  primaryContact: false,
  status: 'ACTIVE',
  remark: ''
})

const customerId = computed(() => Number(route.params.id))
const canWrite = computed(
  () => auth.canWriteCustomer && (auth.isDsm || customer.value?.ownerId === auth.user?.id)
)
const transferOwners = computed(() =>
  meta.options.owners.filter((owner) => owner.region === customer.value?.region)
)

const contactRules = {
  name: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择联系人角色', trigger: 'change' }],
  phone: [{ pattern: /^\d{11}$/, message: '手机号需为 11 位数字', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    customer.value = await customerApi.detail(customerId.value)
    const [contactList, visitPage, orderPage] = await Promise.all([
      contactApi.list(customerId.value),
      visitApi.page({ customerId: customerId.value, page: 1, size: 10 }),
      orderApi.page({ keyword: customer.value.name, page: 1, size: 10 })
    ])
    contacts.value = contactList
    visits.value = visitPage.records
    visitTotal.value = visitPage.total
    orders.value = orderPage.records
    orderTotal.value = orderPage.total
  } finally {
    loading.value = false
  }
}

function onSaved(saved: Customer) {
  customer.value = saved
  load()
}

function openTransfer() {
  transferOwnerId.value = null
  transferVisible.value = true
}

async function confirmTransfer() {
  if (!customer.value || !transferOwnerId.value) return
  await customerApi.transfer(customer.value.id, transferOwnerId.value)
  ElMessage.success('客户负责人已转交')
  transferVisible.value = false
  load()
}

function createVisit() {
  router.push({ path: '/visits', query: { customerId: String(customerId.value), create: '1' } })
}

function createOrder() {
  router.push({ path: '/orders/new', query: { customerId: String(customerId.value) } })
}

function openContact(row?: Contact) {
  editingContact.value = row || null
  contactForm.name = row?.name || ''
  contactForm.title = row?.title || ''
  contactForm.role = row?.role || 'DECISION'
  contactForm.phone = row?.phone || ''
  contactForm.email = row?.email || ''
  contactForm.primaryContact = row?.primaryContact || false
  contactForm.status = row?.status || 'ACTIVE'
  contactForm.remark = row?.remark || ''
  contactVisible.value = true
}

async function saveContact() {
  if (!contactFormRef.value) return
  const valid = await contactFormRef.value.validate().catch(() => false)
  if (!valid) return
  contactSaving.value = true
  try {
    const payload = { ...contactForm, phone: contactForm.phone || null, email: contactForm.email || null }
    if (editingContact.value) {
      await contactApi.update(customerId.value, editingContact.value.id, payload)
      ElMessage.success('联系人已更新')
    } else {
      await contactApi.create(customerId.value, payload)
      ElMessage.success('联系人已新增')
    }
    contactVisible.value = false
    await load()
  } finally {
    contactSaving.value = false
  }
}

async function removeContact(row: Contact) {
  await ElMessageBox.confirm(`确认删除联系人「${row.name}」吗？被拜访记录引用的联系人不能删除。`, '删除联系人', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await contactApi.remove(customerId.value, row.id)
  ElMessage.success('联系人已删除')
  load()
}

onMounted(async () => {
  await meta.load()
  load()
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
.span-3 {
  grid-column: span 3;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.ml6 {
  margin-left: 6px;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
