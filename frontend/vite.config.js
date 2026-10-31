import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 【开发环境代理】把 /api 请求转发到后端 8080，解决跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/pbl-app': {
        target: 'http://127.0.0.1:5014',
        changeOrigin: true
      }
    }
  }
})
