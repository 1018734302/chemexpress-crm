<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>维护平台账号、角色、区域与启用状态，仅系统管理员可访问</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建用户</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          class="grow"
          placeholder="搜索账号、姓名或手机号"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.role" placeholder="角色" clearable style="width: 130px" @change="search">
          <el-option v-for="(label, key) in meta.options.roles" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="filters.region" placeholder="区域" clearable style="width: 110px" @change="search">
          <el-option v-for="(label, key) in meta.options.regions" :key="key" :label="label" :value="key" />
        </el-select>
        <el-select v-model="filters.enabled" placeholder="状态" clearable style="width: 110px" @change="search">
          <el-option label="启用" value="true" />
          <el-option label="停用" value="false" />
        </el-select>
        <el-button :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows">
        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="user-name">
              {{ row.name }}
              <el-tag v-if="row.id === auth.user?.id" size="small" type="primary" effect="plain">当前账号</el-tag>
            </div>
            <div class="user-account">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)" effect="light">{{ row.roleName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="区域" width="90" />
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openPassword(row)">重置密码</el-button>
              <el-button
                link
                :type="row.enabled ? 'danger' : 'success'"
                :disabled="row.id === auth.user?.id"
                @click="toggleStatus(row)"
              >
                {{ row.enabled ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">未找到匹配账号，试试调整筛选条件。</div>
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

    <el-dialog
      v-model="formVisible"
      :title="editing ? '编辑用户' : '新建用户'"
      width="520px"
      :close-on-click-modal="false"
      @closed="formRef?.clearValidate()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="Boolean(editing)" placeholder="如 mr.wang" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" :disabled="isSelf" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="(label, key) in meta.options.roles" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域" prop="region">
          <el-select v-model="form.region" placeholder="请选择区域" style="width: 100%">
            <el-option v-for="(label, key) in meta.options.regions" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :disabled="isSelf" active-text="启用" inactive-text="停用" />
          <span v-if="isSelf" class="form-hint">不能修改自己的角色或停用当前账号</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">
          {{ editing ? '保存修改' : '创建用户' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="430px" :close-on-click-modal="false">
      <p class="muted small reset-tip">
        为 <b>{{ pwdTarget?.name }}</b>（{{ pwdTarget?.username }}）设置新密码，保存后旧密码立即失效。
      </p>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="86px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useMetaStore } from '@/stores/meta'
import type { UserRow } from '@/types'
import { dateTime } from '@/utils/format'

const auth = useAuthStore()
const meta = useMetaStore()

const rows = ref<UserRow[]>([])
const loading = ref(false)
const saving = ref(false)
const filters = reactive({ keyword: '', role: '', region: '', enabled: '' })
const pager = reactive({ page: 1, size: 20, total: 0 })

const formVisible = ref(false)
const editing = ref<UserRow | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  name: '',
  role: '',
  region: '',
  phone: '',
  password: '',
  enabled: true
})

const isSelf = computed(() => Boolean(editing.value && editing.value.id === auth.user?.id))

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      {
        pattern: /^[a-zA-Z0-9._-]{3,30}$/,
        message: '3-30 位字母、数字、点、下划线或短横线',
        trigger: 'blur'
      }
    ],
    name: [
      { required: true, message: '请输入姓名', trigger: 'blur' },
      { min: 2, max: 20, message: '姓名需为 2-20 个字符', trigger: 'blur' }
    ],
    role: [{ required: true, message: '请选择角色', trigger: 'change' }],
    region: [{ required: true, message: '请选择区域', trigger: 'change' }],
    phone: [{ max: 20, message: '手机号不能超过 20 个字符', trigger: 'blur' }]
  }
  if (!editing.value) {
    base.password = [
      { required: true, message: '请输入初始密码', trigger: 'blur' },
      { min: 6, max: 32, message: '密码需为 6-32 个字符', trigger: 'blur' }
    ]
  }
  return base
})

const pwdVisible = ref(false)
const pwdTarget = ref<UserRow | null>(null)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ password: '', confirm: '' })
const pwdRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码需为 6-32 个字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  loading.value = true
  try {
    const result = await userApi.page({
      keyword: filters.keyword || undefined,
      role: filters.role || undefined,
      region: filters.region || undefined,
      enabled: filters.enabled === '' ? undefined : filters.enabled === 'true',
      page: pager.page,
      size: pager.size
    })
    rows.value = result.records
    pager.total = result.total
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
  filters.role = ''
  filters.region = ''
  filters.enabled = ''
  search()
}

function roleType(role: string): 'danger' | 'warning' | 'success' | 'info' {
  if (role === 'ADMIN') return 'danger'
  if (role === 'DSM') return 'warning'
  if (role === 'MR') return 'success'
  return 'info'
}

function openCreate() {
  editing.value = null
  form.username = ''
  form.name = ''
  form.role = ''
  form.region = ''
  form.phone = ''
  form.password = ''
  form.enabled = true
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: UserRow) {
  editing.value = row
  form.username = row.username
  form.name = row.name
  form.role = row.role
  form.region = row.region
  form.phone = row.phone || ''
  form.password = ''
  form.enabled = row.enabled
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      name: form.name,
      role: form.role,
      region: form.region,
      phone: form.phone,
      enabled: form.enabled
    }
    if (editing.value) {
      await userApi.update(editing.value.id, payload)
      ElMessage.success('用户信息已更新')
      if (isSelf.value) {
        await auth.fetchMe()
      }
    } else {
      await userApi.create({ ...payload, username: form.username, password: form.password })
      ElMessage.success('用户已创建')
    }
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openPassword(row: UserRow) {
  pwdTarget.value = row
  pwdForm.password = ''
  pwdForm.confirm = ''
  pwdVisible.value = true
  nextTick(() => pwdFormRef.value?.clearValidate())
}

async function submitPassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid || !pwdTarget.value) return
  saving.value = true
  try {
    await userApi.resetPassword(pwdTarget.value.id, pwdForm.password)
    ElMessage.success('密码已重置，请通知用户使用新密码登录')
    pwdVisible.value = false
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: UserRow) {
  const action = row.enabled ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}账号「${row.name}（${row.username}）」？${row.enabled ? '停用后该账号将无法登录。' : ''}`,
      `${action}账号`,
      { type: 'warning', confirmButtonText: `确认${action}`, cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await userApi.update(row.id, {
    name: row.name,
    role: row.role,
    region: row.region,
    phone: row.phone,
    enabled: !row.enabled
  })
  ElMessage.success(`已${action}`)
  await load()
}

onMounted(async () => {
  await meta.load()
  await load()
})
</script>

<style scoped>
.user-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #16283f;
}
.user-account {
  margin-top: 1px;
  font-size: 12px;
  color: #8a99ab;
}
.form-hint {
  margin-left: 10px;
  font-size: 12px;
  color: #8a99ab;
}
.reset-tip {
  margin: 0 0 16px;
}
</style>
