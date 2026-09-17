import { createRouter, createWebHistory } from 'vue-router'

/**
 * 静态路由 — 所有角色公共
 */
const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '404', noAuth: true },
  },
]

/**
 * 管理端动态路由
 */
export const adminRoutes = [
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/knowledge',
    children: [
      {
        path: 'knowledge',
        name: 'KnowledgeTree',
        component: () => import('@/views/admin/KnowledgeTree.vue'),
        meta: { title: '知识树管理', icon: 'Collection' },
      },
      {
        path: 'questions',
        name: 'QuestionList',
        component: () => import('@/views/admin/QuestionList.vue'),
        meta: { title: '题库管理', icon: 'Document' },
      },
      {
        path: 'cases',
        name: 'CaseUpload',
        component: () => import('@/views/admin/CaseUpload.vue'),
        meta: { title: '测试用例', icon: 'Files' },
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/admin/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
    ],
  },
]

/**
 * 学生端动态路由
 */
export const studentRoutes = [
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    redirect: '/student/skill-tree',
    children: [
      {
        path: 'skill-tree',
        name: 'SkillTree',
        component: () => import('@/views/student/SkillTree.vue'),
        meta: { title: '技能树', icon: 'Guide' },
      },
      {
        path: 'learning/:nodeId',
        name: 'LearningCabin',
        component: () => import('@/views/student/LearningCabin.vue'),
        meta: { title: '学习舱', icon: 'Reading' },
      },
      {
        path: 'ide/:questionId',
        name: 'IdeWorkspace',
        component: () => import('@/views/student/IdeWorkspace.vue'),
        meta: { title: '在线IDE', icon: 'Monitor' },
      },
      {
        path: 'growth',
        name: 'GrowthCenter',
        component: () => import('@/views/student/GrowthCenter.vue'),
        meta: { title: '成长中心', icon: 'DataAnalysis' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
