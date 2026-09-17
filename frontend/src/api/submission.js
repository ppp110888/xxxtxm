import request from '@/utils/request'

/** 提交代码 */
export function submitCode(questionId, language, code) {
  return request.post('/student/submit', null, {
    params: { questionId, language, code }
  })
}

/** 获取我的提交列表 */
export function getMySubmissions(params) {
  return request.get('/student/submissions', { params })
}

/** 获取提交详情 */
export function getSubmissionDetail(id) {
  return request.get(`/student/submissions/${id}`)
}

/** 批量查询最佳提交状态 */
export function getBestStatus(questionIds) {
  return request.get('/student/submissions/best-status', {
    params: { questionIds: questionIds.join(',') }
  })
}
