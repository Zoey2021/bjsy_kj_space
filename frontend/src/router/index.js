import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { role: 'STUDENT' },
    children: [
      { path: 'map', component: () => import('../views/student/CourseMap.vue') },
      { path: 'textbook/:gradeId', component: () => import('../views/student/TextbookOutline.vue') },
      { path: 'lesson/:id', component: () => import('../views/student/LessonDetail.vue') },
      { path: 'records', component: () => import('../views/student/MyRecords.vue') },
      { path: 'ai', component: () => import('../views/ai/AiCenter.vue') }
    ]
  },
  {
    path: '/teacher',
    component: () => import('../layouts/TeacherLayout.vue'),
    meta: { role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/course-map' },
      { path: 'course-map', component: () => import('../views/teacher/TeacherCourseMap.vue') },
      { path: 'course-map/grade/:gradeId', component: () => import('../views/teacher/TeacherTextbookHub.vue') },
      { path: 'activity-editor', component: () => import('../views/teacher/ActivityEditor.vue') },
      { path: 'dashboard', component: () => import('../views/teacher/ClassDashboard.vue') },
      { path: 'ai-evaluation', component: () => import('../views/teacher/AiEvaluation.vue') },
      { path: 'points', component: () => import('../views/teacher/PointsManage.vue') },
      { path: 'settings', component: () => import('../views/teacher/TeacherSettings.vue') },
      { path: 'lesson/:lessonId/stats', component: () => import('../views/teacher/LessonStats.vue') },
      { path: 'course-editor', redirect: '/teacher/course-map' },
      { path: 'matrix', redirect: { path: '/teacher/dashboard', query: { tab: 'matrix' } } },
      { path: 'ranking', redirect: { path: '/teacher/points', query: { tab: 'ranking' } } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { role: 'ADMIN' },
    children: [
      { path: 'users', component: () => import('../views/admin/UserManage.vue') }
    ]
  },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页，角色不匹配跳转对应首页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.path === '/login') {
    return next()
  }
  if (!token) {
    return next('/login')
  }
  if (to.meta.role && to.meta.role !== role) {
    const home = { STUDENT: '/student/map', TEACHER: '/teacher/course-map', ADMIN: '/admin/users' }
    return next(home[role] || '/login')
  }
  next()
})

export default router
