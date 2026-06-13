// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/home/HomePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/profile/ProfilePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resume',
      name: 'ResumeList',
      component: () => import('@/views/resume/ResumeListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resume/new',
      name: 'ResumeNew',
      component: () => import('@/views/resume/ResumeEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resume/:id/edit',
      name: 'ResumeEdit',
      component: () => import('@/views/resume/ResumeEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resume/:id/preview',
      name: 'ResumePreview',
      component: () => import('@/views/resume/ResumePreviewPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interview',
      name: 'InterviewList',
      component: () => import('@/views/interview/InterviewListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interview/new',
      name: 'InterviewNew',
      component: () => import('@/views/interview/InterviewNewPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interview/:id/room',
      name: 'InterviewRoom',
      component: () => import('@/views/interview/InterviewRoomPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interview/:id/report',
      name: 'InterviewReport',
      component: () => import('@/views/interview/InterviewReportPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/job/import',
      name: 'JobImport',
      component: () => import('@/views/job/JobImportPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/job/favorites',
      name: 'JobFavorites',
      component: () => import('@/views/job/JobFavoritePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/job/list',
      name: 'JobList',
      component: () => import('@/views/job/JobListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/job/:id',
      name: 'JobDetail',
      component: () => import('@/views/job/JobDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/job/:id/questions',
      name: 'JobQuestions',
      component: () => import('@/views/job/JobQuestionsPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/company/:id',
      name: 'CompanyDetail',
      component: () => import('@/views/job/CompanyDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications',
      name: 'ApplicationList',
      component: () => import('@/views/application/ApplicationListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/new',
      name: 'ApplicationNew',
      component: () => import('@/views/application/ApplicationEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/todos',
      name: 'ApplicationTodos',
      component: () => import('@/views/application/ApplicationTodoPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/:id',
      name: 'ApplicationDetail',
      component: () => import('@/views/application/ApplicationDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training',
      name: 'training',
      component: () => import('@/views/training/TrainingCenterPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training/plan/:id',
      name: 'training-plan-detail',
      component: () => import('@/views/training/TrainingPlanDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training/question/:id',
      name: 'training-question',
      component: () => import('@/views/training/TrainingQuestionPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training/mistakes',
      name: 'training-mistakes',
      component: () => import('@/views/training/TrainingMistakePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training/mastery',
      name: 'training-mastery',
      component: () => import('@/views/training/TrainingMasteryPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coach',
      name: 'CoachHome',
      component: () => import('@/views/coach/CoachHomePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coach/diagnosis/:id',
      name: 'CoachDiagnosisDetail',
      component: () => import('@/views/coach/CoachDiagnosisDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coach/plan/:id',
      name: 'CoachPlanDetail',
      component: () => import('@/views/coach/CoachPlanDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/analytics',
      name: 'Analytics',
      component: () => import('@/views/analytics/AnalyticsOverviewPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/AdminDashboardPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/users',
      name: 'AdminUsers',
      component: () => import('@/views/admin/AdminUserListPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/users/:id',
      name: 'AdminUserDetail',
      component: () => import('@/views/admin/AdminUserDetailPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

// 导航守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 如果本地有 token 但 store 中没有用户信息，尝试恢复
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchCurrentUser()
    } catch {
      // 恢复失败（token 过期），清除 token
      userStore.logout()
    }
  }

  // 未登录访问需认证页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 非管理员访问管理员路由，回到用户端首页
  if (to.meta.requiresAdmin && userStore.userInfo?.userRole !== 'admin') {
    next('/')
    return
  }

  // 已登录访问登录页，跳转到首页
  if (to.meta.public && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
