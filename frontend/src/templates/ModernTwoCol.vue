<!-- src/templates/ModernTwoCol.vue -->
<template>
  <div class="modern-two-col">
    <div class="modern-two-col__sidebar">
      <div class="modern-two-col__avatar">
        {{ (basic?.name as string)?.[0] || '你' }}
      </div>
      <h1 class="modern-two-col__name">{{ basic?.name || '你的名字' }}</h1>
      <p v-if="basic?.targetPosition" class="modern-two-col__position">{{ basic.targetPosition }}</p>

      <div class="modern-two-col__contact-list">
        <div v-if="basic?.email" class="modern-two-col__contact-item">{{ basic.email }}</div>
        <div v-if="basic?.phone" class="modern-two-col__contact-item">{{ basic.phone }}</div>
        <div v-if="basic?.city" class="modern-two-col__contact-item">{{ basic.city }}</div>
        <div v-if="basic?.github" class="modern-two-col__contact-item">{{ basic.github }}</div>
        <div v-if="basic?.blog" class="modern-two-col__contact-item">{{ basic.blog }}</div>
      </div>

      <div v-if="skillCategories.length" class="modern-two-col__section">
        <h2 class="modern-two-col__section-title">技能</h2>
        <div v-for="(cat, index) in skillCategories" :key="index" class="modern-two-col__skill-cat">
          <div v-if="cat.name" class="modern-two-col__skill-cat-name">{{ cat.name }}</div>
          <div v-if="cat.items?.length" class="modern-two-col__skill-tags">
            <span v-for="(s, si) in cat.items" :key="si" class="modern-two-col__skill-tag">{{ s }}</span>
          </div>
        </div>
      </div>

      <div v-if="education.length" class="modern-two-col__section">
        <h2 class="modern-two-col__section-title">教育</h2>
        <div v-for="(item, index) in education" :key="index" class="modern-two-col__edu-item">
          <div class="modern-two-col__edu-school">{{ item.school }}</div>
          <div class="modern-two-col__edu-detail">{{ item.major }} · {{ item.degree }}</div>
          <div class="modern-two-col__edu-date">{{ item.startDate }} - {{ item.endDate || '至今' }}</div>
        </div>
      </div>
    </div>

    <div class="modern-two-col__main">
      <div v-if="work.length" class="modern-two-col__section">
        <h2 class="modern-two-col__section-title-main">工作经历</h2>
        <div v-for="(item, index) in work" :key="index" class="modern-two-col__entry">
          <div class="modern-two-col__entry-header">
            <strong>{{ item.company }}</strong>
            <span class="modern-two-col__date">{{ item.startDate }} - {{ item.endDate || '至今' }}</span>
          </div>
          <div v-if="item.position" class="modern-two-col__sub">{{ item.position }}</div>
          <p v-if="item.description" class="modern-two-col__desc">{{ item.description }}</p>
          <ul v-if="(item.highlights as string[])?.length" class="modern-two-col__highlights">
            <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
          </ul>
        </div>
      </div>

      <div v-if="project.length" class="modern-two-col__section">
        <h2 class="modern-two-col__section-title-main">项目经历</h2>
        <div v-for="(item, index) in project" :key="index" class="modern-two-col__entry">
          <div class="modern-two-col__entry-header">
            <strong>{{ item.name }}</strong>
            <span class="modern-two-col__date">{{ item.startDate }} - {{ item.endDate || '至今' }}</span>
          </div>
          <div v-if="item.role" class="modern-two-col__sub">{{ item.role }}</div>
          <p v-if="item.description" class="modern-two-col__desc">{{ item.description }}</p>
          <ul v-if="(item.highlights as string[])?.length" class="modern-two-col__highlights">
            <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
          </ul>
        </div>
      </div>

      <div v-if="summary?.content" class="modern-two-col__section">
        <h2 class="modern-two-col__section-title-main">自我评价</h2>
        <p class="modern-two-col__desc">{{ summary.content }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SkillCategory } from '@/types/resume'

const props = defineProps<{
  basic: Record<string, unknown> | null
  education: Record<string, unknown>[]
  work: Record<string, unknown>[]
  project: Record<string, unknown>[]
  skills: Record<string, unknown> | null
  summary: Record<string, unknown> | null
}>()

const skillCategories = computed(() => (props.skills?.categories as SkillCategory[] | undefined) ?? [])
</script>

<style scoped>
.modern-two-col {
  display: flex;
  font-family: 'Public Sans', sans-serif;
  color: #2d3436;
  min-height: 100%;
}

.modern-two-col__sidebar {
  width: 35%;
  background: #f0edff;
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.modern-two-col__avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--nb-primary, #6c5ce7);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Lexend Mega', sans-serif;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
}

.modern-two-col__name {
  font-family: 'Lexend Mega', sans-serif;
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px;
  text-align: center;
}

.modern-two-col__position {
  font-size: 13px;
  color: #6c5ce7;
  font-weight: 600;
  margin: 0 0 16px;
}

.modern-two-col__contact-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  margin-bottom: 20px;
}

.modern-two-col__contact-item {
  font-size: 11px;
  color: #636e72;
  word-break: break-all;
}

.modern-two-col__section {
  width: 100%;
  margin-bottom: 16px;
}

.modern-two-col__section-title {
  font-family: 'Lexend Mega', sans-serif;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0 0 10px;
  padding-bottom: 4px;
  border-bottom: 2px solid #6c5ce7;
}

.modern-two-col__skill-cat {
  margin-bottom: 8px;
}

.modern-two-col__skill-cat-name {
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 4px;
}

.modern-two-col__skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.modern-two-col__skill-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: #6c5ce7;
  color: #fff;
  border-radius: 3px;
}

.modern-two-col__edu-item {
  margin-bottom: 8px;
}

.modern-two-col__edu-school {
  font-size: 12px;
  font-weight: 600;
}

.modern-two-col__edu-detail {
  font-size: 11px;
  color: #636e72;
}

.modern-two-col__edu-date {
  font-size: 10px;
  color: #636e72;
}

.modern-two-col__main {
  width: 65%;
  padding: 32px 24px;
}

.modern-two-col__section-title-main {
  font-family: 'Lexend Mega', sans-serif;
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 2px solid #2d3436;
  padding-bottom: 4px;
  margin: 0 0 12px;
}

.modern-two-col__entry {
  margin-bottom: 14px;
}

.modern-two-col__entry-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
}

.modern-two-col__date {
  font-size: 11px;
  color: #636e72;
  white-space: nowrap;
}

.modern-two-col__sub {
  font-size: 12px;
  color: #636e72;
}

.modern-two-col__desc {
  font-size: 12px;
  margin: 4px 0;
  line-height: 1.6;
}

.modern-two-col__highlights {
  margin: 4px 0;
  padding-left: 16px;
  font-size: 12px;
  line-height: 1.6;
}
</style>
