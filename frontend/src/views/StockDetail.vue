<template>
  <div class="stock-detail">
    <div class="detail-header">
      <el-button @click="$router.back()" :icon="ArrowLeft" type="default">返回</el-button>
      <h2 v-if="stockInfo">{{ stockInfo.name }} ({{ stockInfo.code }})</h2>
    </div>

    <el-row :gutter="16" v-if="stockInfo">
      <el-col :span="6" v-for="item in quoteCards" :key="item.label">
        <el-card class="quote-card" shadow="hover">
          <div class="card-label">{{ item.label }}</div>
          <div class="card-value" :class="item.cls">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="chart-card" shadow="hover" v-loading="chartLoading">
      <template #header>
        <div class="chart-header">
          <span>K线走势图</span>
          <el-radio-group v-model="klt" size="small" @change="fetchKLine">
            <el-radio-button value="101">日K</el-radio-button>
            <el-radio-button value="102">周K</el-radio-button>
            <el-radio-button value="103">月K</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="chartRef" style="width: 100%; height: 500px"></div>
    </el-card>

    <div class="action-buttons">
      <el-button type="success" size="large" @click="goToAnalysis">
        <el-icon><Cpu /></el-icon>
        AI 智能分析此股票
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Cpu } from '@element-plus/icons-vue'
import { getStockQuote, getKLineData } from '../api'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()

const code = route.params.code
const market = route.params.market

const stockInfo = ref(null)
const chartLoading = ref(false)
const klt = ref('101')
const chartRef = ref(null)
let chartInstance = null

const quoteCards = computed(() => {
  if (!stockInfo.value) return []
  const s = stockInfo.value
  const up = s.changePercent > 0
  const cls = (v) => v > 0 ? 'price-up' : v < 0 ? 'price-down' : ''
  return [
    { label: '最新价', value: formatPrice(s.price), cls: cls(s.changePercent) },
    { label: '涨跌幅', value: formatPercent(s.changePercent), cls: cls(s.changePercent) },
    { label: '涨跌额', value: formatPrice(s.changeAmount), cls: cls(s.changeAmount) },
    { label: '今开', value: formatPrice(s.open), cls: '' },
    { label: '最高', value: formatPrice(s.high), cls: 'price-up' },
    { label: '最低', value: formatPrice(s.low), cls: 'price-down' },
    { label: '昨收', value: formatPrice(s.preClose), cls: '' },
    { label: '成交量(手)', value: formatVolume(s.volume), cls: '' },
    { label: '成交额', value: formatAmount(s.amount), cls: '' },
    { label: '换手率', value: formatPercent(s.turnoverRate), cls: '' },
    { label: '市盈率', value: formatPrice(s.pe), cls: '' },
    { label: '总市值', value: formatCap(s.totalMarketCap), cls: '' }
  ]
})

const fetchQuote = async () => {
  try {
    stockInfo.value = await getStockQuote(code, market)
  } catch {
    // ignore
  }
}

const fetchKLine = async () => {
  chartLoading.value = true
  try {
    const data = await getKLineData(code, market, klt.value, '1', 100)
    if (data && data.klines && data.klines.length > 0) {
      await nextTick()
      renderChart(data.klines)
    }
  } catch {
    // ignore
  } finally {
    chartLoading.value = false
  }
}

const renderChart = (klines) => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const dates = klines.map(k => k.date)
  const values = klines.map(k => [k.open, k.close, k.low, k.high])
  const volumes = klines.map(k => k.volume)
  const changes = klines.map(k => k.changePercent)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['K线', 'MA5', 'MA10', 'MA20', '成交量'],
      top: 5
    },
    grid: [
      { left: '8%', right: '8%', top: '15%', height: '55%' },
      { left: '8%', right: '8%', top: '75%', height: '15%' }
    ],
    xAxis: [
      {
        type: 'category',
        data: dates,
        gridIndex: 0,
        axisLabel: { show: false }
      },
      {
        type: 'category',
        data: dates,
        gridIndex: 1
      }
    ],
    yAxis: [
      {
        type: 'value',
        gridIndex: 0,
        scale: true,
        axisLabel: { formatter: '{value}' }
      },
      {
        type: 'value',
        gridIndex: 1,
        axisLabel: { formatter: (v) => v >= 10000 ? (v / 10000).toFixed(0) + '万' : v }
      }
    ],
    dataZoom: [
      { type: 'inside', xAxisIndex: [0, 1], start: 50, end: 100 },
      { type: 'slider', xAxisIndex: [0, 1], start: 50, end: 100, bottom: 10 }
    ],
    series: [
      {
        name: 'K线',
        type: 'candlestick',
        data: values,
        xAxisIndex: 0,
        yAxisIndex: 0,
        itemStyle: {
          color: '#ff4d4f',
          color0: '#52c41a',
          borderColor: '#ff4d4f',
          borderColor0: '#52c41a'
        }
      },
      {
        name: 'MA5',
        type: 'line',
        data: calcMA(klines.map(k => k.close), 5),
        smooth: true,
        xAxisIndex: 0,
        yAxisIndex: 0,
        lineStyle: { width: 1.5, color: '#f5a623' },
        showSymbol: false
      },
      {
        name: 'MA10',
        type: 'line',
        data: calcMA(klines.map(k => k.close), 10),
        smooth: true,
        xAxisIndex: 0,
        yAxisIndex: 0,
        lineStyle: { width: 1.5, color: '#4a90d9' },
        showSymbol: false
      },
      {
        name: 'MA20',
        type: 'line',
        data: calcMA(klines.map(k => k.close), 20),
        smooth: true,
        xAxisIndex: 0,
        yAxisIndex: 0,
        lineStyle: { width: 1.5, color: '#7b68ee' },
        showSymbol: false
      },
      {
        name: '成交量',
        type: 'bar',
        data: volumes.map((v, i) => {
          const change = changes[i]
          return {
            value: v,
            itemStyle: {
              color: change >= 0 ? '#ff4d4f' : '#52c41a'
            }
          }
        }),
        xAxisIndex: 1,
        yAxisIndex: 1
      }
    ]
  }

  chartInstance.setOption(option, true)
}

const calcMA = (data, period) => {
  const result = []
  for (let i = 0; i < data.length; i++) {
    if (i < period - 1) {
      result.push(null)
    } else {
      let sum = 0
      for (let j = i - period + 1; j <= i; j++) {
        sum += data[j]
      }
      result.push(+(sum / period).toFixed(2))
    }
  }
  return result
}

const goToAnalysis = () => {
  const name = stockInfo.value?.name || ''
  router.push(`/analysis?code=${code}&market=${market}&name=${encodeURIComponent(name)}`)
}

const formatPrice = (val) => val != null ? Number(val).toFixed(2) : '-'
const formatPercent = (val) => val != null ? (Number(val) >= 0 ? '+' : '') + Number(val).toFixed(2) + '%' : '-'
const formatVolume = (val) => {
  if (val == null) return '-'
  return val >= 10000 ? (val / 10000).toFixed(0) + '万' : val.toFixed(0)
}
const formatAmount = (val) => {
  if (val == null) return '-'
  return val >= 100000000 ? (val / 100000000).toFixed(2) + '亿' : (val / 10000).toFixed(0) + '万'
}
const formatCap = (val) => {
  if (val == null) return '-'
  return val >= 100000000 ? (val / 100000000).toFixed(2) + '亿' : (val / 10000).toFixed(0) + '万'
}

const handleResize = () => chartInstance?.resize()
window.addEventListener('resize', handleResize)

onMounted(async () => {
  await fetchQuote()
  await fetchKLine()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.detail-header h2 {
  font-size: 22px;
  color: #1a1a2e;
}

.quote-card {
  margin-bottom: 12px;
  text-align: center;
}

.card-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 6px;
}

.card-value {
  font-size: 18px;
  font-weight: 700;
  color: #333;
}

.card-value.price-up { color: #ff4d4f; }
.card-value.price-down { color: #52c41a; }

.chart-card {
  margin-top: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>