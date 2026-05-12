import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import AIAnalysis from '../views/AIAnalysis.vue'
import StockDetail from '../views/StockDetail.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/analysis',
    name: 'AIAnalysis',
    component: AIAnalysis
  },
  {
    path: '/stock/:code/:market',
    name: 'StockDetail',
    component: StockDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router