import request from '@/utils/request'

// ── 管理端 ──

/** 分页查询题目（管理端） */
export function listQuestions(params) {
  return request.get('/admin/questions', { params })
}

/** 获取题目详情（管理端） */
export function getQuestion(id) {
  return request.get(`/admin/questions/${id}`)
}

/** 创建题目 */
export function createQuestion(data) {
  return request.post('/admin/questions', data)
}

/** 更新题目 */
export function updateQuestion(id, data) {
  return request.put(`/admin/questions/${id}`, data)
}

/** 删除题目 */
export function deleteQuestion(id) {
  return request.delete(`/admin/questions/${id}`)
}

// ── 学生端 ──

/** 获取题目详情（学生端） */
export function getStudentQuestion(id) {
  return request.get(`/student/questions/${id}`)
}

/** 按知识节点查询关联题目（学生端） */
export function listQuestionsByKnowledge(knowledgeId, type) {
  return request.get('/student/questions', { params: { knowledgeId, type } })
}

// ── 测试用例管理 ──

/** 获取题目的测试用例列表 */
export function listTestCases(questionId) {
  return request.get(`/admin/cases`, { params: { questionId } })
}

/** 上传测试用例 */
export function uploadTestCase(questionId, inputFile, outputFile, isVisible = false) {
  const formData = new FormData()
  formData.append('questionId', questionId)
  formData.append('inputFile', inputFile)
  formData.append('outputFile', outputFile)
  formData.append('isVisible', isVisible ? '1' : '0')
  return request.post('/admin/cases/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除测试用例 */
export function deleteTestCase(id) {
  return request.delete(`/admin/cases/${id}`)
}
