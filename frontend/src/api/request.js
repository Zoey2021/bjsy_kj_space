import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * Axios 请求封装
 * - 自动在请求头带上 JWT Token
 * - 401 时跳转登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const url = config.url || ''
  const isPublicAuth = url.includes('/auth/login') || url.includes('/auth/dingtalk/') || url.includes('/auth/class-code/')
  const token = localStorage.getItem('token')
  if (token && !isPublicAuth) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let lastNoticeAt = 0
let leaving = false

function notice(text) {
  const now = Date.now()
  if (!text || now - lastNoticeAt < 2500) return
  lastNoticeAt = now
  ElMessage.error({ message: text, grouping: true, duration: 2500 })
}

function leaveToLogin(text) {
  if (leaving || router.currentRoute.value.path === '/login') {
    notice(text)
    return
  }
  leaving = true
  localStorage.clear()
  notice(text)
  router.push('/login').finally(() => {
    setTimeout(() => { leaving = false }, 1500)
  })
}

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      notice(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    const status = err.response && err.response.status
    const body = err.response && err.response.data
    const serverMsg = body && typeof body === 'object' ? (body.message || body.msg) : ''
    const reqUrl = (err.config && err.config.url) || ''
    const isPublicAuth = reqUrl.includes('/auth/login') || reqUrl.includes('/auth/dingtalk/') || reqUrl.includes('/auth/class-code/')
    if (err.code === 'ERR_CANCELED' || err.name === 'CanceledError') {
      return Promise.reject(err)
    }
    const retryCount = err.config && err.config.__retryCount ? err.config.__retryCount : 0
    if (status === 403 && err.config && retryCount < 2 && !serverMsg) {
      err.config.__retryCount = retryCount + 1
      const wait = retryCount === 0 ? 400 : 1200
      return new Promise((resolve) => setTimeout(resolve, wait)).then(() => request(err.config))
    }
    if (status === 401 && !isPublicAuth) {
      leaveToLogin(serverMsg || '登录已失效，请重新登录')
    } else if (status === 403) {
      if (body && body.mustChangePassword) {
        localStorage.setItem('mustChangePassword', '1')
        notice(serverMsg || '请先修改密码后再使用平台')
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
      } else {
        notice(serverMsg || '刚才没连上，请刷新页面再试一次。')
      }
    } else if (err.code === 'ECONNABORTED' || (err.message && err.message.includes('timeout'))) {
      const slowAi = reqUrl.includes('/scaffold/generate') || reqUrl.includes('/ai/') || reqUrl.includes('/generate-activities')
      notice(slowAi
        ? '百炼工作流超时，请稍候再试（最多等待 3 分钟）'
        : '服务器响应超时，请稍后重试。')
    } else if (serverMsg) {
      notice(serverMsg)
    } else if (!status) {
      notice('网络不稳定，请稍后再试。')
    } else {
      notice('网络错误，请稍后重试。')
    }
    return Promise.reject(err)
  }
)

export default request
