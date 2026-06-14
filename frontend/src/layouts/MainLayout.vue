<!-- src/layouts/MainLayout.vue -->
<template>
  <div class="main-layout">
    <header class="main-layout__header">
      <div class="main-layout__header-inner">
        <router-link to="/" class="main-layout__brand">
          <span class="main-layout__brand-text">面试吧</span>
        </router-link>

        <nav class="main-layout__nav">
          <router-link
            v-for="item in visibleNavItems"
            :key="item.to"
            :to="item.to"
            class="main-layout__nav-link"
            @click="closeMobileMenu"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="main-layout__actions">
          <button
            class="main-layout__menu-btn"
            type="button"
            aria-label="切换菜单"
            @click="toggleMobileMenu"
          >
            <span class="main-layout__menu-icon" :class="{ 'main-layout__menu-icon--open': mobileMenuOpen }"></span>
          </button>

          <div class="main-layout__user">
            <el-dropdown v-if="userStore.isLoggedIn" trigger="click">
              <div class="main-layout__user-trigger">
                <div class="main-layout__avatar">
                  <img
                    v-if="avatarUrl"
                    :src="avatarUrl"
                    :alt="displayName"
                    class="main-layout__avatar-img"
                  />
                  <span v-else class="main-layout__avatar-initial">
                    {{ avatarInitial }}
                  </span>
                </div>
                <span class="main-layout__username">
                  {{ displayName }}
                </span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">个人资料</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <router-link v-else to="/login" class="main-layout__login-link">
              <NbButton type="primary">登录</NbButton>
            </router-link>
          </div>
        </div>
      </div>

      <transition name="main-layout__dropdown">
        <nav v-show="mobileMenuOpen" class="main-layout__mobile-nav">
          <router-link
            v-for="item in visibleNavItems"
            :key="item.to"
            :to="item.to"
            class="main-layout__mobile-link"
            @click="closeMobileMenu"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </transition>
    </header>

    <main class="main-layout__main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import NbButton from '@/components/NbButton.vue'

interface NavItem {
  label: string
  to: string
  group: 'core' | 'growth' | 'account'
  adminOnly?: boolean
}

const navItems: NavItem[] = [
  { label: '首页', to: '/', group: 'core' },
  { label: '我的简历', to: '/resume', group: 'core' },
  { label: '模拟面试', to: '/interview', group: 'core' },
  { label: '职位情报', to: '/job/import', group: 'core' },
  { label: '投递管理', to: '/applications', group: 'growth' },
  { label: '训练中心', to: '/training', group: 'growth' },
  { label: '求职教练', to: '/coach', group: 'growth' },
  { label: '管理后台', to: '/admin', group: 'account', adminOnly: true },
  { label: '个人资料', to: '/profile', group: 'account' },
]

const userStore = useUserStore()
const router = useRouter()

const mobileMenuOpen = ref(false)

const isAdmin = computed(() => userStore.userInfo?.userRole === 'admin')

const visibleNavItems = computed(() =>
  navItems.filter((item) => !item.adminOnly || isAdmin.value),
)

const avatarUrl = computed(() => {
  const avatar = userStore.userInfo?.userAvatar
  return avatar && avatar.trim() !== '' ? avatar : ''
})

const displayName = computed(
  () => userStore.userInfo?.userName || userStore.userInfo?.userAccount || '用户',
)

const avatarInitial = computed(
  () => userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U',
)

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--nb-bg);
}

.main-layout__header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--nb-border-color);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.main-layout__header-inner {
  max-width: var(--nb-container);
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.main-layout__brand {
  text-decoration: none;
  flex-shrink: 0;
}

/* 品牌文字 — 渐变效果 */
.main-layout__brand-text {
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 700;
  background: var(--nb-primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.main-layout__nav {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.main-layout__nav-link {
  font-family: var(--font-body);
  font-weight: 500;
  font-size: 14px;
  color: var(--nb-muted);
  text-decoration: none;
  padding: 8px 14px;
  border-radius: var(--nb-radius);
  transition: var(--nb-transition);
}

.main-layout__nav-link:hover {
  color: var(--nb-ink);
  background: var(--nb-muted-surface);
}

.main-layout__nav-link.router-link-active {
  color: var(--nb-primary);
  background: var(--nb-primary-light);
  font-weight: 600;
}

.main-layout__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.main-layout__menu-btn {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
  box-shadow: var(--nb-shadow-xs);
  cursor: pointer;
  transition: var(--nb-transition);
}

.main-layout__menu-btn:hover {
  border-color: var(--nb-primary);
  box-shadow: var(--nb-shadow-sm);
}

.main-layout__menu-icon,
.main-layout__menu-icon::before,
.main-layout__menu-icon::after {
  display: block;
  width: 20px;
  height: 2px;
  background: var(--nb-ink);
  border-radius: 2px;
  transition: var(--nb-transition);
}

.main-layout__menu-icon {
  position: relative;
}

.main-layout__menu-icon::before,
.main-layout__menu-icon::after {
  content: '';
  position: absolute;
  left: 0;
}

.main-layout__menu-icon::before {
  top: -7px;
}

.main-layout__menu-icon::after {
  top: 7px;
}

.main-layout__menu-icon--open {
  background: transparent;
}

.main-layout__menu-icon--open::before {
  top: 0;
  transform: rotate(45deg);
}

.main-layout__menu-icon--open::after {
  top: 0;
  transform: rotate(-45deg);
}

.main-layout__user {
  display: flex;
  align-items: center;
}

.main-layout__user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--nb-radius);
  transition: var(--nb-transition);
}

.main-layout__user-trigger:hover {
  background: var(--nb-primary-light);
}

.main-layout__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 2px solid var(--nb-primary-light);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--nb-primary-gradient);
  color: #fff;
  flex-shrink: 0;
}

.main-layout__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.main-layout__avatar-initial {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
}

.main-layout__username {
  font-weight: 500;
  font-size: 14px;
  color: var(--nb-ink);
}

.main-layout__main {
  max-width: var(--nb-container);
  margin: 0 auto;
  padding: 28px 24px;
}

.main-layout__login-link {
  text-decoration: none;
}

.main-layout__mobile-nav {
  display: none;
}

.main-layout__mobile-link {
  display: block;
  padding: 14px 24px;
  font-family: var(--font-body);
  font-weight: 500;
  font-size: 15px;
  color: var(--nb-muted);
  text-decoration: none;
  border-bottom: 1px solid var(--nb-border-color-light);
  transition: var(--nb-transition);
}

.main-layout__mobile-link:hover,
.main-layout__mobile-link.router-link-active {
  color: var(--nb-primary);
  background: var(--nb-primary-light);
  font-weight: 600;
}

.main-layout__dropdown-enter-active,
.main-layout__dropdown-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}

.main-layout__dropdown-enter-from,
.main-layout__dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 899px) {
  .main-layout__nav {
    display: none;
  }

  .main-layout__menu-btn {
    display: flex;
  }

  .main-layout__mobile-nav {
    display: flex;
    flex-direction: column;
    border-top: var(--nb-border);
    background: var(--nb-surface);
    overflow: hidden;
  }

  .main-layout__username {
    display: none;
  }
}
</style>
