<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>产品中心</h1>
        <p>产品资料只读，用于拜访关联与订单下单，停售产品不可下单</p>
      </div>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          class="grow"
          placeholder="搜索产品商品名、通用名或编码"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="filters.status" placeholder="产品状态" clearable style="width: 130px" @change="search">
          <el-option label="在售" value="ACTIVE" />
          <el-option label="停售" value="INACTIVE" />
        </el-select>
        <el-button :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" size="default">
        <el-table-column prop="code" label="产品编码" width="120" />
        <el-table-column label="商品名" min-width="150">
          <template #default="{ row }">
            <b class="product-name">{{ row.tradeName }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="genericName" label="通用名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" min-width="130" />
        <el-table-column prop="dosageForm" label="剂型" width="100" />
        <el-table-column prop="insuranceType" label="医保类型" width="120" />
        <el-table-column label="参考单价" width="120" align="right">
          <template #default="{ row }">
            <span class="money strong">{{ money(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '在售' : '停售' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="indication" label="适应症" min-width="220" show-overflow-tooltip />
        <template #empty>
          <div class="empty-tip">未找到匹配产品。</div>
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
import { Refresh, Search } from '@element-plus/icons-vue'
import { productApi } from '@/api'
import type { Product } from '@/types'
import { money } from '@/utils/format'

const loading = ref(false)
const rows = ref<Product[]>([])
const pager = reactive({ page: 1, size: 20, total: 0 })
const filters = reactive({ keyword: '', status: '' })

async function load() {
  loading.value = true
  try {
    const page = await productApi.page({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page: pager.page,
      size: pager.size
    })
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
  search()
}

onMounted(load)
</script>

<style scoped>
.product-name {
  color: #173250;
}
.strong {
  font-weight: 700;
  color: #12324f;
}
</style>
