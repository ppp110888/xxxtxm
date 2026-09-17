<template>
  <div class="growth-center" v-loading="loading">
    <h2>📊 个人成长中心</h2>

    <!-- 数据画像卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-num">{{ stats.totalSubmissions }}</div>
          <div class="stat-label">总提交数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-num" style="color:#67C23A">{{ stats.acRate }}%</div>
          <div class="stat-label">AC 通过率</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-num">{{ stats.totalAc }}</div>
          <div class="stat-label">已 AC 题数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-num">{{ stats.preferredLanguage }}</div>
          <div class="stat-label">最常用语言</div>
        </div>
      </el-col>
    </el-row>

    <!-- 学习进度 -->
    <el-row :gutter="20" class="stats-row" style="margin-top:16px">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num" style="color:#67C23A">{{ stats.clearedNodes }}</div>
          <div class="stat-label">已通关知识点</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num" style="color:#E6A23C">{{ stats.inProgressNodes }}</div>
          <div class="stat-label">学习中</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num">{{ stats.totalNodes }}</div>
          <div class="stat-label">已解锁知识点</div>
        </div>
      </el-col>
    </el-row>

    <!-- 排行榜 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <div class="card-container">
          <h3 style="margin:0 0 16px 0">🏆 全站排行榜</h3>
          <el-tabs v-model="leaderboardTab">
            <el-tab-pane label="📝 错题排行榜" name="wrong">
              <el-table :data="wrongRanking" stripe size="small" v-loading="rankingLoading" empty-text="暂无数据">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="questionTitle" label="题目名称" min-width="200">
                  <template #default="{ row }">
                    <el-link type="primary" @click="goToQuestion(row.questionId)">{{ row.questionTitle }}</el-link>
                  </template>
                </el-table-column>
                <el-table-column prop="difficulty" label="难度" width="80">
                  <template #default="{ row }">
                    <el-tag :type="diffTagType(row.difficulty)" size="small">{{ diffLabel(row.difficulty) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="wrongUserCount" label="错误人数" width="100" sortable />
                <el-table-column prop="totalAttempts" label="总错误次数" width="110" sortable />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="📊 通过率排行榜" name="passrate">
              <el-table :data="passRateRanking" stripe size="small" v-loading="rankingLoading" empty-text="暂无数据">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="questionTitle" label="题目名称" min-width="200">
                  <template #default="{ row }">
                    <el-link type="primary" @click="goToQuestion(row.questionId)">{{ row.questionTitle }}</el-link>
                  </template>
                </el-table-column>
                <el-table-column prop="difficulty" label="难度" width="80">
                  <template #default="{ row }">
                    <el-tag :type="diffTagType(row.difficulty)" size="small">{{ diffLabel(row.difficulty) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="passRate" label="通过率" width="180" sortable>
                  <template #default="{ row }">
                    <div style="display:flex;align-items:center;gap:8px">
                      <el-progress :percentage="row.passRate" :stroke-width="8"
                        :color="row.passRate >= 70 ? '#67C23A' : row.passRate >= 40 ? '#E6A23C' : '#F56C6C'"
                        style="flex:1" />
                      <span style="font-size:12px;white-space:nowrap">{{ row.passRate }}%</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="通过/尝试" width="100">
                  <template #default="{ row }">{{ row.acUsers }} / {{ row.totalUsers }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="👑 通关数排行榜" name="passedCount">
              <el-table :data="passedCountRanking" stripe size="small" v-loading="rankingLoading" empty-text="暂无数据">
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="nickname" label="用户" min-width="180" />
                <el-table-column prop="passedCount" label="通过题目数" width="140" sortable>
                  <template #default="{ row }">
                    <div style="display:flex;align-items:center;gap:8px">
                      <el-progress :percentage="Math.min(row.passedCount * 10, 100)" :stroke-width="8"
                        :color="row.passedCount >= 20 ? '#FFD700' : row.passedCount >= 10 ? '#67C23A' : '#409EFF'"
                        style="flex:1" />
                      <span style="font-size:13px;font-weight:600;white-space:nowrap">{{ row.passedCount }} 题</span>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
    </el-row>

    <!-- 我的通关 -->
    <div class="card-container" style="margin-top:20px">
      <h3 style="margin-bottom:16px">✅ 我的通关</h3>
      <div v-if="clearedQuestions.length" class="cleared-grid">
        <div
          v-for="q in clearedQuestions"
          :key="q.questionId"
          class="cleared-card"
          @click="goToQuestion(q.questionId)"
        >
          <div class="cleared-card-header">
            <span class="cleared-title">{{ q.title }}</span>
            <el-tag :type="diffTagType(q.difficulty)" size="small">{{ diffLabel(q.difficulty) }}</el-tag>
          </div>
          <div class="cleared-card-meta">
            <span>{{ q.languageLimit === 'all' ? '🌐 多语言' : q.languageLimit === 'java' ? '☕ Java' : '🐍 Python' }}</span>
            <span style="color:#909399;font-size:12px">通关于 {{ formatDate(q.acTime) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="还没有通关的题目，快去技能树刷题吧！" :image-size="80" />
    </div>

    <!-- 提交热力图 -->
    <div class="card-container" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="margin:0">🔥 提交热力图</h3>
        <div class="heatmap-legend">
          <span>少</span>
          <span class="legend-cell" style="background:#ebedf0"></span>
          <span class="legend-cell" style="background:#9be9a8"></span>
          <span class="legend-cell" style="background:#40c463"></span>
          <span class="legend-cell" style="background:#30a14e"></span>
          <span class="legend-cell" style="background:#216e39"></span>
          <span>多</span>
        </div>
      </div>
      <div v-if="heatmapData.length" class="heatmap-grid" ref="heatmapRef">
        <!-- 月份标签 -->
        <div class="heatmap-months">
          <span v-for="(m, i) in monthLabels" :key="i" :style="{ gridColumn: m.col + ' / span ' + m.span }">
            {{ m.label }}
          </span>
        </div>
        <!-- 星期标签 -->
        <div class="heatmap-days">
          <span v-for="d in dayLabels" :key="d.label" :style="{ gridRow: d.row }">
            {{ d.label }}
          </span>
        </div>
        <!-- 格子 -->
        <div
          v-for="cell in heatmapCells"
          :key="cell.date"
          class="heatmap-cell"
          :style="{ gridColumn: cell.col, gridRow: cell.row, background: cell.color }"
          :title="cell.tooltip"
        >
          <el-tooltip :content="cell.tooltip" placement="top" :show-after="200">
            <span style="display:block;width:100%;height:100%"></span>
          </el-tooltip>
        </div>
      </div>
      <el-empty v-else description="暂无提交记录，快去 IDE 写代码吧！" :image-size="80" />
    </div>

    <!-- 错题本 -->
    <div class="card-container" style="margin-top:20px">
      <h3 style="margin-bottom:16px">📝 智能错题本</h3>
      <el-tabs v-model="errorTab">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="WA (答案错误)" name="WA" />
        <el-tab-pane label="TLE (超时)" name="TLE" />
        <el-tab-pane label="RE (运行错误)" name="RE" />
        <el-tab-pane label="CE (编译错误)" name="CE" />
      </el-tabs>
      <div v-if="filteredWrongAnswers.length">
        <div v-for="wa in filteredWrongAnswers" :key="wa.id" class="wa-item">
          <el-tag :type="statusTagType(wa.status)" size="small">{{ statusLabel(wa.status) }}</el-tag>
          <span style="margin-left:8px">题目 #{{ wa.questionId }}</span>
          <span class="wa-time">{{ formatTime(wa.createTime) }}</span>
          <span class="wa-lang">{{ wa.language?.toUpperCase() }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无错题记录，继续保持！" />
    </div>

    <!-- AI 学情诊断 -->
    <div class="card-container" style="margin-top:20px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3 style="margin:0">🤖 AI 学情诊断</h3>
        <el-button
          type="primary" size="small"
          :loading="reportLoading"
          @click="generateReport"
        >
          {{ reportContent ? '重新生成' : '生成诊断报告' }}
        </el-button>
      </div>
      <div v-if="reportContent" class="report-body markdown-body" v-html="renderedReport"></div>
      <div v-else-if="reportLoading" class="report-loading">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p>AI 正在分析您的学习数据...</p>
      </div>
      <div v-else class="report-empty">
        <p>点击「生成诊断报告」，AI 将分析您的错题规律</p>
        <p style="color:#909399;font-size:13px">
          （需要至少有一些提交记录才能生成有意义的报告）
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { JUDGE_STATUS_MAP, DIFFICULTY_MAP } from '@/utils/constants'
import { useAiStream } from '@/composables/useAiStream'
import { renderMarkdown } from '@/utils/markdown'
import { getGrowthStats, getWrongAnswers, getHeatmap, getWrongRanking, getPassRateRanking, getPassedCountRanking, getClearedQuestions } from '@/api/progress'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

const router = useRouter()
const errorTab = ref('all')
const loading = ref(false)
const wrongAnswers = ref([])
const heatmapData = ref([])

// ── 排行榜 ──
const leaderboardTab = ref('wrong')
const rankingLoading = ref(false)
const wrongRanking = ref([])
const passRateRanking = ref([])
const passedCountRanking = ref([])
const clearedQuestions = ref([])

// ── AI 报告 ──
const { content: reportContent, loading: reportLoading, startStream: reportStart } = useAiStream()
const renderedReport = computed(() => renderMarkdown(reportContent.value))

function generateReport() {
  reportStart('/api/v1/student/ai/report')
}

const stats = reactive({
  totalSubmissions: 0,
  acRate: 0,
  totalAc: 0,
  preferredLanguage: '--',
  clearedNodes: 0,
  inProgressNodes: 0,
  totalNodes: 0,
})

// ── 热力图计算 ──────────────────────────────────
const CELL_SIZE = 13
const CELL_GAP = 3
const DAY_LABELS = ['一', '三', '五']  // 只标奇数行节省空间

const dayLabels = computed(() => {
  const rows = []
  for (let r = 2; r <= 8; r++) {  // row 2-8 = Mon-Sun (row 1 = months)
    const idx = r - 2  // 0=Mon ... 6=Sun
    const label = DAY_LABELS[Math.floor(idx / 2)]  // '一','三','五' 各覆盖2天
    const show = idx % 2 === 0
    rows.push({ row: r, label: show ? label : '' })
  }
  return rows
})

const monthLabels = computed(() => {
  if (!heatmapData.value.length) return []
  const months = []
  let prevMonth = -1
  let startCol = 1
  heatmapData.value.forEach((item, i) => {
    const d = dayjs(item.date)
    const m = d.month()
    if (m !== prevMonth) {
      if (prevMonth !== -1) {
        months[months.length - 1].span = startCol - months[months.length - 1].col + 1
      }
      // col = weekIndex + 1 (grid columns are 1-indexed, first data col is month labels)
      const weekIdx = Math.floor(i / 7) + 1
      months.push({ label: `${m + 1}月`, col: weekIdx, span: 1 })
      prevMonth = m
    }
    startCol = Math.floor(i / 7) + 1
  })
  if (months.length) {
    months[months.length - 1].span = startCol - months[months.length - 1].col + 1
  }
  return months
})

const heatmapCells = computed(() => {
  if (!heatmapData.value.length) return []
  return heatmapData.value.map((item, i) => {
    const weekIdx = Math.floor(i / 7) + 1  // column index (1-based)
    const dayIdx = i % 7                     // 0=Mon ... 6=Sun
    const row = dayIdx + 2                   // grid row (2=Mon ... 8=Sun)
    const count = item.count || 0
    const color = countToColor(count)
    const dateStr = dayjs(item.date).format('YYYY-MM-DD')
    const tooltip = count > 0
      ? `${dateStr} — ${count} 次提交`
      : `${dateStr} — 无提交`
    return { date: item.date, col: weekIdx, row, color, tooltip, count }
  })
})

function countToColor(c) {
  if (c <= 0) return '#ebedf0'
  if (c <= 2) return '#9be9a8'
  if (c <= 5) return '#40c463'
  if (c <= 10) return '#30a14e'
  return '#216e39'
}

// ── 错题筛选 ────────────────────────────────────
const filteredWrongAnswers = computed(() => {
  if (errorTab.value === 'all') return wrongAnswers.value
  return wrongAnswers.value.filter(w => w.status === errorTab.value)
})
const statusLabel = (s) => JUDGE_STATUS_MAP[s]?.label || s
const statusTagType = (s) => {
  const map = { WA: 'danger', TLE: 'warning', MLE: 'warning', RE: 'danger', CE: 'info' }
  return map[s] || 'info'
}
const formatTime = (t) => dayjs(t).format('YYYY-MM-DD HH:mm')
const formatDate = (t) => dayjs(t).format('YYYY-MM-DD')

// ── 排行榜工具函数 ──
const diffLabel = (d) => DIFFICULTY_MAP[d]?.label || d
const diffTagType = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')

function goToQuestion(questionId) {
  router.push(`/student/ide/${questionId}`)
}

async function fetchLeaderboard() {
  rankingLoading.value = true
  try {
    const [wrongData, passData, passedCountData, clearedData] = await Promise.all([
      getWrongRanking(),
      getPassRateRanking(),
      getPassedCountRanking(),
      getClearedQuestions(),
    ])
    wrongRanking.value = wrongData || []
    passRateRanking.value = passData || []
    passedCountRanking.value = passedCountData || []
    clearedQuestions.value = clearedData || []
  } catch { /* ignore */ }
  finally { rankingLoading.value = false }
}

async function fetchData() {
  loading.value = true
  try {
    const [s, wrongs, heatmap] = await Promise.all([
      getGrowthStats(),
      getWrongAnswers(),
      getHeatmap(20),
    ])
    Object.assign(stats, s)
    wrongAnswers.value = wrongs || []
    heatmapData.value = heatmap || []
  } catch { /* ignore */ }
  finally { loading.value = false }
  // 排行榜独立加载（不阻塞个人数据）
  fetchLeaderboard()
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.growth-center {
  max-width: 1000px; margin: 0 auto; padding: 20px;
  h2 { margin-bottom: 24px; }
}

.stats-row { margin-bottom: 20px; }

.stat-card {
  background: #fff; border-radius: var(--card-radius);
  box-shadow: var(--card-shadow); padding: 24px; text-align: center;
}

.stat-num {
  font-size: 32px; font-weight: 700; color: var(--color-primary);
}

.stat-label {
  margin-top: 8px; font-size: 14px; color: #909399;
}

/* ── 热力图 ──────────────────────────────── */
.heatmap-grid {
  display: grid;
  grid-template-columns: 32px repeat(auto-fill, 13px);
  grid-template-rows: 20px repeat(7, 13px);
  gap: 3px;
  justify-content: flex-start;
  overflow-x: auto;
  padding-bottom: 8px;
}
.heatmap-months {
  display: contents;
  font-size: 11px; color: #909399;
  & > span { display: block; white-space: nowrap; }
}
.heatmap-days {
  display: contents;
  font-size: 11px; color: #909399; text-align: right; line-height: 13px;
  & > span { display: block; }
}
.heatmap-cell {
  width: 13px; height: 13px; border-radius: 2px; cursor: pointer;
  transition: transform 0.1s;
  &:hover { transform: scale(1.4); outline: 1px solid #303133; }
}

.heatmap-legend {
  display: flex; align-items: center; gap: 4px; font-size: 12px; color: #909399;
}
.legend-cell {
  width: 13px; height: 13px; border-radius: 2px;
}

.report-body, .report-empty, .report-loading {
  padding: 24px; background: #fafafa;
  border-radius: var(--card-radius); min-height: 120px;
}
.report-empty, .report-loading {
  text-align: center; color: #909399;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
}

/* Report Markdown styles */
.report-body {
  line-height: 1.8; font-size: 14px;
  h1, h2, h3 { margin: 16px 0 8px; }
  h4, h5, h6 { margin: 12px 0 6px; }
  p { margin: 0 0 8px; }
  ul, ol { padding-left: 20px; margin: 8px 0; }
  li { margin: 4px 0; }
  code {
    background: #f0f0f0; padding: 2px 6px; border-radius: 4px;
    font-family: var(--font-mono); font-size: 13px;
  }
  pre {
    background: #282c34; color: #abb2bf; padding: 12px;
    border-radius: 6px; overflow-x: auto;
    code { background: transparent; padding: 0; color: inherit; }
  }
  blockquote {
    margin: 8px 0; padding: 6px 14px;
    border-left: 3px solid var(--color-primary);
    background: #f0f7ff; color: #606266;
  }
}

.wa-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 0; border-bottom: 1px solid #f0f0f0;
}
.wa-time { color: #909399; font-size: 12px; margin-left: auto; }
.wa-lang { color: #C0C4CC; font-size: 12px; }

/* ── 通关卡片 ── */
.cleared-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.cleared-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 18px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  &:hover {
    border-color: var(--color-primary);
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    transform: translateY(-1px);
  }
}
.cleared-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.cleared-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}
.cleared-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}
</style>
