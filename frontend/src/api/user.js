import request from '@/utils/request'

/** 获取用户分页列表 */
export function getUserPage(params) {
  return request.get('/admin/users', { params })
}

/** 获取用户详情 */
export function getUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}

/** 更新用户信息（昵称、角色） */
export function updateUser(id, data) {
  return request.put(`/admin/users/${id}`, null, { params: data })
}

/** 删除用户 */
export function deleteUser(id) {
  return request.delete(`/admin/users/${id}`)
}
