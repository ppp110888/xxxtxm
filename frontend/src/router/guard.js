import { useUserStore } from '@/stores/user'
import { adminRoutes, studentRoutes } from './index'

const WHITE_LIST = ['/login', '/404']

/**
 * 路由守卫 — 动态路由拦截器
 */
export function setupRouterGuard(router) {
  router.beforeEach(async (to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} — CodeMate` : 'CodeMate'

    const userStore = useUserStore()

    // 1. 白名单直接放行
    if (WHITE_LIST.includes(to.path)) {
      // 已登录访问 /login → 重定向到主页
      if (to.path === '/login' && userStore.isLoggedIn) {
        return next(userStore.isAdmin ? '/admin' : '/student')
      }
      return next()
    }

    // 2. 未登录 → 跳转登录
    if (!userStore.isLoggedIn) {
      return next(`/login?redirect=${to.path}`)
    }

    // 3. 恢复用户信息（刷新页面后）
    if (!userStore.userInfo) {
      try {
        await userStore.restoreUserInfo()
      } catch {
        return next('/login')
      }
    }

    // 4. 动态添加角色路由
    if (!userStore.routesLoaded) {
      const routes = userStore.isAdmin ? adminRoutes : studentRoutes
      routes.forEach(route => router.addRoute(route))
      userStore.routesLoaded = true
      // 重新触发当前路由匹配
      return next({ ...to, replace: true })
    }

    // 5. 角色越权检查
    if (to.path.startsWith('/admin') && !userStore.isAdmin) {
      return next('/404')
    }
    if (to.path.startsWith('/student') && !userStore.isStudent) {
      return next('/404')
    }

    // 6. 根路径重定向
    if (to.path === '/') {
      return next(userStore.isAdmin ? '/admin' : '/student')
    }

    next()
  })
}
