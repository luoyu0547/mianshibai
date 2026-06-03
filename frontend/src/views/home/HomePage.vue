<!-- src/views/home/HomePage.vue -->
<template>
  <MainLayout>
    <div class="home-page">
      <!-- 欢迎卡片 -->
      <NbCard class="welcome-card">
        <div class="welcome-card__content">
          <div class="welcome-card__greeting">
            <span class="welcome-card__emoji">👋</span>
            <h2 class="welcome-card__title">
              欢迎回来，{{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || '求职者' }}
            </h2>
          </div>
          <p class="welcome-card__subtitle">
            今天准备挑战哪个方向的面试？让 AI 陪你练起来！
          </p>
        </div>
      </NbCard>

      <!-- 统计区域 -->
      <div class="home-page__stats">
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">已完成面试</div>
        </NbCard>
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">面试题目</div>
        </NbCard>
        <NbCard hoverable class="stat-card">
          <div class="stat-card__value">0</div>
          <div class="stat-card__label">练习天数</div>
        </NbCard>
      </div>

      <!-- 快捷操作 -->
      <div class="home-page__actions">
        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-primary);">
            <IconTarget />
          </div>
          <h3 class="action-card__title">开始面试</h3>
          <p class="action-card__desc">选择岗位方向，开始 AI 模拟面试</p>
          <NbButton type="primary" block>立即开始</NbButton>
        </NbCard>

        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-secondary);">
            <IconChart />
          </div>
          <h3 class="action-card__title">面试记录</h3>
          <p class="action-card__desc">查看历史面试记录和表现分析</p>
          <NbButton type="secondary" block>查看记录</NbButton>
        </NbCard>

        <NbCard hoverable class="action-card">
          <div class="action-card__icon" style="background: var(--nb-accent);">
            <IconResume />
          </div>
          <h3 class="action-card__title">我的简历</h3>
          <p class="action-card__desc">AI 驱动简历编辑，智能优化内容</p>
          <router-link to="/resume">
            <NbButton type="accent" block>管理简历</NbButton>
          </router-link>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChart from '@/components/icons/IconChart.vue'
import IconResume from '@/components/icons/IconResume.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.welcome-card__content {
  text-align: center;
}

.welcome-card__greeting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.welcome-card__emoji {
  font-size: 32px;
}

.welcome-card__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.welcome-card__subtitle {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
}

.home-page__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.stat-card {
  text-align: center;
  padding: 32px 24px;
}

.stat-card__value {
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  color: var(--nb-primary);
  margin-bottom: 8px;
}

.stat-card__label {
  font-size: 14px;
  color: var(--nb-muted);
}

.home-page__actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.action-card {
  text-align: center;
}

.action-card__icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
}

.action-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px;
}

.action-card__desc {
  font-size: 14px;
  color: var(--nb-muted);
  margin-bottom: 20px;
}

.home-page__actions a {
  text-decoration: none;
}

@media (max-width: 768px) {
  .home-page__stats {
    grid-template-columns: 1fr;
  }

  .home-page__actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .home-page__actions {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
