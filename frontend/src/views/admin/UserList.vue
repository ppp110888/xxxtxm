<template>
  <div class="user-page">
    <div class="page-header">
      <h3>👥 用户管理</h3>
    </div>

    <div class="card-container">
      <!-- 搜索栏 -->
      <div class="filter-bar">
        <el-input
          v-model="query.username"
          placeholder="搜索用户名"
          clearable
          style="width: 200px"
          @keyup.enter="fetchData"
        />
        <el-select v-model="query.role" placeholder="角色筛选" clearable style="width: 140px; margin-left: 12px" @change="fetchData">
          <el-option label="管理员" value="admin" />
          <el-option label="学生" value="student" />
        </el-select>
        <el-button type="primary" style="margin-left: 12px" @click="fetchData">查询</el-button>
      </div>

      <!-- 用户表格 -->
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'success'" size="small">
              {{ row.role === 'admin' ? '管理员' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该用户？此操作不可恢复" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="'编辑用户 — ' + form.username" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="30" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="学生" value="student" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserPage, updateUser, deleteUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref(null)

const query = reactive({
  page: 1,
  pageSize: 20,
  username: '',
  role: '',
})

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  role: 'student',
})

const formRules = {
  nickname: [{ required: true, message: '昵称不能为空', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage({
      page: query.page,
      pageSize: query.pageSize,
      username: query.username || undefined,
      role: query.role || undefined,
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openEdit(row) {
  form.id = row.id
  form.username = row.username
  form.nickname = row.nickname || ''
  form.role = row.role
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateUser(form.id, { nickname: form.nickname, role: form.role })
    ElMessage.success('更新成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ }
  finally { saving.value = false }
}

async function handleDelete(id) {
  // 防止管理员删除自己（后端也会拦截，前端做一层预判）
  if (userStore.userInfo?.id === id) {
    ElMessage.warning('不能删除自己')
    return
  }
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    // 如果当前页只剩一条且非首页，回到上一页
    if (tableData.value.length === 1 && query.page > 1) {
      query.page--
    }
    fetchData()
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.user-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h3 { margin: 0; }
}

.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
