import request from '@/utils/request'

/** 获取知识树（管理端） */
export function getKnowledgeTree() {
  return request.get('/admin/knowledge/tree')
}

/** 创建知识节点 */
export function createKnowledgeNode(data) {
  return request.post('/admin/knowledge', data)
}

/** 更新知识节点 */
export function updateKnowledgeNode(id, data) {
  return request.put(`/admin/knowledge/${id}`, data)
}

/** 删除知识节点 */
export function deleteKnowledgeNode(id) {
  return request.delete(`/admin/knowledge/${id}`)
}

// ── 学生端 ──

/** 获取知识树（学生端） */
export function getStudentKnowledgeTree() {
  return request.get('/student/knowledge/tree')
}

/** 获取知识节点详情（学生端） */
export function getStudentKnowledgeNode(id) {
  return request.get(`/student/knowledge/${id}`)
}
