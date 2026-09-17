import { test, expect } from '@playwright/test'
import { TEST_USERS } from './fixtures.js'

/**
 * 登录 & 注册 E2E 测试
 *
 * 前置条件: 后端 dev 模式运行中 (mvn spring-boot:run -Dspring-boot.run.profiles=dev)
 * 测试用户由 DataInitializer 自动创建
 */
test.describe('登录页面', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
  })

  test('显示登录表单', async ({ page }) => {
    await expect(page.locator('h1')).toHaveText('⚡ CodeMate')
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible()
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible()
    await expect(page.getByRole('button', { name: '登 录' })).toBeVisible()
  })

  test('空表单校验', async ({ page }) => {
    await page.getByRole('button', { name: '登 录' }).click()
    await expect(page.locator('.el-form-item__error').first()).toBeVisible()
  })

  test('错误密码登录失败', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', 'student')
    await page.fill('input[placeholder="请输入密码"]', 'wrongpassword')
    await page.getByRole('button', { name: '登 录' }).click()
    // 应该停留在登录页或显示错误
    await page.waitForTimeout(2000)
    await expect(page).not.toHaveURL(/\/student/)
  })

  test('学生成功登录并跳转到技能树', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', TEST_USERS.student.username)
    await page.fill('input[placeholder="请输入密码"]', TEST_USERS.student.password)
    const loginResponse = page.waitForResponse(response =>
      response.url().includes('/api/v1/auth/login'))
    await page.getByRole('button', { name: '登 录' }).click()
    expect((await loginResponse).status()).toBe(200)

    // 等待跳转到学生端
    await page.waitForURL(/\/student/, { timeout: 10000 })
    await expect(page).toHaveURL(/\/student/)
  })

  test('管理员成功登录', async ({ page }) => {
    await page.fill('input[placeholder="请输入用户名"]', TEST_USERS.admin.username)
    await page.fill('input[placeholder="请输入密码"]', TEST_USERS.admin.password)
    const loginResponse = page.waitForResponse(response =>
      response.url().includes('/api/v1/auth/login'))
    await page.getByRole('button', { name: '登 录' }).click()
    expect((await loginResponse).status()).toBe(200)

    await page.waitForURL(/\/admin/, { timeout: 10000 })
    await expect(page).toHaveURL(/\/admin/)
  })

  test('注册弹窗可以打开和关闭', async ({ page }) => {
    await page.getByRole('button', { name: '立即注册' }).click()
    await expect(page.locator('.el-dialog')).toBeVisible()
    await expect(page.locator('.el-dialog')).toContainText('注册新账号')

    // 关闭弹窗
    await page.getByRole('button', { name: '取消' }).click()
    await page.waitForTimeout(500)
    await expect(page.locator('.el-dialog')).not.toBeVisible()
  })
})
