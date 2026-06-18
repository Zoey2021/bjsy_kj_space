<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">系统管理</span>
      <el-menu mode="horizontal" :default-active="route.path" router background-color="#5e35b1" text-color="#fff" active-text-color="#ffd04b">
        <el-menu-item index="/admin/users">账号管理</el-menu-item>
      </el-menu>
      <span class="user">{{ realName }} <el-button link style="color:#fff" @click="logout">退出</el-button></span>
    </el-header>
    <el-main><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()
const realName = localStorage.getItem('realName')
const logout = () => { localStorage.clear(); router.push('/login') }
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.header {
  flex-shrink: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  background: #5e35b1;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}
.layout :deep(.el-main) {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}
.logo { color: #fff; font-size: 18px; font-weight: bold; margin-right: 30px; }
.user { color: #fff; margin-left: auto; }
.el-menu { border: none; flex: 1; }
</style>
