<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">信息科技学习空间</span>
      <el-menu mode="horizontal" :default-active="route.path" router background-color="#1a73e8" text-color="#fff" active-text-color="#ffd04b">
        <el-menu-item index="/student/map">课程地图</el-menu-item>
        <el-menu-item index="/student/records">学习记录</el-menu-item>
        <el-menu-item index="/student/ai">AI体验中心</el-menu-item>
      </el-menu>
      <span class="user">{{ realName }} <el-button link style="color:#fff" @click="logout">退出</el-button></span>
    </el-header>
    <el-main :class="mainClass"><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const realName = localStorage.getItem('realName')
const logout = () => { localStorage.clear(); router.push('/login') }

/** 课时工作台占满 header 以下区域，避免与顶部 banner 重叠 */
const mainClass = computed(() => ({
  'main-lesson': route.path.includes('/student/lesson/')
}))
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --student-header-h: 60px;
}
.header {
  flex-shrink: 0;
  height: var(--student-header-h);
  z-index: 1000;
  display: flex;
  align-items: center;
  background: #1a73e8;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.layout :deep(.el-main) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
}
.layout :deep(.el-main.main-lesson) {
  padding: 0;
  overflow: hidden;
}
.logo { color: #fff; font-size: 18px; font-weight: bold; margin-right: 30px; white-space: nowrap; }
.user { color: #fff; margin-left: auto; white-space: nowrap; }
.el-menu { border: none; flex: 1; }
</style>
