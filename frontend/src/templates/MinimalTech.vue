<!-- 极简专业风 — 单栏、清晰排版、ATS友好 -->
<template>
  <div class="minimal-clean">
    <!-- 头部 -->
    <div class="mc-header">
      <h1 class="mc-name">{{ basic?.name || '你的名字' }}</h1>
      <div v-if="basic?.targetPosition" class="mc-title">{{ basic.targetPosition }}</div>
      <div class="mc-contact">
        <span v-if="basic?.email" class="mc-contact-item">
          <span class="mc-contact-icon">✉</span>{{ basic.email }}
        </span>
        <span v-if="basic?.phone" class="mc-contact-item">
          <span class="mc-contact-icon">☎</span>{{ basic.phone }}
        </span>
        <span v-if="basic?.city" class="mc-contact-item">
          <span class="mc-contact-icon">⌂</span>{{ basic.city }}
        </span>
        <span v-if="basic?.github" class="mc-contact-item">
          <span class="mc-contact-icon">⌘</span>{{ basic.github }}
        </span>
        <span v-if="basic?.blog" class="mc-contact-item">
          <span class="mc-contact-icon">◈</span>{{ basic.blog }}
        </span>
      </div>
    </div>

    <!-- 技能 -->
    <div v-if="skillCategories.length" class="mc-block">
      <h2 class="mc-block-title">专业技能</h2>
      <div class="mc-skills">
        <div v-for="(cat, i) in skillCategories" :key="i" class="mc-skill-group">
          <span class="mc-skill-cat">{{ cat.name }}</span>
          <span class="mc-skill-items">{{ (cat.items as string[])?.join('  ·  ') }}</span>
        </div>
      </div>
    </div>

    <!-- 工作经历 -->
    <div v-if="work.length" class="mc-block">
      <h2 class="mc-block-title">工作经历</h2>
      <div v-for="(item, i) in work" :key="i" class="mc-entry">
        <div class="mc-entry-head">
          <div class="mc-entry-main">
            <strong class="mc-entry-org">{{ item.company }}</strong>
            <span v-if="item.position" class="mc-entry-role">{{ item.position }}</span>
          </div>
          <span v-if="item.startDate" class="mc-entry-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="mc-desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="mc-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <!-- 项目经历 -->
    <div v-if="project.length" class="mc-block">
      <h2 class="mc-block-title">项目经历</h2>
      <div v-for="(item, i) in project" :key="i" class="mc-entry">
        <div class="mc-entry-head">
          <div class="mc-entry-main">
            <strong class="mc-entry-org">{{ item.name }}</strong>
            <span v-if="item.role" class="mc-entry-role">{{ item.role }}</span>
          </div>
          <span v-if="item.startDate" class="mc-entry-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="mc-desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="mc-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <!-- 教育经历 -->
    <div v-if="education.length" class="mc-block">
      <h2 class="mc-block-title">教育经历</h2>
      <div v-for="(item, i) in education" :key="i" class="mc-entry">
        <div class="mc-entry-head">
          <div class="mc-entry-main">
            <strong class="mc-entry-org">{{ item.school }}</strong>
            <span class="mc-entry-role">{{ item.major }}<template v-if="item.degree"> · {{ item.degree }}</template></span>
          </div>
          <span v-if="item.startDate" class="mc-entry-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
        </div>
        <div v-if="item.gpa" class="mc-meta">GPA: {{ item.gpa }}</div>
      </div>
    </div>

    <!-- 自我评价 -->
    <div v-if="summary?.content" class="mc-block">
      <h2 class="mc-block-title">自我评价</h2>
      <p class="mc-desc mc-summary">{{ summary.content }}</p>
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
.minimal-clean {
  font-family: var(--font-body);
  color: var(--nb-ink);
  line-height: 1.7;
}

/* 头部 */
.mc-header {
  text-align: center;
  padding-bottom: 24px;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--nb-primary);
}

.mc-name {
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
  color: var(--nb-ink);
}

.mc-title {
  font-size: 15px;
  color: var(--nb-primary);
  font-weight: 600;
  margin-bottom: 14px;
}

.mc-contact {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 18px;
  font-size: 12.5px;
  color: var(--nb-muted);
}

.mc-contact-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.mc-contact-icon {
  font-size: 11px;
  opacity: 0.5;
}

/* 内容块 */
.mc-block {
  margin-bottom: 22px;
}

.mc-block-title {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--nb-primary);
  margin: 0 0 12px;
  padding-bottom: 6px;
  border-bottom: 1.5px solid var(--nb-primary-light);
}

/* 技能 */
.mc-skills {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mc-skill-group {
  font-size: 13px;
  line-height: 1.6;
}

.mc-skill-cat {
  font-weight: 700;
  color: var(--nb-ink);
}

.mc-skill-items {
  color: var(--nb-muted);
}

/* 条目 */
.mc-entry {
  margin-bottom: 16px;
}

.mc-entry-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.mc-entry-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.mc-entry-org {
  font-size: 14px;
  font-weight: 700;
  color: var(--nb-ink);
}

.mc-entry-role {
  font-size: 13px;
  color: var(--nb-muted);
}

.mc-entry-date {
  font-size: 12px;
  color: var(--nb-muted-light);
  white-space: nowrap;
  flex-shrink: 0;
}

.mc-desc {
  font-size: 13px;
  color: var(--nb-muted);
  margin: 6px 0 0;
  line-height: 1.7;
}

.mc-summary {
  font-size: 13.5px;
}

.mc-list {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--nb-muted);
  line-height: 1.7;
}

.mc-meta {
  font-size: 12px;
  color: var(--nb-muted-light);
  margin-top: 2px;
}
</style>
