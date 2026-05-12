<template>
  <div id="app">
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-icon :size="28"><TrendCharts /></el-icon>
          <span class="app-title">AI 股票分析平台</span>
        </div>
        <div class="header-right">
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="header-menu"
          >
            <el-menu-item index="/">
              <el-icon><DataBoard /></el-icon>
              <span>行情看板</span>
            </el-menu-item>
            <el-menu-item index="/analysis">
              <el-icon><Cpu /></el-icon>
              <span>AI 分析</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #e0e0e0;
}

.app-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #00d2ff, #3a7bd5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-menu {
  background: transparent !important;
  border-bottom: none !important;
}

.header-menu .el-menu-item {
  color: #a0a0b0 !important;
  border-bottom: 2px solid transparent !important;
}

.header-menu .el-menu-item:hover {
  color: #e0e0e0 !important;
  background: rgba(255, 255, 255, 0.05) !important;
}

.header-menu .el-menu-item.is-active {
  color: #00d2ff !important;
  border-bottom-color: #00d2ff !important;
  background: rgba(0, 210, 255, 0.08) !important;
}

.el-main {
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>