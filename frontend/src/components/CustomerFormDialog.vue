<template>
  <el-dialog
    v-model="visible"
    :title="customer ? '编辑客户' : '新建客户'"
    width="660px"
    :close-on-click-modal="false"
    @closed="reset"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <div class="form-grid">
        <el-form-item label="客户名称" prop="name" class="span-2">
          <el-input v-model="form.name" maxlength="60" placeholder="如：上海示范第一医院" />
        </el-form-item>
        <el-form-item label="客户类型" prop="type">
          <el-select v-model="form.type" :disabled="Boolean(customer)" placeholder="请选择">
            <el-option v-for="(label, key) in meta.options.customerTypes" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户等级" prop="level">
          <el-radio-group v-model="form.level">
            <el-radio-button value="A">A 级</el-radio-button>
            <el-radio-button value="B">B 级</el-radio-button>
            <el-radio-button value="C">C 级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="区域">
          <el-input :model-value="form.region" disabled />
        </el-form-item>
        <el-form-item label="负责人" prop="ownerId">
          <el-select v-if="auth.isDsm" v-model="form.ownerId" placeholder="请选择代表">
            <el-option
              v-for="owner in regionOwners"
              :key="owner.id"
              :label="owner.name"
              :value="owner.id"
            />
          </el-select>
          <el-input v-else :model-value="`${auth.user?.name}（本人）`" disabled />
        </el-form-item>
        <el-form-item label="客户状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option
              v-for="(label, key) in meta.options.customerStatuses"
              :key="key"
              :label="label"
              :value="key"
              :disabled="key === 'CHURNED' && auth.isMr"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年采购潜力" prop="annualPotential">
          <el-input-number
            v-model="form.annualPotential"
            :min="0"
            :max="999999999"
            :precision="0"
            :controls="false"
            placeholder="元"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="客户地址" prop="address" class="span-2">
          <el-input v-model="form.address" maxlength="120" placeholder="省市区 + 详细地址" />
        </el-form-item>
        <el-form-item label="客户来源">
          <el-select v-model="form.source" placeholder="请选择">
            <el-option label="陌拜" value="陌拜" />
            <el-option label="转介绍" value="转介绍" />
            <el-option label="市场活动" value="市场活动" />
            <el-option label="商业推荐" value="商业推荐" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" maxlength="200" placeholder="逗号分隔，最多 5 个" />
        </el-form-item>
        <el-form-item v-if="form.status === 'CHURNED'" label="流失原因" prop="churnReason" class="span-2">
          <el-input v-model="form.churnReason" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" class="span-2">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </div>
      <div class="form-tip">A 级客户必须填写大于 0 的年采购潜力；客户类型创建后不可修改。</div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { customerApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { Customer } from '@/types'

const props = defineProps<{
  modelValue: boolean
  customer?: Customer | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved', customer: Customer): void
}>()

const auth = useAuthStore()
const meta = useMetaStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = reactive<any>({
  name: '',
  type: 'HOSPITAL',
  level: 'C',
  region: auth.region,
  ownerId: null,
  status: 'POTENTIAL',
  address: '',
  annualPotential: undefined,
  tags: '',
  source: '陌拜',
  remark: '',
  churnReason: ''
})

const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择客户等级', trigger: 'change' }],
  status: [{ required: true, message: '请选择客户状态', trigger: 'change' }],
  address: [{ required: true, message: '请输入客户地址', trigger: 'blur' }],
  ownerId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  annualPotential: [
    {
      validator: (_rule: any, value: any, callback: any) => {
        if (form.level === 'A' && (!value || Number(value) <= 0)) {
          callback(new Error('A 级客户必须填写年采购潜力'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const regionOwners = computed(() => meta.options.owners.filter((owner) => owner.region === form.region))

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    meta.load()
    const source = props.customer
    form.name = source?.name || ''
    form.type = source?.type || 'HOSPITAL'
    form.level = source?.level || 'C'
    form.region = source?.region || auth.region
    form.ownerId = source?.ownerId || (auth.isMr ? auth.user?.id : null)
    form.status = source?.status || 'POTENTIAL'
    form.address = source?.address || ''
    form.annualPotential = source?.annualPotential ?? undefined
    form.tags = source?.tags || ''
    form.source = source?.source || '陌拜'
    form.remark = source?.remark || ''
    form.churnReason = ''
  }
)

function reset() {
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (auth.isMr) payload.ownerId = auth.user?.id
    const result = props.customer
      ? await customerApi.update(props.customer.id, payload)
      : await customerApi.create(payload)
    ElMessage.success(props.customer ? '客户已更新' : '客户创建成功')
    emit('saved', result)
    visible.value = false
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}
.span-2 {
  grid-column: span 2;
}
:deep(.el-select),
:deep(.el-input) {
  width: 100%;
}
</style>
