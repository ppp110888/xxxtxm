<template>
  <div class="learning-cabin">
    <!-- 顶部返回栏 -->
    <div class="cabin-top-bar">
      <el-button text @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        <span v-if="node?.parentId">返回上级节点</span>
        <span v-else>返回技能树</span>
      </el-button>
      <span class="cabin-breadcrumb">{{ node?.name || '' }}</span>
    </div>

    <!-- 阶段标签 -->
    <div class="phase-tabs">
      <el-steps :active="activePhase" align-center>
        <el-step title="知识吸收" description="阅读教程" />
        <el-step title="例题拆解" description="学习思路" />
        <el-step title="实战演练" description="在线编码" />
      </el-steps>
    </div>

    <div v-loading="loading" class="phase-body">
      <!-- 加载失败 -->
      <el-result v-if="error" icon="error" :title="error" style="margin-top:60px">
        <template #extra>
          <el-button type="primary" @click="$router.back()">返回技能树</el-button>
        </template>
      </el-result>

      <template v-else-if="node">
        <!-- 阶段一：知识吸收 -->
        <div v-show="activePhase === 0" class="card-container phase-content">
          <div style="display:flex;justify-content:space-between;align-items:flex-start">
            <h3>{{ node.name }}</h3>
            <el-button
              type="primary" class="ai-btn" text
              :loading="aiExplainLoading"
              @click="toggleAiExplain"
            >
              🤖 {{ showAiExplain ? '收起讲解' : 'AI 讲解' }}
            </el-button>
          </div>

          <!-- AI 讲解面板 -->
          <div v-if="showAiExplain" class="ai-explain-panel">
            <div v-if="aiExplainContent" class="markdown-body" v-html="renderMarkdown(aiExplainContent)" />
            <div v-else-if="aiExplainLoading" style="text-align:center;padding:24px;color:#909399">
              <el-icon class="is-loading" :size="20"><Loading /></el-icon>
              <p>AI 正在为你讲解这个知识点...</p>
            </div>
          </div>

          <div class="markdown-body" v-html="renderedMarkdown" v-if="node.markdownContent" />
          <el-empty v-else description="暂无讲义内容" />

          <!-- 子节点导航 -->
          <div v-if="childNodes.length" class="child-nodes-section">
            <h4>📂 本章节内容</h4>
            <div class="child-nodes-grid">
              <div
                v-for="child in childNodes"
                :key="child.id"
                class="child-node-card"
                @click="enterChild(child)"
              >
                <div class="child-icon">
                  <el-icon :size="20">
                    <Loading v-if="child._status === 'in_progress'" />
                    <CircleCheck v-else />
                  </el-icon>
                </div>
                <div class="child-info">
                  <span class="child-name">{{ child.name }}</span>
                  <el-tag
                    :type="child._status === 'cleared' ? 'success' : ''"
                    size="small"
                  >
                    {{ statusLabel(child._status) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>

          <div style="text-align:right;margin-top:24px">
            <el-button type="primary" @click="activePhase = 1">下一步：例题拆解 →</el-button>
          </div>
        </div>

        <!-- 阶段二：例题拆解 -->
        <div v-show="activePhase === 1" class="card-container phase-content">
          <h3>经典例题</h3>
          <el-empty v-if="!exampleQuestions.length" description="暂无关联例题" />
          <div v-else class="example-list">
            <el-card v-for="q in exampleQuestions" :key="q.id" class="example-card">
              <template #header>
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <span>{{ q.title }}</span>
                  <el-tag :type="diffType(q.difficulty)" size="small">{{ diffLabel(q.difficulty) }}</el-tag>
                </div>
              </template>
              <!-- 题目描述 -->
              <div class="markdown-body" v-html="renderMarkdown(q.description)" />

              <!-- 参考代码 -->
              <div v-if="q.referenceAnswer" class="solution-section">
                <h4>📝 参考代码</h4>
                <pre class="code-block"><code>{{ q.referenceAnswer }}</code></pre>
              </div>

              <!-- 题目详解 -->
              <div v-if="q.explanation" class="solution-section">
                <h4>💡 详细解析</h4>
                <div class="markdown-body" v-html="renderMarkdown(q.explanation)" />
              </div>
            </el-card>
          </div>
          <div style="display:flex;justify-content:space-between;margin-top:24px">
            <el-button @click="activePhase = 0">← 返回学习</el-button>
            <el-button type="primary" @click="activePhase = 2">下一步：实战演练 →</el-button>
          </div>
        </div>

        <!-- 阶段三：实战演练 -->
        <div v-show="activePhase === 2" class="card-container phase-content">
          <h3>实战练习题</h3>
          <el-empty v-if="!practiceQuestions.length" description="暂无练习题" />
          <div v-else class="practice-list">
            <el-card v-for="q in practiceQuestions" :key="q.id" class="practice-card">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <div>
                  <span style="font-size:16px;font-weight:500">{{ q.title }}</span>
                  <el-tag style="margin-left:12px" :type="diffType(q.difficulty)" size="small">
                    {{ diffLabel(q.difficulty) }}
                  </el-tag>
                  <el-tag
                    v-if="submissionStatus[q.id] === 'AC'"
                    style="margin-left:8px"
                    type="success"
                    size="small"
                    effect="dark"
                  >
                    ✅ 已通关
                  </el-tag>
                  <el-tag
                    v-else-if="submissionStatus[q.id] === 'WA'"
                    style="margin-left:8px"
                    type="warning"
                    size="small"
                  >
                    尝试过
                  </el-tag>
                </div>
                <el-button type="primary" @click="$router.push(`/student/ide/${q.id}?returnTo=/student/learning/${nodeId}&fromPhase=2`)">
                  去通关 →
                </el-button>
              </div>
            </el-card>
          </div>
          <div style="text-align:left;margin-top:24px">
            <el-button @click="activePhase = 1">← 返回学习</el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStudentKnowledgeNode, getStudentKnowledgeTree } from '@/api/knowledge'
import { listQuestionsByKnowledge } from '@/api/question'
import { getMyProgress } from '@/api/progress'
import { renderMarkdown } from '@/utils/markdown'
import { getBestStatus } from '@/api/submission'
import { useAiStream } from '@/composables/useAiStream'
import { Loading, CircleCheck, ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const nodeId = computed(() => Number(route.params.nodeId))
const activePhase = ref(0)
const node = ref(null)
const childNodes = ref([])
const exampleQuestions = ref([])
const practiceQuestions = ref([])
const submissionStatus = ref({})
const loading = ref(false)
const error = ref('')

// ── AI 知识点讲解 ──
const { content: aiExplainContent, loading: aiExplainLoading, startStream: aiExplainStart } = useAiStream()
const showAiExplain = ref(false)

function toggleAiExplain() {
  if (showAiExplain.value) {
    showAiExplain.value = false
    return
  }
  showAiExplain.value = true
  if (!aiExplainContent.value && nodeId.value) {
    aiExplainStart(`/api/v1/student/ai/explain/knowledge/${nodeId.value}`)
  }
}

const DIFF_MAP = { easy: { label: '简单', type: 'success' }, medium: { label: '中等', type: 'warning' }, hard: { label: '困难', type: 'danger' } }
const diffLabel = (d) => DIFF_MAP[d]?.label || d
const diffType = (d) => DIFF_MAP[d]?.type || 'info'
const statusLabel = (s) => ({ in_progress: '学习中', cleared: '已通关' }[s] || s)

const renderedMarkdown = computed(() => renderMarkdown(node.value?.markdownContent))

function goBack() {
  if (node.value?.parentId) {
    router.push(`/student/learning/${node.value.parentId}`)
  } else {
    router.push('/student/skill-tree')
  }
}

function enterChild(child) {
  router.push(`/student/learning/${child.id}`)
}

async function fetchChildNodes(parentId) {
  try {
    const [allNodes, progressMap] = await Promise.all([
      getStudentKnowledgeTree(),
      getMyProgress(),
    ])
    childNodes.value = (allNodes || [])
      .filter(n => n.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(n => ({
        ...n,
        _status: progressMap[n.id] || 'in_progress',
      }))
  } catch {
    childNodes.value = []
  }
}

onMounted(async () => {
  loading.value = true
  try {
    node.value = await getStudentKnowledgeNode(nodeId.value)
    // 加载子节点（带进度状态）
    await fetchChildNodes(nodeId.value)
    // 加载关联的例题和练习题
    const [examples, practices] = await Promise.all([
      listQuestionsByKnowledge(nodeId.value, 'example'),
      listQuestionsByKnowledge(nodeId.value, 'practice'),
    ])
    exampleQuestions.value = examples || []
    practiceQuestions.value = practices || []
    // 加载练习题提交状态
    await fetchSubmissionStatus()
    // 根据 URL 参数恢复之前所在阶段（从 IDE 返回时）
    const phase = Number(route.query.phase)
    if (phase >= 0 && phase <= 2) activePhase.value = phase
  } catch (e) {
    error.value = '加载知识节点失败'
  } finally {
    loading.value = false
  }
})

async function fetchSubmissionStatus() {
  const ids = practiceQuestions.value.map(q => q.id)
  if (!ids.length) return
  try {
    submissionStatus.value = await getBestStatus(ids)
  } catch {
    submissionStatus.value = {}
  }
}

// 路由变化时重新加载
watch(nodeId, async (id) => {
  if (!id) return
  loading.value = true
  activePhase.value = 0
  try {
    node.value = await getStudentKnowledgeNode(id)
    await fetchChildNodes(id)
    const [examples, practices] = await Promise.all([
      listQuestionsByKnowledge(id, 'example'),
      listQuestionsByKnowledge(id, 'practice'),
    ])
    exampleQuestions.value = examples || []
    practiceQuestions.value = practices || []
    await fetchSubmissionStatus()
    // 根据 URL 参数恢复之前所在阶段
    const phase = Number(route.query.phase)
    if (phase >= 0 && phase <= 2) activePhase.value = phase
  } catch (e) {
    error.value = '加载知识节点失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.cabin-top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;

  .back-btn {
    font-size: 14px;
    color: #606266;
    &:hover { color: var(--color-primary); }
  }

  .cabin-breadcrumb {
    font-size: 14px;
    font-weight: 500;
    color: #909399;
    &::before { content: '/'; margin-right: 12px; color: #dcdfe6; }
  }
}

.phase-tabs { margin-bottom: 32px; }
.phase-body { min-height: 500px; }
.phase-content { min-height: 400px; }

.example-card, .practice-card {
  margin-bottom: 16px;
}

/* 参考代码和解析 */
.solution-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;

  h4 {
    margin-bottom: 12px;
    color: #303133;
    font-size: 15px;
  }
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 4;

  code {
    background: transparent;
    padding: 0;
    color: inherit;
    font-size: inherit;
  }
}

.markdown-body {
  line-height: 1.8;
  h1, h2, h3 { margin: 16px 0 8px; }
  h4, h5, h6 { margin: 12px 0 6px; }
  p { margin: 8px 0; }
  ul, ol { padding-left: 24px; margin: 8px 0; }
  li { margin: 4px 0; }
  blockquote {
    margin: 12px 0; padding: 8px 16px;
    border-left: 4px solid var(--color-primary);
    background: #f5f7fa; color: #606266;
  }
  a { color: var(--color-primary); text-decoration: none; &:hover { text-decoration: underline; } }
  code {
    background: #f5f5f5; padding: 2px 6px; border-radius: 4px;
    font-family: var(--font-mono); font-size: 13px;
  }
  pre {
    background: #282c34; color: #abb2bf; padding: 16px;
    border-radius: 8px; overflow-x: auto; margin: 12px 0;
    code { background: transparent; padding: 0; color: inherit; font-size: 13px; }
  }
  .table-wrapper {
    overflow-x: auto; margin: 12px 0;
  }
  table {
    border-collapse: collapse; width: 100%;
    th, td { border: 1px solid #e4e7ed; padding: 8px 12px; text-align: left; }
    th { background: #f5f7fa; font-weight: 600; }
    tr:nth-child(even) { background: #fafafa; }
  }
  img { max-width: 100%; border-radius: 4px; }
  hr { border: none; border-top: 1px solid #e4e7ed; margin: 16px 0; }
}

/* 子节点导航 */
.child-nodes-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;

  h4 {
    margin-bottom: 16px;
    color: #303133;
    font-size: 16px;
  }
}

.child-nodes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.child-node-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--color-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }

}

.child-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f7fa;
}

.child-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  .child-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
}

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
