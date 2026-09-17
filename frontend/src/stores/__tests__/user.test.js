import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

// Mock the API module
vi.mock('@/api/auth', () => ({
  login: vi.fn().mockResolvedValue('mock-token-abc'),
  getCurrentUser: vi.fn().mockResolvedValue({
    id: 1, username: 'student', nickname: '小明', role: 'student',
  }),
  logout: vi.fn().mockResolvedValue(),
}))

describe('useUserStore', () => {
  beforeEach(() => {
    // Fresh Pinia instance for each test
    setActivePinia(createPinia())
    // Clear localStorage
    localStorage.clear()
  })

  it('initializes with empty token and null userInfo', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.isLoggedIn).toBe(false)
  })

  it('isAdmin and isStudent return false when not logged in', () => {
    const store = useUserStore()
    expect(store.isAdmin).toBe(false)
    expect(store.isStudent).toBe(false)
  })

  it('correctly identifies admin role', () => {
    const store = useUserStore()
    store.$patch({
      token: 'test-token',
      userInfo: { id: 1, username: 'admin', role: 'admin', nickname: '管理员' },
    })
    expect(store.isAdmin).toBe(true)
    expect(store.isStudent).toBe(false)
  })

  it('correctly identifies student role', () => {
    const store = useUserStore()
    store.$patch({
      token: 'test-token',
      userInfo: { id: 2, username: 'student', role: 'student', nickname: '小明' },
    })
    expect(store.isStudent).toBe(true)
    expect(store.isAdmin).toBe(false)
  })

  it('isLoggedIn is true after setting token', () => {
    const store = useUserStore()
    store.$patch({ token: 'test-token' })
    expect(store.isLoggedIn).toBe(true)
  })

  it('logout clears all state', async () => {
    const store = useUserStore()
    store.$patch({
      token: 'test-token',
      userInfo: { id: 1, username: 'admin', role: 'admin' },
      routesLoaded: true,
    })
    await store.logout()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.routesLoaded).toBe(false)
    expect(store.isLoggedIn).toBe(false)
  })

  it('login calls API and sets state', async () => {
    const store = useUserStore()
    const user = await store.login('student', 'student123')
    expect(store.token).toBe('mock-token-abc')
    expect(user).toEqual({ id: 1, username: 'student', nickname: '小明', role: 'student' })
    expect(store.isLoggedIn).toBe(true)
  })
})
