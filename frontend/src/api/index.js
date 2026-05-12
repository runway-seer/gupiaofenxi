import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    return Promise.reject(error)
  }
)

export function getStockList(market = 'all', page = 1, pageSize = 20) {
  return http.get('/stock/list', { params: { market, page, pageSize } })
}

export function getStockQuote(code, market = '') {
  return http.get('/stock/quote', { params: { code, market } })
}

export function getKLineData(code, market = '', klt = '101', fqt = '1', count = 60) {
  return http.get('/stock/kline', { params: { code, market, klt, fqt, count } })
}

export function searchStocks(keyword) {
  return http.get('/stock/search', { params: { keyword } })
}

export function analyzeStock(code, market = '') {
  return http.post('/analysis/stock', null, { params: { code, market } })
}

export default http