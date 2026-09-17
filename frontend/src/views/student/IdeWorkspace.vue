<template>
  <div class="ide-workspace">
    <!-- 顶栏 -->
    <div class="ide-header">
      <div class="ide-title">
        <el-button text @click="handleBack"><el-icon><ArrowLeft /></el-icon></el-button>
        <span>{{ question?.title || '加载中...' }}</span>
        <el-tag v-if="question" :type="diffType(question.difficulty)" size="small" style="margin-left:12px">
          {{ diffLabel(question.difficulty) }}
        </el-tag>
      </div>
      <div class="ide-toolbar">
        <el-tag v-if="wsConnected" type="success" size="small" effect="dark">⚡ 实时</el-tag>
        <el-select v-model="language" size="small" style="width:120px" @change="handleLanguageChange">
          <el-option label="Python" value="python" />
          <el-option label="Java" value="java" />
        </el-select>
        <el-button type="success" @click="handleRun" :loading="running" size="small">
          <el-icon><CaretRight /></el-icon> 自测
        </el-button>
        <el-button type="warning" @click="handleAiDiagnose" :loading="aiDiagnoseLoading" size="small">
          🤖 AI 诊断
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" size="small">
          <el-icon><Finished /></el-icon> 提交
        </el-button>
      </div>
    </div>

    <!-- 双栏主体 -->
    <div class="ide-body">
      <!-- 左侧面板 -->
      <div class="ide-left">
        <el-tabs v-model="leftTab" class="left-tabs">
          <el-tab-pane label="题目描述" name="desc">
            <div class="tab-content markdown-body">
              <div v-if="question">
                <h3>{{ question.title }}</h3>
                <div v-html="renderedDescription" />

                <el-divider />
                <h4>输入格式</h4>
                <p>{{ question.inputFormat }}</p>

                <h4>输出格式</h4>
                <p>{{ question.outputFormat }}</p>

                <h4>数据范围</h4>
                <p>{{ question.dataRange || '无特殊限制' }}</p>

                <el-divider />
                <el-tag>时间限制: {{ question.timeLimitMs }}ms</el-tag>
                <el-tag style="margin-left:8px">内存限制: {{ question.memoryLimitMb }}MB</el-tag>
                <el-tag style="margin-left:8px" :type="langTagType">{{ langLabel }}</el-tag>
              </div>
              <el-empty v-else description="题目加载中..." />
            </div>
          </el-tab-pane>

          <el-tab-pane label="样例" name="samples">
            <div class="tab-content">
              <div v-if="visibleCases.length">
                <el-card v-for="(c, i) in visibleCases" :key="c.id" class="sample-card">
                  <template #header>样例 {{ i + 1 }}</template>
                  <div><strong>输入:</strong><pre>{{ c.inputPreview || '(文件用例)' }}</pre></div>
                  <div><strong>输出:</strong><pre>{{ c.outputPreview || '(文件用例)' }}</pre></div>
                </el-card>
              </div>
              <el-empty v-else description="暂无可见样例" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="提交历史" name="history">
            <div class="tab-content">
              <div v-if="submissions.length">
                <div v-for="s in submissions" :key="s.id" class="history-item">
                  <el-tag :type="statusTagType(s.status)" size="small" effect="dark">
                    {{ statusLabel(s.status) }}
                  </el-tag>
                  <span class="history-time">{{ formatTime(s.createTime) }}</span>
                  <span v-if="s.timeUsed" class="history-meta">{{ s.timeUsed }}ms / {{ s.memoryUsed }}KB</span>
                </div>
              </div>
              <el-empty v-else description="暂无提交记录" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右侧编辑器 -->
      <div class="ide-right">
        <div class="editor-container">
          <MonacoEditor
            v-model="code"
            :language="language"
            height="100%"
            @mount="onEditorMount"
          />
        </div>

        <!-- 自测输入栏 -->
        <div class="run-input-bar">
          <el-input
            v-model="runInput"
            placeholder="输入测试数据（可选，回车触发自测）..."
            size="small"
            clearable
            @keyup.enter="handleRun"
          >
            <template #prepend>📥 输入</template>
          </el-input>
        </div>

        <!-- 输出面板 -->
        <div class="output-panel">
          <div class="output-header">
            <span>📋 输出结果</span>
            <div class="output-actions">
              <el-button text size="small" @click="clearOutput">清空</el-button>
            </div>
          </div>
          <pre class="output-content" :class="output ? outputStatus : 'output-placeholder'">{{ output || '点击「自测」运行代码，或点击「提交」进行判题' }}</pre>
        </div>

        <!-- AI 诊断面板 -->
        <div v-if="showAiDiagnosis" class="ai-panel">
          <div class="ai-panel-header">
            <span>🤖 AI 代码诊断</span>
            <el-button text size="small" @click="showAiDiagnosis = false">收起</el-button>
          </div>
          <div class="ai-panel-body">
            <div v-if="aiDiagnoseLoading && !aiDiagnoseContent" style="text-align:center;padding:24px;color:#909399">
              <el-icon class="is-loading" :size="20"><Loading /></el-icon>
              <p>AI 正在分析你的代码...</p>
            </div>
            <div v-else-if="aiDiagnoseContent" class="ai-content markdown-body" v-html="renderMarkdown(aiDiagnoseContent)" />
            <div v-else style="text-align:center;padding:24px;color:#C0C4CC">
              点击「🤖 AI 诊断」按钮获取代码分析
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStudentQuestion } from '@/api/question'
import { submitCode, getMySubmissions } from '@/api/submission'
import request from '@/utils/request'
import { JUDGE_STATUS_MAP, DIFFICULTY_MAP } from '@/utils/constants'
import { useJudgeSocket } from '@/composables/useJudgeSocket'
import { useAiStream } from '@/composables/useAiStream'
import { renderMarkdown } from '@/utils/markdown'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const questionId = computed(() => Number(route.params.questionId))

const question = ref(null)
const visibleCases = ref([])
const language = ref('python')
const code = ref('')
const output = ref('')
const outputStatus = ref('')
const running = ref(false)
const submitting = ref(false)
const leftTab = ref('desc')
const submissions = ref([])
const monacoRef = ref(null)
const runInput = ref('')
const lastFailedSubmissionId = ref(null)

// AI 代码诊断
const {
  content: aiDiagnoseContent,
  loading: aiDiagnoseLoading,
  startStream: aiDiagnoseStart,
} = useAiStream()
const showAiDiagnosis = ref(false)

function handleBack() {
  const returnTo = route.query.returnTo
  const fromPhase = route.query.fromPhase
  if (returnTo) {
    const target = fromPhase ? `${returnTo}?phase=${fromPhase}` : returnTo
    router.push(target)
  } else if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/student/skill-tree')
  }
}

function clearOutput() {
  output.value = ''
  outputStatus.value = ''
  runInput.value = ''
  showAiDiagnosis.value = false
}

function handleAiDiagnose() {
  if (!code.value.trim()) {
    ElMessage.warning('请先编写代码')
    return
  }
  showAiDiagnosis.value = true
  // 使用 POST 方式调用 AI 自由诊断，传入当前代码
  aiDiagnoseStart('/api/v1/student/ai/diagnose-code', {
    method: 'POST',
    body: JSON.stringify({
      questionId: questionId.value,
      language: language.value,
      code: code.value,
    }),
  })
}




// WebSocket 实时判题推送
const { connected: wsConnected, lastResult, connect: wsConnect } = useJudgeSocket()

// 监听 WebSocket 判题结果
watch(lastResult, (result) => {
  if (!result) return
  const emoji = result.status === 'AC' ? '✅' : '❌'
  const wsStatusLabel = JUDGE_STATUS_MAP[result.status]?.label || result.status
  let text = `${emoji} 判题完成 — ${wsStatusLabel} (ID: ${result.submissionId})\n⏱ 耗时: ${result.timeUsed}ms | 📦 内存: ${result.memoryUsed}KB`

  // 解析 detail 中的 JSON 详情
  if (result.detail) {
    try {
      const detail = JSON.parse(result.detail)
      if (detail.passed !== undefined && detail.total !== undefined) {
        text += `\n🧪 测试用例: ${detail.passed}/${detail.total} 通过`
      }
      if (detail.details) {
        text += '\n' + detail.details
      }
    } catch {
      text += '\n' + result.detail
    }
  }

  output.value = text
  outputStatus.value = result.status === 'AC' ? 'status-ac' : 'status-wa'
  if (result.status !== 'AC' && result.status !== 'PENDING' && result.status !== 'JUDGING') {
    lastFailedSubmissionId.value = result.submissionId
  }
  loadSubmissions()
})

// ── 工具函数 ──
const diffLabel = (d) => DIFFICULTY_MAP[d]?.label || d
const diffType = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')
const statusLabel = (s) => JUDGE_STATUS_MAP[s]?.label || s
const statusTagType = (s) => {
  const map = { AC: 'success', WA: 'danger', TLE: 'warning', MLE: 'warning', RE: 'danger', CE: '', PENDING: 'info' }
  return map[s] || 'info'
}
const formatTime = (t) => dayjs(t).format('MM-DD HH:mm')

/** 将判题结果详情 JSON 解析为可读文本 */
function formatJudgeDetail(submission) {
  const parts = []
  // 优先解析 resultDetail JSON
  if (submission?.resultDetail) {
    try {
      const detail = JSON.parse(submission.resultDetail)
      if (detail.passed !== undefined && detail.total !== undefined) {
        parts.push(`🧪 测试用例: ${detail.passed}/${detail.total} 通过`)
      }
      if (detail.details) {
        parts.push(detail.details)
      }
    } catch {
      parts.push(submission.resultDetail)
    }
  }
  // 编译错误 / 系统错误 → 显示 errorMessage
  if (submission?.errorMessage) {
    parts.push('---')
    parts.push(submission.errorMessage)
  }
  return parts.join('\n')
}
const langLabel = computed(() => {
  const limit = question.value?.languageLimit
  if (!limit || limit === 'all') return '语言: Java / Python 均可'
  return `仅限: ${limit === 'java' ? 'Java' : 'Python'}`
})
const langTagType = computed(() => {
  const limit = question.value?.languageLimit
  return limit === 'all' ? 'success' : 'warning'
})

// ── Markdown 简单渲染 ──
const renderedDescription = computed(() => {
  const text = question.value?.description || ''
  return text
    .replace(/### (.+)/g, '<h3>$1</h3>')
    .replace(/## (.+)/g, '<h2>$1</h2>')
    .replace(/# (.+)/g, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
})

// ── 语言切换时设置默认模板 ──
const TEMPLATES = {
  python: '# 在此编写 Python 代码\n\ndef solve():\n    pass\n\nif __name__ == "__main__":\n    solve()\n',
  java: '// 在此编写 Java 代码\n\nimport java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // TODO: 实现解题逻辑\n    }\n}\n',
}

function handleLanguageChange(lang) {
  if (!code.value.trim() || Object.keys(TEMPLATES).some(k => code.value === TEMPLATES[k])) {
    code.value = TEMPLATES[lang] || ''
  }
}

function onEditorMount(editor) {
  monacoRef.value = editor
}

// ── 自测 ──
async function handleRun() {
  if (!code.value.trim()) {
    output.value = '⚠️ 请先编写代码'
    outputStatus.value = 'status-wa'
    return
  }
  running.value = true
  output.value = '🔄 正在执行代码...'
  outputStatus.value = 'status-pending'
  try {
    const result = await request.post('/student/run', null, {
      params: {
        questionId: questionId.value,
        language: language.value,
        code: code.value,
        input: runInput.value || '',
      },
    })
    if (result.timeout) {
      output.value = `⏰ 运行超时\n---\n最后输出:\n${result.stdout || '(无)'}`
      outputStatus.value = 'status-wa'
    } else if (result.exitCode !== 0) {
      output.value = `❌ 运行错误 (exit=${result.exitCode})\n---\n${result.stderr || result.stdout || '(无输出)'}`
      outputStatus.value = 'status-wa'
    } else {
      output.value = `✅ 运行成功 (${result.timeUsed}ms)\n---\n${result.stdout || '(无输出)'}`
      outputStatus.value = 'status-ac'
    }
  } catch (e) {
    output.value = '❌ 执行失败，请确认代码正确后重试'
    outputStatus.value = 'status-wa'
  } finally {
    running.value = false
  }
}

// ── 提交 ──
async function handleSubmit() {
  if (!code.value.trim()) {
    output.value = '⚠️ 请先编写代码再提交'
    outputStatus.value = 'status-wa'
    return
  }
  submitting.value = true
  output.value = '⏳ 提交中，正在判题...'
  outputStatus.value = 'status-pending'
  try {
    const submission = await submitCode(questionId.value, language.value, code.value)
    const status = submission.status
    const statusLabel = JUDGE_STATUS_MAP[status]?.label || status
    if (status === 'AC') {
      output.value = `✅ ${statusLabel}! (ID: ${submission.id})\n⏱ ${submission.timeUsed || '?'}ms | 📦 ${submission.memoryUsed || '?'}KB\n${formatJudgeDetail(submission)}`
      outputStatus.value = 'status-ac'
      lastFailedSubmissionId.value = null
    } else if (status === 'PENDING' || status === 'JUDGING') {
      output.value = `⏳ 判题中... (ID: ${submission.id})\n结果将通过 WebSocket 实时推送。`
      outputStatus.value = 'status-pending'
    } else {
      output.value = `❌ ${statusLabel} (ID: ${submission.id})\n${formatJudgeDetail(submission)}`
      outputStatus.value = 'status-wa'
      lastFailedSubmissionId.value = submission.id
    }
    ElMessage.success('提交成功')
    loadSubmissions()
  } catch (e) {
    output.value = '❌ 提交失败，请稍后重试'
    outputStatus.value = 'status-wa'
  } finally {
    submitting.value = false
  }
}

// ── 加载提交历史 ──
async function loadSubmissions() {
  try {
    const res = await getMySubmissions({ questionId: questionId.value, pageSize: 10 })
    submissions.value = res.records || []
  } catch { /* ignore */ }
}

onMounted(async () => {
  // 连接 WebSocket 接收实时判题结果
  wsConnect()

  try {
    question.value = await getStudentQuestion(questionId.value)
      // 加载可见测试用例
    try {
      const cases = await request.get('/student/questions/' + questionId.value + '/cases')
        visibleCases.value = cases || []
    } catch { /* ignore */ }
    // 加载历史
    await loadSubmissions()
    // 设置默认模板
    code.value = TEMPLATES[language.value]
  } catch {
    output.value = '加载题目信息失败'
  }
})
</script>

<style scoped lang="scss">
.ide-workspace {
  height: calc(100vh - 60px - 40px);
  display: flex; flex-direction: column;
}

.ide-header {
  height: 48px; display: flex; align-items: center;
  justify-content: space-between;
  padding: 0 16px; background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.ide-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 16px; font-weight: 500;
}

.ide-toolbar {
  display: flex; align-items: center; gap: 8px;
}

.ide-body {
  flex: 1; display: flex; min-height: 0;
}

.ide-left {
  width: 35%; border-right: 1px solid #e4e7ed;
  background: #fff; display: flex; flex-direction: column;
  min-width: 280px;
}

.left-tabs {
  flex: 1; display: flex; flex-direction: column;
  :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 8px; }
  :deep(.el-tabs__content) { flex: 1; overflow-y: auto; }
}

.tab-content { padding: 16px; }

.markdown-body {
  line-height: 1.8;
  h1, h2, h3 { margin: 12px 0 8px; }
  code {
    background: #f5f5f5; padding: 2px 6px; border-radius: 4px;
    font-family: var(--font-mono); font-size: 13px;
  }
}

.ide-right {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
}

.editor-container {
  flex: 1; min-height: 0; background: #1e1e1e;
}

.output-panel {
  max-height: 220px; border-top: 2px solid var(--color-primary);
  flex-shrink: 0;
}

.output-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #fafafa; font-weight: 500;
  border-bottom: 1px solid #e4e7ed;
}
.output-actions { display: flex; align-items: center; gap: 4px; }

.output-content {
  padding: 12px 16px; margin: 0; font-family: var(--font-mono);
  font-size: 13px; max-height: 180px; overflow-y: auto;
  background: #fafafa; white-space: pre-wrap; word-break: break-all;

  &.status-pending { color: #909399; }
  &.status-wa { color: #F56C6C; }
  &.status-ac { color: #67C23A; }
  &.output-placeholder { color: #C0C4CC; font-style: italic; }
}

.run-input-bar {
  padding: 4px 0; background: #fafafa;
  border-top: 1px solid #e4e7ed;
}

/* ── AI 诊断面板 ── */
.ai-panel {
  max-height: 320px; border-top: 2px solid #E6A23C; flex-shrink: 0;
}
.ai-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 16px; background: #fef7e8; font-weight: 500;
  border-bottom: 1px solid #f5dab1;
}
.ai-panel-body {
  padding: 16px; max-height: 280px; overflow-y: auto; background: #fff;
}
.ai-content {
  line-height: 1.8; font-size: 14px;
  h1, h2, h3 { margin: 12px 0 8px; }
  h4, h5, h6 { margin: 8px 0 4px; }
  p { margin: 0 0 8px; }
  ul, ol { padding-left: 20px; margin: 8px 0; }
  code {
    background: #f5f5f5; padding: 2px 6px; border-radius: 4px;
    font-family: var(--font-mono); font-size: 13px;
  }
  pre {
    background: #282c34; color: #abb2bf; padding: 12px;
    border-radius: 6px; overflow-x: auto;
    code { background: transparent; padding: 0; color: inherit; }
  }
  blockquote {
    margin: 8px 0; padding: 6px 14px;
    border-left: 3px solid #E6A23C;
    background: #fef7e8; color: #606266;
  }
  table {
    border-collapse: collapse;
    th, td { border: 1px solid #e4e7ed; padding: 6px 10px; }
    th { background: #f5f7fa; }
  }
}

.sample-card {
  margin-bottom: 12px;
  pre { margin: 4px 0; padding: 8px; background: #f5f5f5; border-radius: 4px;
       font-family: var(--font-mono); font-size: 13px; white-space: pre-wrap; }
}

.history-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 0; border-bottom: 1px solid #f0f0f0;
}

.history-time { color: #909399; font-size: 12px; }
.history-meta { color: #C0C4CC; font-size: 12px; margin-left: auto; }

/* AI 按钮强调样式 */
.ai-btn {
  display: inline-flex !important;
  align-items: center !important;
  gap: 4px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  padding: 8px 16px !important;
  border: 1px dashed #c0a0e0 !important;
  border-radius: 8px !important;
  transition: all 0.2s;
}
.ai-btn:hover {
  background: linear-gradient(135deg, #f5f0ff, #ede0ff) !important;
  border-color: #a855f7 !important;
}
</style>
