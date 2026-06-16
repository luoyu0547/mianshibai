<!-- 现代双栏风 — 侧边栏 + 主内容区 -->
<template>
  <div class="modern-split">
    <!-- 左侧栏 -->
    <aside class="ms-sidebar">
      <div class="ms-avatar">
        <img v-if="basic?.avatar" :src="String(basic.avatar)" alt="头像" />
        <span v-else>{{ (basic?.name as string)?.[0] || '你' }}</span>
      </div>
      <h1 class="ms-name">{{ basic?.name || '你的名字' }}</h1>
      <p v-if="basic?.targetPosition" class="ms-title">{{ basic.targetPosition }}</p>

      <div class="ms-contact">
        <div v-if="basic?.email" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
          {{ basic.email }}
        </div>
        <div v-if="basic?.phone" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/></svg>
          {{ basic.phone }}
        </div>
        <div v-if="basic?.currentStatus" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
          {{ basic.currentStatus }}
        </div>
        <div v-if="hasContent(basic?.expectedLocation)" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>
          期望地：{{ fmtArray(basic?.expectedLocation) }}
        </div>
        <div v-if="basic?.expectedSalary" class="ms-contact-row">期望薪资：{{ basic.expectedSalary }}</div>
        <div v-if="basic?.city" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>
          {{ basic.city }}
        </div>
        <div v-if="basic?.github" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M12 2A10 10 0 0 0 2 12c0 4.42 2.87 8.17 6.84 9.5.5.08.66-.23.66-.5v-1.69c-2.77.6-3.36-1.34-3.36-1.34-.46-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.6.07-.6 1 .07 1.53 1.03 1.53 1.03.87 1.52 2.34 1.07 2.91.83.09-.65.35-1.09.63-1.34-2.22-.25-4.55-1.11-4.55-4.92 0-1.11.38-2 1.03-2.71-.1-.25-.45-1.29.1-2.64 0 0 .84-.27 2.75 1.02.79-.22 1.65-.33 2.5-.33.85 0 1.71.11 2.5.33 1.91-1.29 2.75-1.02 2.75-1.02.55 1.35.2 2.39.1 2.64.65.71 1.03 1.6 1.03 2.71 0 3.82-2.34 4.66-4.57 4.91.36.31.69.92.69 1.85V21c0 .27.16.59.67.5C19.14 20.16 22 16.42 22 12A10 10 0 0 0 12 2z"/></svg>
          {{ basic.github }}
        </div>
        <div v-if="basic?.blog" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
          {{ basic.blog }}
        </div>
        <div v-if="basic?.website" class="ms-contact-row">
          <svg class="ats-icon" viewBox="0 0 24 24" width="13" height="13"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c.96-1.66 2.49-2.93 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
          {{ basic.website }}
        </div>
      </div>

      <div v-if="skillCategories.length" class="ms-side-block">
        <h2 class="ms-side-title">技能</h2>
        <div v-for="(cat, i) in skillCategories" :key="i" class="ms-skill-group">
          <div v-if="cat.name" class="ms-skill-label">{{ cat.name }}</div>
          <div v-if="cat.items?.length" class="ms-tags">
            <span v-for="(s, si) in cat.items" :key="si" class="ms-tag">{{ s }}</span>
          </div>
        </div>
      </div>

      <div v-if="education.length" class="ms-side-block">
        <h2 class="ms-side-title">教育</h2>
        <div v-for="(item, i) in education" :key="i" class="ms-edu">
          <div class="ms-edu-school"><svg class="ats-icon" viewBox="0 0 24 24" width="14" height="14"><path d="M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82zM12 3L1 9l11 6 9-4.91V17h2V9L12 3z"/></svg>{{ item.school }}</div>
          <div class="ms-edu-major">{{ item.major }}<template v-if="item.degree"> · {{ item.degree }}</template></div>
          <div class="ms-edu-date" v-if="item.startDate">{{ item.startDate }} — {{ item.endDate || '至今' }}</div>
          <p v-if="item.activities" class="ms-desc" v-html="item.activities"></p>
        </div>
      </div>
    </aside>

    <!-- 主内容 -->
    <div class="ms-main">
      <div v-if="work.length" class="ms-block">
        <h2 class="ms-block-title">工作经历</h2>
        <div v-for="(item, i) in work" :key="i" class="ms-entry">
          <div class="ms-entry-head">
            <div>
              <strong class="ms-entry-org">{{ item.company }}</strong>
              <span v-if="item.position" class="ms-entry-role"> · {{ item.position }}</span>
            </div>
            <span class="ms-entry-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
          </div>
          <p v-if="item.description" class="ms-desc" v-html="item.description"></p>
          <div v-if="(item.techStack as string[])?.length" class="ms-stack">
            {{ (item.techStack as string[])?.join(' / ') }}
          </div>
        </div>
      </div>

      <div v-if="project.length" class="ms-block">
        <h2 class="ms-block-title">项目经历</h2>
        <div v-for="(item, i) in project" :key="i" class="ms-entry">
          <div class="ms-entry-head">
            <div>
              <strong class="ms-entry-org">{{ item.name }}</strong>
              <span v-if="item.role" class="ms-entry-role"> · {{ item.role }}</span>
            </div>
            <span class="ms-entry-date">{{ item.startDate }} — {{ item.endDate || '至今' }}</span>
          </div>
          <p v-if="item.description" class="ms-desc" v-html="item.description"></p>
        </div>
      </div>

      <div v-if="summary?.content" class="ms-block">
        <h2 class="ms-block-title">自我评价</h2>
        <p class="ms-desc" v-html="summary?.content"></p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SkillCategory } from '@/types/resume'
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
.modern-split {
  display: flex;
  font-family: var(--font-body);
  color: var(--nb-ink);
  min-height: 100%;
}

/* 左侧栏 */
.ms-sidebar {
  width: 34%;
  background: #f0f5ff;
  padding: 36px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid var(--nb-border-color-light);
}

.ms-avatar {
  width: 82px;
  height: 98px;
  border-radius: 10px;
  background: #dbe7ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 12px;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
  overflow: hidden;
}

.ms-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ms-name {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px;
  text-align: center;
  letter-spacing: -0.3px;
}

.ms-title {
  font-size: 13px;
  color: var(--nb-primary);
  font-weight: 600;
  margin: 0 0 18px;
  text-align: center;
}

.ms-contact {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 100%;
  margin-bottom: 22px;
  padding: 14px;
  background: rgba(255,255,255,0.5);
  border-radius: var(--nb-radius);
}

.ms-contact-row {
  font-size: 11.5px;
  color: var(--nb-muted);
  word-break: break-all;
  line-height: 1.5;
}

.ms-side-block {
  width: 100%;
  margin-bottom: 20px;
}

.ms-side-title {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin: 0 0 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid #3f6df6;
  color: #2f63ef;
}

.ms-skill-group {
  margin-bottom: 10px;
}

.ms-skill-label {
  font-size: 11.5px;
  font-weight: 700;
  margin-bottom: 5px;
  color: var(--nb-ink);
}

.ms-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.ms-tag {
  font-size: 10.5px;
  padding: 3px 9px;
  background: #3f6df6;
  color: #fff;
  border-radius: 12px;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.ms-edu {
  margin-bottom: 10px;
}

.ms-edu-school {
  font-size: 12.5px;
  font-weight: 700;
}

.ms-edu-major {
  font-size: 11.5px;
  color: var(--nb-muted);
}

.ms-edu-date {
  font-size: 10.5px;
  color: var(--nb-muted-light);
}

/* 主内容 */
.ms-main {
  width: 66%;
  padding: 36px 28px;
}

.ms-block {
  margin-bottom: 24px;
}

.ms-block-title {
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--nb-ink);
  margin: 0 0 14px;
  padding-bottom: 6px;
  border-left: 5px solid #3f6df6;
  border-bottom: none;
  background: #edf3ff;
  color: #2f63ef;
  padding: 7px 12px 7px 14px;
}

.ms-entry {
  margin-bottom: 16px;
}

.ms-entry-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  font-size: 13.5px;
}

.ms-entry-org {
  font-weight: 700;
}

.ms-entry-role {
  color: var(--nb-muted);
  font-size: 13px;
}

.ms-entry-date {
  font-size: 12px;
  color: var(--nb-muted-light);
  white-space: nowrap;
  flex-shrink: 0;
}

.ms-desc {
  font-size: 13px;
  color: var(--nb-muted);
  margin: 5px 0 0;
  line-height: 1.7;
}

.ms-stack {
  display: inline-block;
  margin-top: 5px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 11.5px;
  font-weight: 600;
}

.ms-list {
  margin: 5px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--nb-muted);
  line-height: 1.7;
}

.ats-icon {
  display: inline-block;
  vertical-align: -2px;
  margin-right: 4px;
  fill: var(--nb-accent, #6C5CE7);
  flex-shrink: 0;
}
</style>
