<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>⚡ CodeMate</h1>
        <p>AI 伴学编程平台</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            :disabled="loading"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="login-btn"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <el-button link type="primary" @click="showRegister = true">
          立即注册
        </el-button>
      </div>

      <!-- 注册弹窗 -->
      <el-dialog v-model="showRegister" title="注册新账号" width="400px">
        <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="regForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="regForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="regForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showRegister = false">取消</el-button>
          <el-button type="primary" @click="handleRegister" :loading="regLoading">注册</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ── 登录表单 ──
const formRef = ref(null)
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    // 跳转到目标页面
    const redirect = route.query.redirect || (userStore.isAdmin ? '/admin' : '/student')
    router.push(redirect)
  } catch (e) {
    // 错误信息由拦截器处理
  } finally {
    loading.value = false
  }
}

// ── 注册 ──
const showRegister = ref(false)
const regFormRef = ref(null)
const regForm = reactive({ username: '', password: '', confirmPassword: '' })
const regLoading = ref(false)
const regRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== regForm.password) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return

  regLoading.value = true
  try {
    await register(regForm.username, regForm.password, 'student')
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
  } catch { /* handled by interceptor */ }
  finally { regLoading.value = false }
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h1 {
    font-size: 32px;
    color: #303133;
    margin-bottom: 8px;
  }
  p {
    color: #909399;
    font-size: 14px;
  }
}

.login-btn {
  width: 100%;
}

.login-footer {
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
