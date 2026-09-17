<template>
  <el-container class="student-layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <span class="logo" @click="$router.push('/student')">⚡ CodeMate</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        router
        class="nav-menu"
      >
        <el-menu-item index="/student/skill-tree">
          <el-icon><Guide /></el-icon> 技能树
        </el-menu-item>
        <el-menu-item index="/student/growth">
          <el-icon><DataAnalysis /></el-icon> 成长中心
        </el-menu-item>
      </el-menu>

      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-avatar :size="32" icon="UserFilled" />
            <span class="username">{{ userStore.userInfo?.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                学号: {{ userStore.userInfo?.id }}
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <!-- 主内容区 — 沉浸式全宽 -->
    <el-main class="main">
      <router-view v-slot="{ Component, route: routeKey }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="routeKey.path" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录?', '提示', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning',
    })
    await userStore.logout()
    router.push('/login')
  } catch { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.student-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  height: 60px;
  flex-shrink: 0;
}

.header-left {
  .logo {
    font-size: 20px;
    font-weight: 700;
    color: var(--color-primary);
    cursor: pointer;
    margin-right: 32px;
  }
}

.nav-menu {
  flex: 1;
  border-bottom: none !important;
}

.header-right {
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    .username { font-size: 14px; }
  }
}

.main {
  background: #f0f2f5;
  flex: 1;
  overflow-y: auto;
}
</style>
