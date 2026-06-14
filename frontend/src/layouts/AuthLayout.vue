<!-- src/layouts/AuthLayout.vue -->
<template>
  <div class="auth-layout">
    <div class="auth-layout__left">
      <div class="auth-layout__decorator auth-layout__decorator--1"></div>
      <div class="auth-layout__decorator auth-layout__decorator--2"></div>
      <div class="auth-layout__decorator auth-layout__decorator--3"></div>

      <div class="auth-layout__brand">
        <h1 class="auth-layout__title">面试吧</h1>
        <p class="auth-layout__slogan">AI 模拟面试，拿下你的 dream offer</p>
      </div>

      <div class="auth-layout__features">
        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconTarget /></div>
          <div class="feature-card__title">智能题库</div>
          <div class="feature-card__desc">AI 实时生成面试问题</div>
        </NbCard>

        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconChat /></div>
          <div class="feature-card__title">模拟对话</div>
          <div class="feature-card__desc">沉浸式面试体验</div>
        </NbCard>

        <NbCard hoverable class="feature-card">
          <div class="feature-card__icon"><IconChart /></div>
          <div class="feature-card__title">即时反馈</div>
          <div class="feature-card__desc">面试表现全分析</div>
        </NbCard>
      </div>
    </div>

    <div class="auth-layout__right">
      <div class="auth-layout__form-wrapper">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import NbCard from '@/components/NbCard.vue'
import IconTarget from '@/components/icons/IconTarget.vue'
import IconChat from '@/components/icons/IconChat.vue'
import IconChart from '@/components/icons/IconChart.vue'
</script>

<style scoped>
.auth-layout {
  display: flex;
  min-height: 100vh;
  background: var(--nb-bg);
}

.auth-layout__left {
  flex: 0 0 58%;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 48px;
  overflow: hidden;
  background: linear-gradient(180deg, var(--nb-primary-light) 0%, rgba(255,255,255,0) 100%);
}

.auth-layout__decorator {
  position: absolute;
  border-radius: var(--nb-radius-lg);
  pointer-events: none;
}

.auth-layout__decorator--1 {
  top: -40px;
  left: -40px;
  width: 200px;
  height: 200px;
  background: var(--nb-primary-gradient);
  transform: rotate(-12deg);
  opacity: 0.08;
  border-radius: 32px;
}

.auth-layout__decorator--2 {
  bottom: 48px;
  right: 32px;
  width: 90px;
  height: 90px;
  background: var(--nb-secondary);
  border-radius: 50%;
  opacity: 0.12;
}

.auth-layout__decorator--3 {
  top: 48px;
  right: 64px;
  width: 56px;
  height: 56px;
  border: 2px solid var(--nb-primary-light);
  border-radius: var(--nb-radius);
  background: transparent;
  transform: rotate(20deg);
  opacity: 0.5;
}

.auth-layout__brand {
  text-align: center;
  margin-bottom: 48px;
  z-index: 1;
}

.auth-layout__title {
  font-family: var(--font-heading);
  font-size: 52px;
  font-weight: 700;
  color: var(--nb-ink);
  margin: 0 0 16px;
  letter-spacing: -1px;
}

.auth-layout__slogan {
  font-family: var(--font-body);
  font-size: 18px;
  color: var(--nb-muted);
  margin: 0;
}

.auth-layout__features {
  display: flex;
  gap: 20px;
  z-index: 1;
}

.feature-card {
  width: 190px;
  text-align: center;
}

.feature-card__icon {
  color: var(--nb-primary);
  margin-bottom: 12px;
}

.feature-card__title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.feature-card__desc {
  font-size: 14px;
  color: var(--nb-muted);
}

.auth-layout__right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--nb-bg);
}

.auth-layout__form-wrapper {
  width: 100%;
  max-width: 420px;
}

@media (max-width: 1024px) {
  .auth-layout {
    flex-direction: column;
  }

  .auth-layout__left {
    flex: none;
    padding: 32px 24px;
    min-height: auto;
  }

  .auth-layout__title {
    font-size: 42px;
  }

  .auth-layout__features {
    flex-direction: column;
    align-items: center;
  }

  .feature-card {
    width: 100%;
    max-width: 300px;
  }

  .auth-layout__right {
    padding: 32px 24px;
  }
}
</style>
