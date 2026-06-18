<template>
  <div class="login-page">
    <aside class="login-visual" aria-hidden="true">
      <img src="/login-cover.png" alt="" class="cover-img" />
      <div class="visual-mask" />
    </aside>

    <main class="login-panel">
      <div class="panel-inner">
        <div class="brand">
          <img src="/it-office-logo.jpg" alt="滨江实验小学信息组" class="brand-logo-img" />
          <h1 class="school-name">杭州市滨江实验小学</h1>
          <p class="product-name">信息科技学习平台</p>
          <div class="brand-bar"><i /><i /></div>
        </div>

        <div class="role-switch">
          <button
            type="button"
            class="role-btn"
            :class="{ active: role === 'STUDENT' }"
            @click="role = 'STUDENT'"
          >学生登录</button>
          <button
            type="button"
            class="role-btn"
            :class="{ active: role === 'TEACHER' }"
            @click="role = 'TEACHER'"
          >教师登录</button>
        </div>

        <h2 class="welcome">欢迎登录</h2>

        <div class="form-zone">
          <div class="mode-switch" :class="{ 'mode-switch--placeholder': role === 'TEACHER' }" :aria-hidden="role === 'TEACHER'">
            <button
              type="button"
              :class="{ active: studentMode === 'account' }"
              :tabindex="role === 'TEACHER' ? -1 : 0"
              @click="studentMode = 'account'"
            >账号登录</button>
            <button
              type="button"
              :class="{ active: studentMode === 'code' }"
              :tabindex="role === 'TEACHER' ? -1 : 0"
              @click="studentMode = 'code'"
            >班级码登录</button>
          </div>

          <el-form
            v-if="role === 'TEACHER' || studentMode === 'account'"
            class="login-form"
            @submit.prevent="handleAccountLogin"
          >
            <el-form-item>
              <el-input
                v-model="form.username"
                :placeholder="role === 'TEACHER' ? '教师账号' : '用户名'"
                prefix-icon="User"
                size="large"
                class="login-input"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
                class="login-input"
              />
            </el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleAccountLogin">
              登 录
            </el-button>
          </el-form>

          <div v-else class="code-form">
            <p class="code-tip">请输入教师提供的 6 位班级码，然后选择你的姓名进入平台。</p>
            <el-input
              v-model="classCode"
              maxlength="6"
              inputmode="numeric"
              placeholder="6 位班级码"
              size="large"
              class="login-input code-input"
              @input="onClassCodeInput"
              @keyup.enter="openStudentPicker"
            />
            <el-button type="primary" size="large" class="login-btn" :loading="codeLoading" @click="openStudentPicker">
              进入班级
            </el-button>
          </div>
        </div>

        <p class="demo-tip">演示：student1 / teacher1 / admin，密码 123456</p>
      </div>
    </main>

    <el-dialog
      v-model="pickerVisible"
      title="请选择你的名字"
      width="920px"
      align-center
      modal-class="student-picker-overlay"
      class="student-picker-dialog"
      :close-on-click-modal="false"
    >
      <p v-if="pickerClassName" class="picker-class">班级：{{ pickerClassName }}</p>
      <div v-loading="pickerLoading" class="student-grid">
        <button
          v-for="stu in pickerStudents"
          :key="stu.studentId"
          type="button"
          class="student-chip"
          :disabled="pickLoading"
          @click="confirmStudent(stu)"
        >
          {{ stu.realName }}
        </button>
      </div>
      <el-empty v-if="!pickerLoading && !pickerStudents.length" description="该班级暂无学生" :image-size="64" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, getClassCodeStudents, classCodeLogin } from '../api'

const router = useRouter()
const loading = ref(false)
const codeLoading = ref(false)
const pickLoading = ref(false)
const role = ref('STUDENT')
const studentMode = ref('code')
const form = ref({ username: '', password: '' })
const classCode = ref('')

const pickerVisible = ref(false)
const pickerStudents = ref([])
const pickerClassName = ref('')
const pickerLoading = ref(false)
const pendingCode = ref('')

const clearAuthStorage = () => {
  ;['token', 'role', 'realName', 'userId', 'username', 'classId', 'enrollmentYear'].forEach((key) => {
    localStorage.removeItem(key)
  })
}

/** 仅保留半角数字，避免粘贴或输入法带入非数字字符 */
const normalizeClassCode = (raw) => {
  const half = String(raw || '').replace(/[０-９]/g, (ch) => String.fromCharCode(ch.charCodeAt(0) - 0xfee0))
  return half.replace(/\D/g, '').slice(0, 6)
}

const onClassCodeInput = (val) => {
  classCode.value = normalizeClassCode(val)
}

const saveLoginAndRedirect = (data) => {
  if (!data?.token) {
    ElMessage.error('登录失败，未获取到有效凭证')
    return
  }
  localStorage.setItem('token', data.token)
  localStorage.setItem('role', data.role)
  localStorage.setItem('realName', data.realName)
  localStorage.setItem('userId', data.userId)
  localStorage.setItem('username', data.username || '')
  localStorage.setItem('classId', data.classId || '')
  if (data.enrollmentYear != null) {
    localStorage.setItem('enrollmentYear', String(data.enrollmentYear))
  } else {
    localStorage.removeItem('enrollmentYear')
  }
  const routes = { STUDENT: '/student/map', TEACHER: '/teacher/dashboard', ADMIN: '/admin/users' }
  router.push(routes[data.role] || '/login')
}

const handleAccountLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await login(form.value)
    const data = res.data
    if (role.value === 'STUDENT' && data.role !== 'STUDENT') {
      ElMessage.warning('请使用学生账号登录，或切换到教师登录')
      return
    }
    if (role.value === 'TEACHER' && data.role !== 'TEACHER' && data.role !== 'ADMIN') {
      ElMessage.warning('请使用教师账号登录')
      return
    }
    ElMessage.success('登录成功')
    saveLoginAndRedirect(data)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

const openStudentPicker = async () => {
  const code = normalizeClassCode(classCode.value)
  classCode.value = code
  if (!/^\d{6}$/.test(code)) {
    ElMessage.warning('请输入 6 位数字班级码')
    return
  }
  clearAuthStorage()
  codeLoading.value = true
  pickerLoading.value = true
  pickerVisible.value = true
  pickerStudents.value = []
  pickerClassName.value = ''
  try {
    const res = await getClassCodeStudents(code)
    pendingCode.value = code
    pickerClassName.value = res.data?.className || ''
    pickerStudents.value = res.data?.students || []
    if (!pickerStudents.value.length) {
      ElMessage.info('该班级暂无学生，请联系教师确认班级码')
    }
  } catch {
    pendingCode.value = ''
    pickerVisible.value = false
  } finally {
    codeLoading.value = false
    pickerLoading.value = false
  }
}

const confirmStudent = async (stu) => {
  if (!pendingCode.value) return
  pickLoading.value = true
  try {
    const res = await classCodeLogin({
      code: pendingCode.value,
      studentId: Number(stu.studentId)
    })
    ElMessage.success(`欢迎，${stu.realName}！`)
    pickerVisible.value = false
    if (res.data?.role !== 'STUDENT') {
      ElMessage.error('班级码登录仅适用于学生')
      return
    }
    saveLoginAndRedirect(res.data)
  } catch {
    /* 拦截器已提示 */
  } finally {
    pickLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f5f8fc;
}

.login-visual {
  flex: 1.15;
  position: relative;
  min-height: 320px;
  overflow: hidden;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  display: block;
}
.visual-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,0.02) 70%, rgba(245,248,252,0.35) 100%);
  pointer-events: none;
}

.login-panel {
  flex: 0 0 min(480px, 42vw);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px 36px 40px;
  background: #fff;
  box-shadow: -8px 0 32px rgba(30, 64, 120, 0.06);
  position: relative;
  min-height: 100vh;
  box-sizing: border-box;
}
.login-panel::after {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  width: 220px;
  height: 120px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 80' fill='none'%3E%3Cpath d='M0 80V40c20-8 40-12 60-10s40 18 60 20 50-8 80-20v50H0z' fill='%23e8f2ff'/%3E%3C/svg%3E") no-repeat bottom right / contain;
  opacity: 0.55;
  pointer-events: none;
}

.panel-inner {
  width: 100%;
  max-width: 360px;
  min-height: 520px;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
}

.brand {
  text-align: center;
  margin-bottom: 24px;
}
.brand-logo-img {
  width: 120px;
  height: auto;
  margin: 0 auto 12px;
  display: block;
  object-fit: contain;
}
.school-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e3a8a;
  letter-spacing: 0.02em;
  line-height: 1.35;
}
.product-name {
  margin: 6px 0 0;
  font-size: 15px;
  color: #3b82f6;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.brand-bar {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-top: 10px;
}
.brand-bar i {
  display: block;
  width: 28px;
  height: 4px;
  border-radius: 2px;
}
.brand-bar i:first-child { background: #fbbf24; }
.brand-bar i:last-child { background: #3b82f6; }

.role-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 4px;
  background: #f1f5f9;
  border-radius: 12px;
  margin-bottom: 20px;
}
.role-btn {
  border: none;
  padding: 10px 8px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}
.role-btn.active {
  background: #fff;
  color: #2563eb;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}

.welcome {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.form-zone {
  flex: 1;
  min-height: 228px;
}

.mode-switch {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  min-height: 37px;
}
.mode-switch--placeholder {
  visibility: hidden;
  pointer-events: none;
}
.mode-switch button {
  border: none;
  background: none;
  padding: 0 0 10px;
  font-size: 14px;
  color: #94a3b8;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.mode-switch button.active {
  color: #2563eb;
  font-weight: 600;
  border-bottom-color: #2563eb;
}

.login-form {
  min-height: 176px;
}
.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.login-form :deep(.el-input__wrapper),
.code-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  padding: 4px 12px;
}
.login-form :deep(.el-input__wrapper.is-focus),
.code-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25) inset;
}

.code-form {
  min-height: 176px;
}
.code-form .code-tip {
  margin: 0 0 14px;
  min-height: 40px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.55;
}
.code-input {
  margin-bottom: 16px;
}
.code-input :deep(.el-input__inner) {
  letter-spacing: 0.35em;
  font-size: 20px;
  font-weight: 700;
  text-align: center;
}

.login-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.15em;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.32);
}
.login-btn:hover {
  background: linear-gradient(135deg, #4f8ff7 0%, #3b82f6 100%);
}

.demo-tip {
  text-align: center;
  color: #94a3b8;
  font-size: 11px;
  margin: 20px 0 0;
}

.picker-class {
  margin: 0 0 16px;
  font-size: 15px;
  color: #475569;
  text-align: center;
}
.student-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
  min-height: calc(6 * 56px + 5 * 12px);
  max-height: calc(6 * 56px + 5 * 12px);
  overflow-y: auto;
  padding: 4px 2px;
}
.student-chip {
  padding: 14px 6px;
  border: 1px solid rgba(147, 197, 253, 0.65);
  border-radius: 12px;
  background: rgba(248, 251, 255, 0.82);
  color: #1e40af;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  line-height: 1.25;
}
.student-chip:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.92);
  color: #fff;
  border-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.22);
}
.student-chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 960px) {
  .student-grid {
    grid-template-columns: repeat(4, 1fr);
    min-height: calc(6 * 52px + 5 * 10px);
    max-height: calc(8 * 52px + 7 * 10px);
  }
}
</style>

<style>
/* 选名弹窗：半透明遮罩 + 毛玻璃面板（需全局样式作用于 el-dialog 挂载层） */
.student-picker-overlay {
  background-color: rgba(15, 23, 42, 0.42) !important;
  backdrop-filter: blur(6px);
}
.student-picker-dialog.el-dialog {
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 24px 64px rgba(30, 64, 120, 0.18);
}
.student-picker-dialog .el-dialog__header {
  padding: 22px 28px 8px;
  margin-right: 0;
}
.student-picker-dialog .el-dialog__title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.student-picker-dialog .el-dialog__body {
  padding: 8px 28px 28px;
}

@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }
  .login-visual {
    flex: none;
    height: 220px;
  }
  .login-panel {
    flex: 1;
    box-shadow: none;
  }
}
</style>
