<!-- ATS 精英单栏：程序员默认投递模板 -->
<template>
  <article class="ats-resume">
    <header class="ats-resume__header">
      <div class="ats-resume__identity">
        <h1>{{ basic?.name || '你的名字' }}</h1>
        <p v-if="basic?.targetPosition" class="ats-resume__position">{{ basic.targetPosition }}</p>
        <div class="ats-resume__contact">
          <span v-if="basic?.phone">{{ basic.phone }}</span>
          <span v-if="basic?.email">{{ basic.email }}</span>
          <span v-if="basic?.currentStatus">{{ basic.currentStatus }}</span>
          <span v-if="basic?.expectedLocation || basic?.city">{{ basic.expectedLocation || basic.city }}</span>
          <span v-if="basic?.github">GitHub: {{ basic.github }}</span>
          <span v-if="basic?.blog || basic?.website">{{ basic.blog || basic.website }}</span>
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
        <p v-if="item.description" class="ats-entry__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="ats-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
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
        <p v-if="item.description" class="ats-entry__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="ats-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
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
        <ul v-if="(item.highlights as string[])?.length" class="ats-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </section>

    <section v-if="summary?.content" class="ats-section">
      <h2>个人简介</h2>
      <p class="ats-summary">{{ summary.content }}</p>
    </section>
  </article>
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
