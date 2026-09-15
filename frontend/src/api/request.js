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
  const isPublicAuth = url.includes('/auth/login') || url.includes('/auth/class-code/')
  const token = localStorage.getItem('token')
  if (token && !isPublicAuth) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  err => {
    const status = err.response && err.response.status
    const body = err.response && err.response.data
    const serverMsg = body && (body.message || body.msg)
    const reqUrl = (err.config && err.config.url) || ''
    const isPublicAuth = reqUrl.includes('/auth/login') || reqUrl.includes('/auth/class-code/')
    if (status === 401 && !isPublicAuth) {
      localStorage.clear()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error(serverMsg || '登录已过期，请重新登录')
    } else if (err.code === 'ERR_CANCELED' || err.name === 'CanceledError') {
      return Promise.reject(err)
    } else if (err.code === 'ECONNABORTED' || (err.message && err.message.includes('timeout'))) {
      const slowAi = reqUrl.includes('/scaffold/generate') || reqUrl.includes('/ai/') || reqUrl.includes('/generate-activities')
      ElMessage.error(slowAi
        ? '百炼工作流超时，请稍候再试（最多等待 3 分钟）'
        : '服务器响应超时，请稍后重试。若连续失败，请联系管理员重启后端。')
    } else if (serverMsg) {
      ElMessage.error(serverMsg)
    } else {
      ElMessage.error(err.message || '网络错误（请确认后端已启动，或 F12 查看接口是否 502）')
    }
    return Promise.reject(err)
  }
)

export default request
