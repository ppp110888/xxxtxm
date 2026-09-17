/**
 * E2E 测试辅助函数和共享配置
 */

const BASE_URL = 'http://localhost:5173'
const API_URL = 'http://localhost:8080'

// 测试用户凭据（由 DataInitializer 创建）
export const TEST_USERS = {
  student: { username: 'student', password: 'student123' },
  admin: { username: 'admin', password: 'admin123' },
}

/**
 * 通过 API 直接登录获取 Token（比 UI 登录更快）
 */
export async function getStudentToken() {
  const res = await fetch(`${API_URL}/api/v1/auth/login?username=${TEST_USERS.student.username}&password=${TEST_USERS.student.password}`, {
    method: 'POST',
  })
  const body = await res.json()
  return body.data  // Sa-Token 直接返回 token 字符串
}

/**
 * 通过 API 注册测试用户（如果用户不存在）
 */
export async function ensureTestUser() {
  try {
    await getStudentToken()
  } catch {
    // 用户不存在则注册
    await fetch(`${API_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        username: TEST_USERS.student.username,
        password: TEST_USERS.student.password,
        role: 'student',
      }),
    })
  }
}

/**
 * 等待 Element Plus 加载完成
 */
export async function waitForApp(page) {
  await page.waitForSelector('.el-loading-mask', { state: 'hidden', timeout: 5000 }).catch(() => {})
  await page.waitForTimeout(500)
}

/**
 * 快速登录（设置 Token 到 sessionStorage）
 */
export async function loginViaApi(page) {
  const token = await getStudentToken()
  await page.goto(BASE_URL)
  await page.evaluate((t) => {
    sessionStorage.setItem('sa-token', t)
  }, token)
  return token
}
