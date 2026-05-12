<template>
  <div class="ai-analysis">
    <div class="analysis-header">
      <h2>AI 智能股票分析</h2>
      <p class="subtitle">基于大数据和人工智能技术，为您提供专业的股票技术分析报告</p>
    </div>

    <el-card class="search-card" shadow="hover">
      <div class="search-area">
        <el-input
          v-model="stockInput"
          placeholder="输入股票代码，如：600519（贵州茅台）"
          size="large"
          clearable
          @keyup.enter="startAnalysis"
          style="width: 400px"
        >
          <template #prepend>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="marketInput" size="large" style="width: 100px">
          <el-option label="自动" value="" />
          <el-option label="沪市" value="SH" />
          <el-option label="深市" value="SZ" />
        </el-select>
        <el-button type="primary" size="large" @click="startAnalysis" :loading="analyzing">
          <el-icon><Cpu /></el-icon>
          {{ analyzing ? '正在分析...' : '开始AI分析' }}
        </el-button>
      </div>

      <div class="hot-stocks" v-if="!analyzing && !result">
        <span class="hot-label">热门股票：</span>
        <el-tag
          v-for="stock in hotStocks"
          :key="stock.code"
          @click="quickAnalyze(stock)"
          class="hot-tag"
          effect="plain"
        >
          {{ stock.name }}({{ stock.code }})
        </el-tag>
      </div>
    </el-card>

    <div v-if="analyzing" class="analyzing-area">
      <el-skeleton :rows="10" animated />
      <div class="analyzing-text">
        <el-icon class="rotating"><Loading /></el-icon>
        AI 正在分析中，请稍候...
      </div>
    </div>

    <div v-if="result" class="result-area">
      <el-card class="result-header-card" shadow="hover">
        <div class="result-title">
          <div>
            <h3>{{ result.stockName }} ({{ result.stockCode }}) 分析报告</h3>
            <el-tag :type="confidenceType" size="large">
              置信度：{{ (result.confidenceScore * 100).toFixed(0) }}%
            </el-tag>
          </div>
          <el-button @click="resetAnalysis">重新分析</el-button>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="hover" class="result-card">
            <template #header>
              <div class="card-title">
                <el-icon color="#3a7bd5"><Document /></el-icon>
                <span>综合概述</span>
              </div>
            </template>
            <div class="card-content">{{ result.summary }}</div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover" class="result-card">
            <template #header>
              <div class="card-title">
                <el-icon color="#f5a623"><TrendCharts /></el-icon>
                <span>趋势分析</span>
              </div>
            </template>
            <div class="card-content">{{ result.trendAnalysis }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <template #header>
              <div class="card-title">
                <el-icon color="#ff4d4f"><WarningFilled /></el-icon>
                <span>风险评估</span>
              </div>
            </template>
            <div class="card-content">{{ result.riskAssessment }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <template #header>
              <div class="card-title">
                <el-icon color="#52c41a"><Select /></el-icon>
                <span>操作建议</span>
              </div>
            </template>
            <div class="card-content">{{ result.suggestion }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="result-card">
            <template #header>
              <div class="card-title">
                <el-icon color="#7b68ee"><Sunny /></el-icon>
                <span>市场情绪</span>
              </div>
            </template>
            <div class="card-content">{{ result.sentiment }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Cpu, Loading, Document, TrendCharts, WarningFilled, Select, Sunny } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { analyzeStock } from '../api'

const route = useRoute()
const router = useRouter()

const stockInput = ref('')
const marketInput = ref('')
const analyzing = ref(false)
const result = ref(null)

const hotStocks = [
  { code: '600519', name: '贵州茅台', market: 'SH' },
  { code: '000858', name: '五粮液', market: 'SZ' },
  { code: '300750', name: '宁德时代', market: 'SZ' },
  { code: '601318', name: '中国平安', market: 'SH' },
  { code: '600036', name: '招商银行', market: 'SH' },
  { code: '000001', name: '平安银行', market: 'SZ' },
  { code: '002594', name: '比亚迪', market: 'SZ' },
  { code: '688981', name: '中芯国际', market: 'SH' }
]

const confidenceType = computed(() => {
  if (!result.value) return 'info'
  const score = result.value.confidenceScore
  if (score >= 0.7) return 'success'
  if (score >= 0.4) return 'warning'
  return 'danger'
})

const startAnalysis = async () => {
  const code = stockInput.value.trim()
  if (!code) {
    ElMessage.warning('请输入股票代码')
    return
  }
  analyzing.value = true
  result.value = null
  try {
    const data = await analyzeStock(code, marketInput.value)
    result.value = data
  } catch (e) {
    ElMessage.error('分析失败，请重试')
  } finally {
    analyzing.value = false
  }
}

const quickAnalyze = (stock) => {
  stockInput.value = stock.code
  marketInput.value = stock.market
  startAnalysis()
}

const resetAnalysis = () => {
  result.value = null
  stockInput.value = ''
  marketInput.value = ''
}

onMounted(() => {
  const code = route.query.code
  const market = route.query.market
  const name = route.query.name
  if (code) {
    stockInput.value = code
    marketInput.value = market || ''
    startAnalysis()
  }
})
</script>

<style scoped>
.analysis-header {
  margin-bottom: 24px;
}

.analysis-header h2 {
  font-size: 26px;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.subtitle {
  color: #888;
  font-size: 14px;
}

.search-card {
  margin-bottom: 24px;
}

.search-area {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.hot-stocks {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-label {
  color: #888;
  font-size: 13px;
}

.hot-tag {
  cursor: pointer;
}

.hot-tag:hover {
  border-color: #3a7bd5;
  color: #3a7bd5;
}

.analyzing-area {
  text-align: center;
  padding: 40px;
}

.analyzing-text {
  margin-top: 20px;
  font-size: 16px;
  color: #888;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.result-header-card {
  margin-bottom: 16px;
}

.result-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-title h3 {
  font-size: 18px;
  color: #1a1a2e;
}

.result-card {
  height: 100%;
  min-height: 200px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.card-content {
  color: #555;
  line-height: 1.8;
  font-size: 14px;
  white-space: pre-wrap;
}
</style>