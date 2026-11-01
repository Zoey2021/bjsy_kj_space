<template>
  <el-container class="layout">
    <el-header class="header">
      <span class="logo">信息科技学习空间</span>
      <el-menu
        :key="activeMenu"
        mode="horizontal"
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.92)"
        active-text-color="#fde68a"
        class="top-menu"
      >
        <el-menu-item index="/student/map">课程内容</el-menu-item>
        <el-menu-item index="/student/ai-lab">人工智能实验室</el-menu-item>
        <el-menu-item index="/student/tech-space">科技空间</el-menu-item>
        <el-menu-item index="/student/park">游学乐园</el-menu-item>
        <el-menu-item index="/student/mall">积分商城</el-menu-item>
      </el-menu>
      <div class="user">
        <el-dropdown trigger="click" @command="onUserCommand">
          <button type="button" class="user-name-btn">
            <span class="user-name">{{ realName }}</span>
            <span class="user-caret" aria-hidden="true">▾</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="records">学习记录</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button link class="logout" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px" append-to-body>
      <p class="pwd-tip">请设置一个自己记得住的新密码。4 到 20 位，不能和原来的一样。</p>
      <el-form label-width="88px" @submit.prevent>
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="4 到 20 位" />
        </el-form-item>
        <el-form-item label="再输入一次">
          <el-input v-model="pwdForm.confirm" type="password" show-password @keyup.enter="submitPassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="submitPassword">保存新密码</el-button>
      </template>
    </el-dialog>

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
import { ElMessage } from 'element-plus'
import StudentNotificationBar from '../components/student/StudentNotificationBar.vue'
import { getCurrentLesson, getNotifications, changePassword } from '../api'

const route = useRoute()
const router = useRouter()
const realName = localStorage.getItem('realName')
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })

const openPassword = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirm: '' }
  pwdVisible.value = true
}

const onUserCommand = (cmd) => {
  if (cmd === 'profile') {
    router.push('/student/profile')
    return
  }
  if (cmd === 'records') {
    router.push('/student/records')
    return
  }
  if (cmd === 'password') openPassword()
}

const activeMenu = computed(() => {
  const p = route.path
  if (
    p === '/student/map'
    || p.startsWith('/student/lesson/')
    || p.startsWith('/student/pretest/')
    || p.startsWith('/student/textbook/')
    || p.startsWith('/student/school-ppt/')
  ) {
    return '/student/map'
  }
  if (p === '/student/ai-lab' || p.startsWith('/student/ai-lab/')) return '/student/ai-lab'
  if (p === '/student/tech-space' || p.startsWith('/student/tech-space/')) return '/student/tech-space'
  if (p === '/student/park' || p.startsWith('/student/park/')) return '/student/park'
  if (p === '/student/mall' || p.startsWith('/student/mall/')) return '/student/mall'
  return p
})

const submitPassword = async () => {
  const next = String(pwdForm.value.newPassword || '').trim()
  if (!pwdForm.value.oldPassword) {
    ElMessage.warning('请填写原来的密码')
    return
  }
  if (next.length < 4 || next.length > 20) {
    ElMessage.warning('新密码请设置 4 到 20 位')
    return
  }
  if (next !== String(pwdForm.value.confirm || '').trim()) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (next === pwdForm.value.oldPassword) {
    ElMessage.warning('新密码不能和原来的一样')
    return
  }
  pwdSaving.value = true
  try {
    await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: next })
    pwdVisible.value = false
    ElMessage.success('密码已修改')
  } catch {
    /* 拦截器已提示 */
  } finally {
    pwdSaving.value = false
  }
}

const notifyVisible = ref(false)
const activeNotify = ref(null)
const notifySince = ref(Number(localStorage.getItem('notify_since') || 0))
let eventSource = null

const mainClass = computed(() => ({
  'main-lesson': route.path.includes('/student/lesson/') || route.path.startsWith('/student/pretest/'),
  'main-map': route.path === '/student/map',
  'main-pbl': route.path === '/student/pbl',
  'main-ai-lab': route.path === '/student/ai-lab' || route.path.startsWith('/student/ai-lab/'),
  'main-tech-space': route.path === '/student/tech-space' || route.path.startsWith('/student/tech-space/'),
  'main-school': route.path.startsWith('/student/textbook/') || route.path.startsWith('/student/school-ppt/')
}))

const logout = () => {
  eventSource?.close()
  localStorage.clear()
  router.push('/login')
}

const showNotify = (payload) => {
  if (!payload || payload.type === 'ping') return
  const data = payload.data || payload
  const type = payload.type || data.type
  if (type === 'class_dismiss' || type === 'park_revoke') {
    window.dispatchEvent(new CustomEvent('learn-park-closed'))
  }
  if (type === 'class_dismiss' || type === 'class_dismiss_clear') {
    return
  }
  activeNotify.value = { ...data, type }
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
  await pullOfflineNotifications()
  await tryAutoEnterLesson()
  // 全班同时登录时，先把首页接口跑完再挂通知长连接，避免把登录拖慢
  setTimeout(connectSse, 800)
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
  --student-header-h: 72px;
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
  margin-left: auto;
  display: flex;
  align-items: center;
  white-space: nowrap;
  line-height: 1.2;
}
.user-name-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  color: #fff;
}
.user-name-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}
.user-name {
  color: #fff;
  font-size: 14px;
}
.user-caret {
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
  line-height: 1;
}
.pwd-tip {
  margin: 0 0 14px;
  line-height: 1.6;
  color: #475569;
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
.layout :deep(.el-main.main-pbl) {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-pbl > *) {
  flex: 1;
  min-height: 0;
  height: 100%;
}
.layout :deep(.el-main.main-lesson) {
  padding: 0;
  overflow: hidden;
  background: #eef1f6;
}
.layout :deep(.el-main.main-map) {
  overflow: hidden;
  padding: 10px 16px 12px;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-map > *) {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.layout :deep(.el-main.main-ai-lab),
.layout :deep(.el-main.main-tech-space) {
  overflow: hidden;
  padding: 12px 16px 16px;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-ai-lab > *),
.layout :deep(.el-main.main-tech-space > *) {
  flex: 1;
  min-height: 0;
  height: 100%;
}
.layout :deep(.el-main.main-school) {
  overflow: hidden;
  padding: 10px 16px 12px;
  display: flex;
  flex-direction: column;
}
.layout :deep(.el-main.main-school > *) {
  flex: 1;
  width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  align-self: stretch;
}
</style>
