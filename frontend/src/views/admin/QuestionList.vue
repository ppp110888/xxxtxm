<template>
  <div class="question-list">
    <div class="page-header">
      <h3>题库管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 添加题目
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="card-container filter-bar">
      <el-form :model="query" inline>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" placeholder="全部" clearable style="width: 120px">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="query.languageLimit" placeholder="全部" clearable style="width: 120px">
            <el-option label="Java" value="java" />
            <el-option label="Python" value="python" />
            <el-option label="均可" value="all" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card-container">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题目标题" min-width="200" />
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="diffType(row.difficulty)" size="small">
              {{ diffLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="languageLimit" label="语言" width="100" align="center" />
        <el-table-column label="时间限制" width="110" align="center">
          <template #default="{ row }">{{ row.timeLimitMs }}ms</template>
        </el-table-column>
        <el-table-column label="内存限制" width="110" align="center">
          <template #default="{ row }">{{ row.memoryLimitMb }}MB</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchData"
        style="margin-top: 20px; justify-content: flex-end;"
      />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑题目' : '添加题目'"
      width="800px"
      top="20px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" style="width:100%">
                <el-option label="简单" value="easy" />
                <el-option label="中等" value="medium" />
                <el-option label="困难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="语言限制" prop="languageLimit">
              <el-select v-model="form.languageLimit" style="width:100%">
                <el-option label="Java" value="java" />
                <el-option label="Python" value="python" />
                <el-option label="均可" value="all" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="时间限制(ms)">
              <el-input-number v-model="form.timeLimitMs" :min="100" :step="100" :max="10000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存限制(MB)">
              <el-input-number v-model="form.memoryLimitMb" :min="32" :step="32" :max="1024" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="6" placeholder="Markdown 格式" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="form.inputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="form.outputFormat" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-input v-model="form.dataRange" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listQuestions, createQuestion, updateQuestion, deleteQuestion } from '@/api/question'
import { ElMessage } from 'element-plus'

const DIFF_MAP = { easy: { label: '简单', type: 'success' }, medium: { label: '中等', type: 'warning' }, hard: { label: '困难', type: 'danger' } }
const diffLabel = (d) => DIFF_MAP[d]?.label || d
const diffType = (d) => DIFF_MAP[d]?.type || 'info'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 20, difficulty: '', languageLimit: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await listQuestions(query)
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

// ── 创建/编辑 ──
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  title: '', difficulty: 'easy', languageLimit: 'all',
  timeLimitMs: 1000, memoryLimitMb: 256,
  description: '', inputFormat: '', outputFormat: '', dataRange: '',
})
const editingId = ref(null)
const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  languageLimit: [{ required: true, message: '请选择语言限制', trigger: 'change' }],
}

function resetForm() {
  Object.assign(form, {
    title: '', difficulty: 'easy', languageLimit: 'all',
    timeLimitMs: 1000, memoryLimitMb: 256,
    description: '', inputFormat: '', outputFormat: '', dataRange: '',
  })
}

function openCreate() {
  isEdit.value = false; editingId.value = null; resetForm(); dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    isEdit.value ? await updateQuestion(editingId.value, { ...form }) : await createQuestion({ ...form })
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchData()
  } finally { saving.value = false }
}

async function handleDelete(id) {
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;
  h3 { margin: 0; font-size: 18px; }
}
.filter-bar { margin-bottom: 16px; }
</style>
