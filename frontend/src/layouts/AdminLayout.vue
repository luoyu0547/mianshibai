<template>
  <div class="admin-layout">
    <aside class="admin-layout__sidebar" :class="{ 'admin-layout__sidebar--open': sidebarOpen }">
      <router-link to="/admin" class="admin-layout__brand">面试吧 Admin</router-link>
      <nav class="admin-layout__nav">
        <router-link to="/admin" class="admin-layout__link" @click="closeSidebar">平台总览</router-link>
        <router-link to="/admin/users" class="admin-layout__link" @click="closeSidebar">用户管理</router-link>
        <router-link to="/admin/job-crawl" class="admin-layout__link" @click="closeSidebar">职位采集</router-link>
        <router-link to="/" class="admin-layout__link admin-layout__link--ghost">返回用户端</router-link>
      </nav>
    </aside>

    <div
      v-show="sidebarOpen"
      class="admin-layout__overlay"
      @click="closeSidebar"
    ></div>

    <section class="admin-layout__content">
      <header class="admin-layout__header">
        <button
          class="admin-layout__menu-btn"
          type="button"
          aria-label="切换侧边栏"
          @click="toggleSidebar"
        >
          <span class="admin-layout__menu-icon"></span>
        </button>
        <span class="admin-layout__header-label">管理员后台</span>
        <strong class="admin-layout__header-user">
          {{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || 'admin' }}
        </strong>
      </header>
      <main class="admin-layout__main">
        <div class="admin-layout__container">
          <slot />
        </div>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const sidebarOpen = ref(false)

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

function closeSidebar() {
  sidebarOpen.value = false
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: var(--nb-bg);
}

.admin-layout__sidebar {
  padding: 24px 16px;
  background: var(--nb-card);
  border-right: var(--nb-border);
  box-shadow: var(--nb-shadow-sm);
}

.admin-layout__brand {
  display: block;
  margin-bottom: 32px;
  padding: 0 8px;
  color: var(--nb-text);
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 800;
  text-decoration: none;
  text-shadow: 2px 2px 0 var(--nb-primary);
}

.admin-layout__nav {
  display: grid;
  gap: 10px;
}

.admin-layout__link {
  display: block;
  padding: 12px 14px;
  color: var(--nb-text);
  text-decoration: none;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
  box-shadow: var(--nb-shadow-xs);
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  transition: var(--nb-transition);
}

.admin-layout__link:hover {
  box-shadow: var(--nb-shadow-sm);
  transform: translate(-1px, -1px);
}

.admin-layout__link.router-link-active {
  background: var(--nb-primary);
  color: #fff;
  box-shadow: var(--nb-shadow-sm);
}

.admin-layout__link--ghost {
  margin-top: 16px;
  background: transparent;
  border-style: dashed;
  box-shadow: none;
  font-size: 13px;
}

.admin-layout__link--ghost:hover {
  border-style: solid;
  box-shadow: var(--nb-shadow-xs);
}

.admin-layout__content {
  min-width: 0;
}

.admin-layout__header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  font-family: var(--font-heading);
  font-weight: 700;
}

.admin-layout__header-label {
  flex: 1;
  color: var(--nb-muted);
}

.admin-layout__header-user {
  color: var(--nb-text);
}

.admin-layout__menu-btn {
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

.admin-layout__menu-icon,
.admin-layout__menu-icon::before,
.admin-layout__menu-icon::after {
  display: block;
  width: 18px;
  height: 2.5px;
  background: var(--nb-text);
  border-radius: 2px;
}

.admin-layout__menu-icon {
  position: relative;
}

.admin-layout__menu-icon::before,
.admin-layout__menu-icon::after {
  content: '';
  position: absolute;
  left: 0;
}

.admin-layout__menu-icon::before {
  top: -6px;
}

.admin-layout__menu-icon::after {
  top: 6px;
}

.admin-layout__main {
  padding: 32px;
}

.admin-layout__container {
  max-width: var(--nb-container);
  margin: 0 auto;
}

.admin-layout__overlay {
  display: none;
}

@media (max-width: 899px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-layout__sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 240px;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 250ms ease;
  }

  .admin-layout__sidebar--open {
    transform: translateX(0);
  }

  .admin-layout__overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(45, 52, 54, 0.4);
    z-index: 150;
  }

  .admin-layout__menu-btn {
    display: flex;
  }

  .admin-layout__header {
    padding: 0 16px;
  }

  .admin-layout__main {
    padding: 16px;
  }
}
</style>
