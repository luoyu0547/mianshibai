<!-- ATS 精英单栏：程序员默认投递模板 -->
<template>
  <article class="ats-resume">
    <header class="ats-resume__header">
      <div class="ats-resume__identity">
        <h1>{{ basic?.name || '你的名字' }}</h1>
        <p v-if="basic?.targetPosition" class="ats-resume__position">{{ basic.targetPosition }}</p>
        <div class="ats-resume__contact">
          <span v-if="basic?.phone">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>
            {{ basic.phone }}
          </span>
          <span v-if="basic?.email">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
            {{ basic.email }}
          </span>
          <span v-if="basic?.currentStatus">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
            {{ basic.currentStatus }}
          </span>
          <span v-if="basic?.city">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
            {{ basic.city }}
          </span>
          <span v-if="hasContent(basic?.expectedLocation)">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>
            期望地：{{ fmtArray(basic?.expectedLocation) }}
          </span>
          <span v-if="basic?.github">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z"/></svg>
            {{ basic.github }}
          </span>
          <span v-if="basic?.blog || basic?.website">
            <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
            {{ basic.blog || basic.website }}
          </span>
        </div>
      </div>
      <img v-if="basic?.avatar" class="ats-resume__avatar" :src="String(basic.avatar)" alt="头像" />
    </header>

    <section v-if="skillCategories.length" class="ats-section ats-section--skills">
      <h2>专业技能</h2>
      <div class="ats-skill-list">
        <div v-for="(cat, index) in skillCategories" :key="index" class="ats-skill-row">
          <strong>{{ cat.name }}：</strong>
          <span>{{ (cat.items as string[])?.join('、') }}</span>
        </div>
      </div>
    </section>

    <section v-if="work.length" class="ats-section">
      <h2>工作经历</h2>
      <div v-for="(item, index) in work" :key="index" class="ats-entry">
        <div class="ats-entry__head">
          <div>
            <strong>{{ item.company }}</strong>
            <span v-if="item.position"> · {{ item.position }}</span>
          </div>
          <time v-if="item.startDate || item.endDate">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</time>
        </div>
        <p v-if="item.description" class="ats-entry__desc" v-html="renderMarkdown(String(item.description || ''))"></p>
      </div>
    </section>

    <section v-if="project.length" class="ats-section">
      <h2>项目经历</h2>
      <div v-for="(item, index) in project" :key="index" class="ats-entry">
        <div class="ats-entry__head">
          <div>
            <strong>{{ item.name }}</strong>
            <span v-if="item.role"> · {{ item.role }}</span>
          </div>
          <time v-if="item.startDate || item.endDate">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</time>
        </div>
        <div v-if="(item.techStack as string[])?.length" class="ats-tech">
          技术栈：{{ (item.techStack as string[])?.join('、') }}
        </div>
        <p v-if="item.description" class="ats-entry__desc" v-html="renderMarkdown(String(item.description || ''))"></p>
      </div>
    </section>

    <section v-if="education.length" class="ats-section">
      <h2>教育经历</h2>
      <div v-for="(item, index) in education" :key="index" class="ats-entry ats-entry--compact">
        <div class="ats-entry__head">
          <div>
            <svg class="ats-icon" viewBox="0 0 24 24" width="14" height="14"><path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/></svg>
            <strong>{{ item.school }}</strong>
            <span v-if="item.major"> · {{ item.major }}</span>
            <span v-if="item.degree"> · {{ item.degree }}</span>
            <span v-if="item.gpa"> · GPA: {{ item.gpa }}</span>
          </div>
          <time v-if="item.startDate || item.endDate">{{ item.startDate }} ~ {{ item.endDate || '至今' }}</time>
        </div>
        <p v-if="item.activities" class="ats-entry__desc" v-html="renderMarkdown(String(item.activities || ''))"></p>
      </div>
    </section>

    <section v-if="summary?.content" class="ats-section">
      <h2>个人简介</h2>
      <p class="ats-summary" v-html="renderMarkdown(String(summary?.content || ''))"></p>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SkillCategory } from '@/types/resume'
import { renderMarkdown } from '@/utils/markdown'

function hasContent(value: unknown): boolean {
  if (Array.isArray(value)) return value.length > 0
  return !!value
}

function fmtArray(value: unknown): string {
  if (Array.isArray(value)) return value.join('、')
  return String(value || '')
}

const props = defineProps<{
  basic: Record<string, unknown> | null
  education: Record<string, unknown>[]
  work: Record<string, unknown>[]
  project: Record<string, unknown>[]
  skills: Record<string, unknown> | null
  summary: Record<string, unknown> | null
  accentColor?: string
}>()

const skillCategories = computed(() => (props.skills?.categories as SkillCategory[] | undefined) ?? [])
</script>

<style scoped>
.ats-resume {
  font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
  color: #111827;
  line-height: 1.72;
}

.ats-resume__header {
  position: relative;
  min-height: 98px;
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  text-align: center;
}

.ats-resume__identity h1 {
  margin: 0 0 5px;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.ats-resume__position {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.ats-resume__contact {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 14px;
  max-width: 520px;
  color: #4b5563;
  font-size: 12px;
}

.ats-resume__contact span:not(:last-child)::after {
  content: "";
}

.ats-resume__avatar {
  position: absolute;
  top: 0;
  right: 0;
  width: 78px;
  height: 96px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
}

.ats-section {
  margin-bottom: 19px;
}

.ats-section h2 {
  margin: 0 0 11px;
  padding: 7px 12px 7px 16px;
  border-left: 5px solid #3f6df6;
  background: #edf3ff;
  color: #2f63ef;
  font-size: 18px;
  line-height: 1.25;
  font-weight: 800;
}

.ats-skill-row {
  font-size: 13px;
  margin-bottom: 6px;
}

.ats-skill-row strong {
  font-weight: 800;
}

.ats-entry {
  margin-bottom: 13px;
}

.ats-entry--compact {
  margin-bottom: 8px;
}

.ats-entry__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: baseline;
  font-size: 14px;
}

.ats-entry__head strong {
  font-weight: 800;
}

.ats-entry__head span {
  color: #6b7280;
  font-weight: 500;
}

.ats-entry__head time {
  flex-shrink: 0;
  color: #6b7280;
  font-size: 12px;
}

.ats-tech,
.ats-entry__desc,
.ats-summary {
  margin: 4px 0 0;
  color: #1f2937;
  font-size: 13px;
}

.ats-tech {
  color: #4b5563;
}

.ats-list {
  margin: 4px 0 0;
  padding-left: 20px;
  color: #111827;
  font-size: 13px;
}

.ats-list li {
  margin-bottom: 2px;
}

.ats-icon {
  display: inline-block;
  vertical-align: -2px;
  margin-right: 4px;
  fill: var(--nb-accent, #6C5CE7);
  flex-shrink: 0;
}
</style>
