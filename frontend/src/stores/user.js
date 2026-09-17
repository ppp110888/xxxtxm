import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser, logout as logoutApi } from '@/api/auth'

/**
 * 用户状态管理 — Token + 角色 + 用户信息
 */
export const useUserStore = defineStore('user', () => {
  // ── state ──
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const routesLoaded = ref(false)

  // ── getters ──
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isAdmin = computed(() => role.value === 'admin')
  const isStudent = computed(() => role.value === 'student')

  // ── actions ──
  /** 登录 */
  async function login(username, password) {
    const tokenValue = await loginApi(username, password)
    token.value = tokenValue
    localStorage.setItem('token', tokenValue)

    // 获取用户信息
    const user = await getCurrentUser()
    userInfo.value = user
    localStorage.setItem('userInfo', JSON.stringify(user))

    return user
  }

  /** 退出 */
  async function logout() {
    try { await logoutApi() } catch (e) { /* ignore */ }
    token.value = ''
    userInfo.value = null
    routesLoaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  /** 从 localStorage 恢复用户信息 */
  async function restoreUserInfo() {
    if (token.value && !userInfo.value) {
      try {
        const user = await getCurrentUser()
        userInfo.value = user
        localStorage.setItem('userInfo', JSON.stringify(user))
      } catch (e) {
        // Token 无效，清除
        token.value = ''
        localStorage.removeItem('token')
      }
    }
  }

  return {
    token, userInfo, routesLoaded,
    isLoggedIn, role, isAdmin, isStudent,
    login, logout, restoreUserInfo,
  }
})
