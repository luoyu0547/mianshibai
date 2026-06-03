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

  // 已登录访问登录页，跳转到首页
  if (to.meta.public && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
