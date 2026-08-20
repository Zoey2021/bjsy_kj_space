<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">信息科技学习空间</span>
      <el-menu
        mode="horizontal"
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.92)"
        active-text-color="#fde68a"
        class="top-menu"
      >
        <el-menu-item index="/student/map">课程地图</el-menu-item>
        <el-menu-item index="/student/records">学习记录</el-menu-item>
        <el-menu-item index="/student/ai">AI体验中心</el-menu-item>
        <el-menu-item index="/student/park">游学乐园</el-menu-item>
      </el-menu>
      <span class="user">
        {{ realName }}
        <el-button link class="logout" @click="logout">退出</el-button>
      </span>
    </el-header>

    <div v-if="notifyVisible" class="notify-wrap">
      <StudentNotificationBar
        :visible="notifyVisible"
        :item="activeNotify"
        @close="closeNotify"
        @action="handleNotifyAction"
      />
    </div>

    <el-main :class="mainClass"><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StudentNotificationBar from '../components/student/StudentNotificationBar.vue'
import { getCurrentLesson, getNotifications } from '../api'

const route = useRoute()
const router = useRouter()
const realName = localStorage.getItem('realName')

const notifyVisible = ref(false)
const activeNotify = ref(null)
const notifySince = ref(Number(localStorage.getItem('notify_since') || 0))
let eventSource = null

const mainClass = computed(() => ({
  'main-lesson': route.path.includes('/student/lesson/')
}))

const logout = () => {
  eventSource?.close()
  localStorage.clear()
  router.push('/login')
}

const showNotify = (payload) => {
  if (!payload || payload.type === 'ping') return
  const data = payload.data || payload
  activeNotify.value = { ...data, type: payload.type || data.type }
  notifyVisible.value = true
  const ts = payload.timestamp || Date.now()
  notifySince.value = ts
  localStorage.setItem('notify_since', String(ts))
}

const closeNotify = () => {
  notifyVisible.value = false
}

const handleNotifyAction = (item) => {
  const type = item?.type || item?.data?.type
  const lessonId = item?.lessonId || item?.data?.lessonId
  const actIndex = item?.activityIndex ?? item?.data?.activityIndex
  closeNotify()
  if (type && String(type).startsWith('park_')) {
    router.push('/student/park')
    return
  }
  if (lessonId) {
    const query = actIndex != null ? { step: String(actIndex) } : {}
    router.push({ path: `/student/lesson/${lessonId}`, query })
  }
}

const connectSse = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  eventSource?.close()
  eventSource = new EventSource(`/api/learn/sse?token=${encodeURIComponent(token)}`)
  eventSource.addEventListener('message', (ev) => {
    try {
      showNotify(JSON.parse(ev.data))
    } catch { /* ignore */ }
  })
  eventSource.onerror = () => {
    eventSource?.close()
    setTimeout(connectSse, 5000)
  }
}

const pullOfflineNotifications = async () => {
  try {
    const res = await getNotifications(notifySince.value)
    const list = res.data || []
    if (list.length) {
      showNotify({ type: list[list.length - 1].type, data: list[list.length - 1] })
      notifySince.value = Date.now()
      localStorage.setItem('notify_since', String(notifySince.value))
    }
  } catch { /* ignore */ }
}

const tryAutoEnterLesson = async () => {
  if (route.path !== '/student/map') return
  try {
    const res = await getCurrentLesson()
    const lessonId = res.data?.lessonId
    if (lessonId) {
      router.replace(`/student/lesson/${lessonId}`)
    }
  } catch { /* ignore */ }
}

onMounted(async () => {
  connectSse()
  await pullOfflineNotifications()
  await tryAutoEnterLesson()
})

onUnmounted(() => {
  eventSource?.close()
})
</script>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  --student-header-h: 64px;
  --ls-primary: #4f46e5;
  --ls-primary-deep: #3730a3;
}
.header {
  flex-shrink: 0;
  height: var(--student-header-h);
  z-index: 1000;
  display: flex;
  align-items: center;
  padding: 0 22px;
  background: linear-gradient(135deg, #312e81 0%, #4f46e5 48%, #6366f1 100%);
  box-shadow: 0 4px 24px rgba(49, 46, 129, 0.28);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  margin-right: 28px;
  white-space: nowrap;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.12);
}
.top-menu {
  border: none !important;
  flex: 1;
  background: transparent !important;
}
.top-menu :deep(.el-menu-item) {
  border-bottom: none !important;
  font-weight: 500;
}
.top-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.12) !important;
  border-radius: 8px;
}
.user {
  color: #fff;
  margin-left: auto;
  white-space: nowrap;
  font-size: 14px;
}
.logout {
  color: rgba(255, 255, 255, 0.92) !important;
  margin-left: 8px;
}
.notify-wrap {
  flex-shrink: 0;
  padding: 10px 20px 0;
  background: #eef1f8;
}
.layout :deep(.el-main) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  background: linear-gradient(180deg, #eef1f8 0%, #e8ecf4 100%);
}
.layout :deep(.el-main.main-lesson) {
  padding: 0;
  overflow: hidden;
  background: #eef1f6;
}
</style>
