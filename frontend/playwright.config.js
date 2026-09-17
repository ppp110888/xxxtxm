import { defineConfig, devices } from '@playwright/test'

/**
 * CodeMate 前端 E2E 测试配置
 *
 * 运行前需确保:
 *   1. 后端已启动 (mvn spring-boot:run -Dspring-boot.run.profiles=dev)
 *   2. 前端已启动 (pnpm dev)
 *
 * 运行所有测试:
 *   npx playwright test
 *
 * 运行指定测试:
 *   npx playwright test --grep "login"
 *
 * UI 模式调试:
 *   npx playwright test --ui
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],
  timeout: 30_000,

  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
    video: 'off',
    // 后端 API 地址
    extraHTTPHeaders: {},
  },

  projects: [
    {
      name: process.env.CI ? 'chromium' : 'msedge',
      use: process.env.CI
        ? { ...devices['Desktop Chrome'] }
        : { ...devices['Desktop Edge'], channel: 'msedge' },
    },
  ],

  // 前端 Vite 开发服务器 (webServer 在测试启动时自动启动)
  webServer: [
    {
      command: 'mvn --file ../backend/pom.xml spring-boot:run',
      url: 'http://127.0.0.1:18080/actuator/health',
      env: {
        ...process.env,
        SPRING_PROFILES_ACTIVE: 'dev',
        SERVER_PORT: '18080',
        SPRING_DATASOURCE_URL: 'jdbc:h2:mem:codemate-e2e;MODE=MySQL;DATABASE_TO_LOWER=TRUE',
        ALLOWED_ORIGINS: 'http://127.0.0.1:4173',
      },
      reuseExistingServer: false,
      timeout: 120_000,
    },
    {
      command: 'pnpm exec vite --host 127.0.0.1 --port 4173 --strictPort',
      url: 'http://127.0.0.1:4173',
      env: { ...process.env, VITE_BACKEND_URL: 'http://127.0.0.1:18080' },
      reuseExistingServer: false,
      timeout: 30_000,
    },
  ],
})
