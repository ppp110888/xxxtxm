<template>
  <div class="skill-tree-page">
    <div class="page-header">
      <h2>📚 技能树</h2>
      <p>{{ progressSummary }}</p>
    </div>

    <div class="skill-tree-container" v-loading="loading">
      <template v-if="treeData.length">
        <div
          v-for="node in treeData"
          :key="node.id"
          class="skill-node-card"
          :class="nodeStatusClass(node)"
          @click="enterNode(node)"
        >
          <div class="node-icon">
            <el-icon :size="28">
              <Loading v-if="node._status === 'in_progress'" />
              <CircleCheck v-else />
            </el-icon>
          </div>
          <div class="node-info">
            <div class="node-name">{{ node.name }}</div>
            <div class="node-status">
              <el-tag
                :type="node._status === 'cleared' ? 'success' : ''"
                size="small"
              >
                {{ statusLabel(node._status) }}
              </el-tag>
            </div>
          </div>
          <div class="node-arrow" v-if="node.children?.length">
            <span>含 {{ node.children.length }} 个子节点</span>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无知识节点数据" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentKnowledgeTree } from '@/api/knowledge'
import { getMyProgress } from '@/api/progress'

const router = useRouter()

const loading = ref(false)
const treeData = ref([])
const progressMap = ref({})

const statusLabel = (s) => ({ in_progress: '学习中', cleared: '已通关' }[s] || s)

const progressSummary = computed(() => {
  const total = treeData.value.length
  const cleared = treeData.value.filter(n => n._status === 'cleared').length
  return total ? `已完成 ${cleared} / ${total} 个知识点` : ''
})

function buildTree(list, parentId = 0) {
  return list
    .filter(item => item.parentId === parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(item => ({
      ...item,
      _status: progressMap.value[item.id] || 'in_progress',
      children: buildTree(list, item.id),
    }))
}

function nodeStatusClass(node) {
  return {
    'node-in-progress': node._status === 'in_progress',
    'node-cleared': node._status === 'cleared',
  }
}

function enterNode(node) {
  router.push(`/student/learning/${node.id}`)
}

async function fetchData() {
  loading.value = true
  try {
    const [list, progress] = await Promise.all([getStudentKnowledgeTree(), getMyProgress()])
    progressMap.value = progress || {}
    treeData.value = buildTree(list || [])
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.page-header {
  text-align: center; margin-bottom: 32px;
  h2 { font-size: 24px; margin-bottom: 8px; }
  p { color: #909399; }
}

.skill-tree-container {
  max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px;
}

.skill-node-card {
  display: flex; align-items: center; gap: 16px;
  padding: 20px; border-radius: var(--card-radius);
  background: #fff; box-shadow: var(--card-shadow);
  cursor: pointer; transition: all 0.3s;

  &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }

  &.node-in-progress { border-left: 4px solid var(--color-warning); }
  &.node-cleared { border-left: 4px solid var(--color-success); }
}

.node-info { flex: 1; }
.node-name { font-size: 16px; font-weight: 500; margin-bottom: 4px; }
.node-arrow { color: #909399; font-size: 13px; }
</style>
