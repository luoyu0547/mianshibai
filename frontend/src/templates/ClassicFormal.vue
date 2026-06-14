<!-- 典雅正式风 — 经典排版、表格式教育、正式风格 -->
<template>
  <div class="executive-classic">
    <!-- 头部 -->
    <div class="ec-header">
      <h1 class="ec-name">{{ basic?.name || '你的名字' }}</h1>
      <div class="ec-contact">
        <span v-if="basic?.email">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
          {{ basic.email }}
        </span>
        <span v-if="basic?.phone">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>
          {{ basic.phone }}
        </span>
        <span v-if="basic?.currentStatus">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
          {{ basic.currentStatus }}
        </span>
        <span v-if="basic?.targetPosition">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
          {{ basic.targetPosition }}
        </span>
        <span v-if="basic?.city">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
          {{ basic.city }}
        </span>
        <span v-if="basic?.expectedLocation">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
          {{ basic.expectedLocation }}
        </span>
        <span v-if="basic?.github">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z"/></svg>
          {{ basic.github }}
        </span>
        <span v-if="basic?.blog">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
          {{ basic.blog }}
        </span>
        <span v-if="basic?.website">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
          {{ basic.website }}
        </span>
      </div>
      <div class="ec-divider"></div>
    </div>

    <!-- 教育经历 — 表格式 -->
    <div v-if="education.length" class="ec-block">
      <h2 class="ec-block-title">教育经历</h2>
      <table class="ec-table">
        <thead>
          <tr>
            <th>学校</th>
            <th>专业</th>
            <th>学历</th>
            <th>时间</th>
            <th v-if="hasAnyGpa">GPA</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in education" :key="i">
            <td><svg class="ats-icon" viewBox="0 0 24 24" width="14" height="14"><path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/></svg>{{ item.school }}</td>
            <td>{{ item.major }}</td>
            <td>{{ item.degree }}</td>
            <td>{{ item.startDate }} — {{ item.endDate || '至今' }}</td>
            <td v-if="hasAnyGpa">{{ item.gpa || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 工作经历 -->
    <div v-if="work.length" class="ec-block">
      <h2 class="ec-block-title">工作经历</h2>
      <div v-for="(item, i) in work" :key="i" class="ec-entry">
        <div class="ec-entry-head">
          <div>
            <strong>{{ item.company }}</strong>
            <em v-if="item.position"> · {{ item.position }}</em>
          </div>
          <span class="ec-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="ec-desc" v-html="renderMarkdown(String(item.description || ''))"></p>
        <p v-if="(item.techStack as string[])?.length" class="ec-stack">技术栈：{{ (item.techStack as string[])?.join('、') }}</p>
      </div>
    </div>

    <!-- 项目经历 -->
    <div v-if="project.length" class="ec-block">
      <h2 class="ec-block-title">项目经历</h2>
      <div v-for="(item, i) in project" :key="i" class="ec-entry">
        <div class="ec-entry-head">
          <div>
            <strong>{{ item.name }}</strong>
            <em v-if="item.role"> · {{ item.role }}</em>
          </div>
          <span class="ec-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="ec-desc" v-html="renderMarkdown(String(item.description || ''))"></p>
      </div>
    </div>

    <!-- 技能 -->
    <div v-if="skillCategories.length" class="ec-block">
      <h2 class="ec-block-title">专业技能</h2>
      <div v-for="(cat, i) in skillCategories" :key="i" class="ec-skill-line">
        <strong>{{ cat.name }}：</strong>
        <span>{{ (cat.items as string[])?.join('、') }}</span>
      </div>
    </div>

    <!-- 自我评价 -->
    <div v-if="summary?.content" class="ec-block">
      <h2 class="ec-block-title">自我评价</h2>
      <p class="ec-desc ec-summary" v-html="renderMarkdown(String(summary?.content || ''))"></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SkillCategory } from '@/types/resume'
import { renderMarkdown } from '@/utils/markdown'

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

const hasAnyGpa = computed(() => props.education.some((e) => e.gpa))
</script>

<style scoped>
.executive-classic {
  font-family: 'Georgia', 'Noto Serif SC', 'SimSun', serif;
  color: var(--nb-ink);
  line-height: 1.8;
}

/* 头部 */
.ec-header {
  text-align: center;
  margin-bottom: 22px;
}

.ec-name {
  font-size: 30px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 2px;
  font-family: var(--font-heading);
}

.ec-contact {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 20px;
  font-size: 12px;
  font-family: var(--font-body);
  color: var(--nb-muted);
  margin-bottom: 14px;
}

.ec-divider {
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--nb-primary), transparent);
  margin: 0 30px;
}

/* 内容块 */
.ec-block {
  margin-bottom: 18px;
}

.ec-block-title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  border-left: 4px solid #3f6df6;
  border-bottom: 1px solid #dbe3ef;
  padding: 4px 0 5px 10px;
  color: #1f2937;
  margin: 0 0 12px;
}

/* 表格 */
.ec-table {
  width: 100%;
  border-collapse: collapse;
  font-family: var(--font-body);
  font-size: 12.5px;
}

.ec-table th {
  text-align: left;
  padding: 6px 10px;
  border-bottom: 2px solid var(--nb-border-color);
  font-weight: 700;
  font-size: 11.5px;
  color: var(--nb-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ec-table td {
  padding: 7px 10px;
  border-bottom: 1px solid var(--nb-border-color-light);
  font-size: 12.5px;
}

/* 条目 */
.ec-entry {
  margin-bottom: 15px;
}

.ec-entry-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  font-size: 13.5px;
}

.ec-entry-head strong {
  font-family: var(--font-heading);
  font-weight: 700;
}

.ec-entry-head em {
  font-style: normal;
  font-family: var(--font-body);
  color: var(--nb-muted);
  font-size: 12.5px;
}

.ec-date {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--nb-muted-light);
  white-space: nowrap;
  flex-shrink: 0;
}

.ec-desc {
  font-family: var(--font-body);
  font-size: 12.5px;
  margin: 5px 0 0;
  line-height: 1.8;
  color: var(--nb-muted);
}

.ec-stack {
  margin: 4px 0 0;
  font-family: var(--font-body);
  font-size: 12px;
  color: #475569;
}

.ec-summary {
  text-indent: 2em;
}

.ec-list {
  margin: 4px 0 0;
  padding-left: 22px;
  font-family: var(--font-body);
  font-size: 12.5px;
  color: var(--nb-muted);
  line-height: 1.8;
}

.ec-skill-line {
  font-family: var(--font-body);
  font-size: 12.5px;
  line-height: 1.9;
  margin-bottom: 3px;
}

.ec-skill-line strong {
  font-family: var(--font-heading);
}

.ats-icon {
  display: inline-block;
  vertical-align: -2px;
  margin-right: 4px;
  fill: var(--nb-accent, #6C5CE7);
  flex-shrink: 0;
}
</style>
