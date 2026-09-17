import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// Monaco 多 Worker 分包 — 必须在 App 挂载前加载
import './monaco-setup'

import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './router/guard'
import './styles/global.scss'

const app = createApp(App)

// Pinia (must be before router for guard to use store)
const pinia = createPinia()
app.use(pinia)

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// Router
setupRouterGuard(router)
app.use(router)

// Element Plus (Chinese locale)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
