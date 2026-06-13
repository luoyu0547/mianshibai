<template>
  <div class="admin-layout">
    <aside class="admin-layout__sidebar">
      <router-link to="/admin" class="admin-layout__brand">面试吧 Admin</router-link>
      <nav class="admin-layout__nav">
        <router-link to="/admin" class="admin-layout__link">平台总览</router-link>
        <router-link to="/admin/users" class="admin-layout__link">用户管理</router-link>
        <router-link to="/" class="admin-layout__link admin-layout__link--ghost">返回用户端</router-link>
      </nav>
    </aside>
    <section class="admin-layout__content">
      <header class="admin-layout__header">
        <span>管理员后台</span>
        <strong>{{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || 'admin' }}</strong>
      </header>
      <main class="admin-layout__main">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: var(--nb-bg);
}

.admin-layout__sidebar {
  padding: 24px;
  background: var(--nb-card);
  border-right: var(--nb-border);
  box-shadow: 6px 0 0 rgba(0, 0, 0, 0.08);
}

.admin-layout__brand {
  display: block;
  margin-bottom: 32px;
  color: var(--nb-text);
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  text-decoration: none;
  text-shadow: 2px 2px 0 var(--nb-primary);
}

.admin-layout__nav {
  display: grid;
  gap: 12px;
}

.admin-layout__link {
  padding: 12px 14px;
  color: var(--nb-text);
  text-decoration: none;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
  box-shadow: var(--nb-shadow-sm);
  font-weight: 700;
}

.admin-layout__link.router-link-active {
  background: var(--nb-primary);
  color: #fff;
}

.admin-layout__link--ghost {
  margin-top: 16px;
  background: transparent;
}

.admin-layout__content {
  min-width: 0;
}

.admin-layout__header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  font-weight: 700;
}

.admin-layout__main {
  padding: 32px;
}

@media (max-width: 768px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-layout__sidebar {
    border-right: 0;
    border-bottom: var(--nb-border);
    box-shadow: 0 4px 0 rgba(0, 0, 0, 0.08);
  }

  .admin-layout__nav {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}
</style>
