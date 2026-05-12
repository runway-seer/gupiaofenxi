<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h2>行情看板</h2>
      <div class="dashboard-controls">
        <el-select v-model="currentMarket" @change="fetchStocks" style="width: 140px">
          <el-option label="全部A股" value="all" />
          <el-option label="上证A股" value="sh" />
          <el-option label="深证A股" value="sz" />
          <el-option label="创业板" value="cyb" />
          <el-option label="科创板" value="kcb" />
          <el-option label="北交所" value="bj" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索股票代码或名称"
          :prefix-icon="Search"
          clearable
          @clear="fetchStocks"
          @keyup.enter="handleSearch"
          style="width: 260px"
        />
        <el-button type="primary" @click="fetchStocks">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-table
      :data="stocks"
      v-loading="loading"
      stripe
      height="calc(100vh - 200px)"
      @row-click="goToDetail"
      style="cursor: pointer"
    >
      <el-table-column prop="code" label="代码" width="100" fixed />
      <el-table-column prop="name" label="名称" width="100" fixed />
      <el-table-column label="最新价" width="100" sortable>
        <template #default="{ row }">
          <span :class="priceClass(row.changePercent)">{{ formatPrice(row.price) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="涨跌幅" width="100" sortable>
        <template #default="{ row }">
          <span :class="priceClass(row.changePercent)">{{ formatPercent(row.changePercent) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="涨跌额" width="100">
        <template #default="{ row }">
          <span :class="priceClass(row.changeAmount)">{{ formatPrice(row.changeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="今开" width="90">
        <template #default="{ row }">{{ formatPrice(row.open) }}</template>
      </el-table-column>
      <el-table-column label="最高" width="90">
        <template #default="{ row }">{{ formatPrice(row.high) }}</template>
      </el-table-column>
      <el-table-column label="最低" width="90">
        <template #default="{ row }">{{ formatPrice(row.low) }}</template>
      </el-table-column>
      <el-table-column label="昨收" width="90">
        <template #default="{ row }">{{ formatPrice(row.preClose) }}</template>
      </el-table-column>
      <el-table-column label="成交量(手)" width="120">
        <template #default="{ row }">{{ formatVolume(row.volume) }}</template>
      </el-table-column>
      <el-table-column label="成交额" width="120">
        <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="换手率" width="90">
        <template #default="{ row }">{{ formatPercent(row.turnoverRate) }}</template>
      </el-table-column>
      <el-table-column label="市盈率" width="90">
        <template #default="{ row }">{{ formatPrice(row.pe) }}</template>
      </el-table-column>
      <el-table-column label="总市值" width="120">
        <template #default="{ row }">{{ formatCap(row.totalMarketCap) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click.stop="goToDetail(row)">
            详情
          </el-button>
          <el-button size="small" type="success" link @click.stop="goToAnalysis(row)">
            AI分析
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        :total="5000"
        @size-change="fetchStocks"
        @current-change="fetchStocks"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getStockList, searchStocks } from '../api'

const router = useRouter()

const stocks = ref([])
const loading = ref(false)
const currentMarket = ref('all')
const currentPage = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')

const fetchStocks = async () => {
  loading.value = true
  try {
    if (searchKeyword.value) {
      const result = await searchStocks(searchKeyword.value)
      stocks.value = result || []
    } else {
      const result = await getStockList(currentMarket.value, currentPage.value, pageSize.value)
      stocks.value = result || []
    }
  } catch {
    stocks.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchStocks()
}

const goToDetail = (row) => {
  router.push(`/stock/${row.code}/${row.market || ''}`)
}

const goToAnalysis = (row) => {
  router.push(`/analysis?code=${row.code}&market=${row.market || ''}&name=${encodeURIComponent(row.name || '')}`)
}

const priceClass = (val) => {
  if (!val) return ''
  return val > 0 ? 'price-up' : val < 0 ? 'price-down' : ''
}

const formatPrice = (val) => {
  if (val == null) return '-'
  return Number(val).toFixed(2)
}

const formatPercent = (val) => {
  if (val == null) return '-'
  return (Number(val) >= 0 ? '+' : '') + Number(val).toFixed(2) + '%'
}

const formatVolume = (val) => {
  if (val == null) return '-'
  if (val >= 10000) return (val / 10000).toFixed(0) + '万'
  return val.toFixed(0)
}

const formatAmount = (val) => {
  if (val == null) return '-'
  if (val >= 100000000) return (val / 100000000).toFixed(2) + '亿'
  if (val >= 10000) return (val / 10000).toFixed(0) + '万'
  return val.toFixed(0)
}

const formatCap = (val) => {
  if (val == null) return '-'
  if (val >= 100000000) return (val / 100000000).toFixed(2) + '亿'
  return (val / 10000).toFixed(0) + '万'
}

onMounted(fetchStocks)
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.dashboard-header h2 {
  font-size: 24px;
  color: #1a1a2e;
}

.dashboard-controls {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.price-up {
  color: #ff4d4f;
  font-weight: 600;
}

.price-down {
  color: #52c41a;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>