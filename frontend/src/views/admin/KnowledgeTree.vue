<template>
  <div class="knowledge-tree">
    <div class="page-header">
      <h3>知识树管理</h3>
      <el-button type="primary" @click="openCreate()">
        <el-icon><Plus /></el-icon> 添加根节点
      </el-button>
    </div>

    <div class="card-container">
      <el-table
        :data="treeData"
        v-loading="loading"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
      >
        <el-table-column prop="name" label="节点名称" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="讲义" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.markdownContent" type="success" size="small">已编写</el-tag>
            <el-tag v-else type="info" size="small">未编写</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row.id)">
              添加子节点
            </el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">
              编辑
            </el-button>
            <el-popconfirm title="删除此节点? 子节点将一并删除" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑节点' : '添加节点'"
      width="700px"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="父节点" v-if="!isEdit">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="节点名称" prop="name">
          <el-input v-model="form.name" placeholder="如：Java基础" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="讲义内容" prop="markdownContent">
          <el-input
            v-model="form.markdownContent"
            type="textarea"
            :rows="10"
            placeholder="支持 Markdown 格式..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getKnowledgeTree, createKnowledgeNode, updateKnowledgeNode, deleteKnowledgeNode } from '@/api/knowledge'
import { ElMessage } from 'element-plus'

// ── 数据 ──
const loading = ref(false)
const treeData = ref([])

// 构造树形数据
function buildTree(list, parentId = 0) {
  return list
    .filter(item => item.parentId === parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(item => ({
      ...item,
      children: buildTree(list, item.id),
      hasChildren: list.some(c => c.parentId === item.id),
    }))
}

async function fetchTree() {
  loading.value = true
  try {
    const list = await getKnowledgeTree()
    treeData.value = buildTree(list || [])
  } finally { loading.value = false }
}

// ── 创建/编辑 ──
const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = ref({ name: '', sortOrder: 0, markdownContent: '', parentId: 0 })
const editingId = ref(null)

const parentName = computed(() => {
  if (form.value.parentId === 0) return '根目录'
  const flatList = []
  const flatten = (nodes) => {
    nodes.forEach(n => { flatList.push(n); if (n.children) flatten(n.children) })
  }
  flatten(treeData.value)
  const parent = flatList.find(n => n.id === form.value.parentId)
  return parent?.name || '根目录'
})

const formRules = {
  name: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
}

function openCreate(parentId = 0) {
  isEdit.value = false
  editingId.value = null
  form.value = { name: '', sortOrder: 0, markdownContent: '', parentId }
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    name: row.name,
    sortOrder: row.sortOrder,
    markdownContent: row.markdownContent || '',
    parentId: row.parentId,
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateKnowledgeNode(editingId.value, form.value)
      ElMessage.success('节点已更新')
    } else {
      await createKnowledgeNode(form.value)
      ElMessage.success('节点已创建')
    }
    dialogVisible.value = false
    fetchTree()
  } finally { saving.value = false }
}

async function handleDelete(id) {
  await deleteKnowledgeNode(id)
  ElMessage.success('节点已删除')
  fetchTree()
}

onMounted(fetchTree)
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h3 { margin: 0; font-size: 18px; }
}
</style>
