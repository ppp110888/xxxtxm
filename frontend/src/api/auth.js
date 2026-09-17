import request from '@/utils/request'

/** 登录 */
export function login(username, password) {
  return request.post('/auth/login', null, { params: { username, password } })
}

/** 注册 */
export function register(username, password, role = 'student') {
  return request.post('/auth/register', null, { params: { username, password, role } })
}

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request.get('/auth/me')
}

/** 退出登录 */
export function logout() {
  return request.post('/auth/logout')
}
