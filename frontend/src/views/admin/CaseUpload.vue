<template>
  <div class="case-upload">
    <div class="page-header">
      <h3>测试用例管理</h3>
    </div>

    <!-- 选择题目 -->
    <div class="card-container" style="margin-bottom:16px">
      <el-form inline>
        <el-form-item label="选择题目">
          <el-select
            v-model="selectedQuestionId"
            placeholder="请选择题目"
            filterable
            style="width: 400px"
            @change="fetchCases"
          >
            <el-option
              v-for="q in questions"
              :key="q.id"
              :label="`#${q.id} ${q.title} (${diffLabel(q.difficulty)})`"
              :value="q.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用例列表 -->
    <div class="card-container" v-if="selectedQuestionId">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <span>共 {{ cases.length }} 个测试用例</span>
        <el-button type="primary" @click="uploadVisible = true">
          <el-icon><Upload /></el-icon> 上传用例
        </el-button>
      </div>

      <el-table :data="cases" v-loading="caseLoading" border stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="inputFilePath" label="输入文件" min-width="200" />
        <el-table-column prop="outputFilePath" label="输出文件" min-width="200" />
        <el-table-column label="可见性" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isVisible ? 'success' : 'warning'" size="small">
              {{ row.isVisible ? '基础可见' : '隐藏判题' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="请先选择一道题目" />

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadVisible" title="上传测试用例" width="500px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="输入文件" prop="inputFile">
          <el-upload :auto-upload="false" :limit="1" :on-change="(file) => uploadForm.inputFile = file.raw">
            <el-button type="primary">选择 .in 文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="输出文件" prop="outputFile">
          <el-upload :auto-upload="false" :limit="1" :on-change="(file) => uploadForm.outputFile = file.raw">
            <el-button type="primary">选择 .out 文件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="可见性">
          <el-switch
            v-model="uploadForm.isVisible"
            active-text="基础可见"
            inactive-text="隐藏判题"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listQuestions, listTestCases, uploadTestCase, deleteTestCase } from '@/api/question'
import { ElMessage } from 'element-plus'

const DIFF_MAP = { easy: '简单', medium: '中等', hard: '困难' }
const diffLabel = (d) => DIFF_MAP[d] || d

// ── 题目列表 ──
const questions = ref([])
onMounted(async () => {
  const res = await listQuestions({ pageSize: 1000 })
  questions.value = res.records || []
})

// ── 用例列表 ──
const selectedQuestionId = ref(null)
const cases = ref([])
const caseLoading = ref(false)

async function fetchCases() {
  if (!selectedQuestionId.value) return
  caseLoading.value = true
  try {
    const res = await listTestCases(selectedQuestionId.value)
    cases.value = res || []
  } finally { caseLoading.value = false }
}

// ── 上传 ──
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref(null)
const uploadForm = reactive({ inputFile: null, outputFile: null, isVisible: 0 })
const uploadRules = {
  inputFile: [{ required: true, message: '请选择输入文件', trigger: 'change' }],
  outputFile: [{ required: true, message: '请选择输出文件', trigger: 'change' }],
}

async function handleUpload() {
  const valid = await uploadFormRef.value.validate().catch(() => false)
  if (!valid) return

  uploading.value = true
  try {
    await uploadTestCase(
      selectedQuestionId.value,
      uploadForm.inputFile,
      uploadForm.outputFile,
      uploadForm.isVisible,
    )
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchCases()
  } finally { uploading.value = false }
}

// ── 删除 ──
async function handleDelete(id) {
  await deleteTestCase(id)
  ElMessage.success('删除成功')
  fetchCases()
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;
  h3 { margin: 0; font-size: 18px; }
}
</style>
