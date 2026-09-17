import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/** Axios 实例 — 自动注入 Token + 统一错误处理 */
const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

// ── 请求拦截器 ──
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = userStore.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── 响应拦截器 ──
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response } = error
    if (response) {
      switch (response.status) {
        case 401:
          // Token 过期 → 跳转登录
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(`请求失败: ${response.status}`)
      }
    } else {
      ElMessage.error('网络异常，请检查连接')
    }
    return Promise.reject(error)
  }
)

export default request
