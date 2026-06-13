<!-- src/layouts/MainLayout.vue -->
<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <header class="main-layout__header">
      <div class="main-layout__header-inner">
        <router-link to="/" class="main-layout__brand">
          <span class="main-layout__brand-text">面试吧</span>
        </router-link>

        <nav class="main-layout__nav">
          <router-link to="/" class="main-layout__nav-link">首页</router-link>
          <router-link to="/resume" class="main-layout__nav-link">我的简历</router-link>
          <router-link to="/interview" class="main-layout__nav-link">模拟面试</router-link>
          <router-link to="/job/import" class="main-layout__nav-link">职位情报</router-link>
          <router-link to="/applications" class="main-layout__nav-link">投递管理</router-link>
          <router-link to="/training" class="main-layout__nav-link">训练中心</router-link>
          <router-link v-if="userStore.userInfo?.userRole === 'admin'" to="/admin" class="main-layout__nav-link">
            管理后台
          </router-link>
          <router-link to="/profile" class="main-layout__nav-link">个人资料</router-link>
        </nav>

        <div class="main-layout__user">
          <el-dropdown v-if="userStore.isLoggedIn" trigger="click">
            <div class="main-layout__user-trigger">
              <div class="main-layout__avatar">
                {{ userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U' }}
              </div>
              <span class="main-layout__username">
                {{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || '用户' }}
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
    </header>

    <!-- 主体内容 -->
    <main class="main-layout__main">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import NbButton from '@/components/NbButton.vue'

const userStore = useUserStore()
const router = useRouter()

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
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  box-shadow: 0 2px 0 rgba(0, 0, 0, 0.05);
}

.main-layout__header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.main-layout__brand {
  text-decoration: none;
}

.main-layout__brand-text {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--nb-text);
  text-shadow: 2px 2px 0 var(--nb-primary);
}

.main-layout__nav {
  display: flex;
  gap: 24px;
}

.main-layout__nav-link {
  font-family: var(--font-body);
  font-weight: 500;
  font-size: 15px;
  color: var(--nb-muted);
  text-decoration: none;
  padding: 8px 4px;
  border-bottom: 3px solid transparent;
  transition: var(--nb-transition);
}

.main-layout__nav-link:hover,
.main-layout__nav-link.router-link-active {
  color: var(--nb-text);
  border-bottom-color: var(--nb-primary);
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
  background: rgba(108, 92, 231, 0.08);
}

.main-layout__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--nb-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  border: 2px solid var(--nb-border);
}

.main-layout__username {
  font-weight: 500;
  color: var(--nb-text);
}

.main-layout__main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.main-layout__login-link {
  text-decoration: none;
}
</style>
