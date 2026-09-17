import request from '@/utils/request'

/** 获取我的学习进度 */
export function getMyProgress() {
  return request.get('/student/progress')
}

/** 成长统计数据 */
export function getGrowthStats() {
  return request.get('/student/growth/stats')
}

/** 错题本 */
export function getWrongAnswers() {
  return request.get('/student/growth/wrong-answers')
}

/** 提交热力图 */
export function getHeatmap(weeks = 20) {
  return request.get('/student/growth/heatmap', { params: { weeks } })
}

/** 错题排行榜 */
export function getWrongRanking() {
  return request.get('/student/growth/leaderboard/wrong-ranking')
}

/** 通过率排行榜 */
export function getPassRateRanking() {
  return request.get('/student/growth/leaderboard/pass-rate-ranking')
}

/** 通过题目数量排行榜 */
export function getPassedCountRanking() {
  return request.get('/student/growth/leaderboard/passed-count-ranking')
}

/** 我的通关列表 */
export function getClearedQuestions() {
  return request.get('/student/growth/cleared-questions')
}
