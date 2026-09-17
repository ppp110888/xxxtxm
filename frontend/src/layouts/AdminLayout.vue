<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapse" class="logo-text">CodeMate 管理</span>
        <span v-else class="logo-mini">CM</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/admin/knowledge">
          <el-icon><Collection /></el-icon>
          <span>知识树管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/questions">
          <el-icon><Document /></el-icon>
          <span>题库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/cases">
          <el-icon><Files /></el-icon>
          <span>测试用例</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="isCollapse = !isCollapse">
        <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
      </div>
    </el-aside>

    <!-- 右侧主区域 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">
            {{ route.meta.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><UserFilled /></el-icon> {{ userStore.userInfo?.role === 'admin' ? '管理员' : '学生' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录?', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await userStore.logout()
    router.push('/login')
  } catch { /* cancelled */ }
}
</script>

<style scoped lang="scss">
.admin-layout {
  height: 100vh;
}

.sidebar {
  background: var(--admin-sidebar-bg);
  overflow-y: auto;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);

  .logo-text {
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    white-space: nowrap;
  }
  .logo-mini {
    color: #fff;
    font-size: 20px;
    font-weight: 700;
  }
}

.el-menu {
  border-right: none;
  flex: 1;
}

.collapse-btn {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--admin-sidebar-text);
  cursor: pointer;
  border-top: 1px solid rgba(255,255,255,0.1);

  &:hover {
    color: #fff;
    background: rgba(255,255,255,0.05);
  }
}

.header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  .username {
    font-size: 14px;
    color: #303133;
  }
}

.main {
  background: #f0f2f5;
  min-height: 0;
}
</style>
