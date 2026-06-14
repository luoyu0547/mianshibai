<!-- 典雅正式风 — 经典排版、表格式教育、正式风格 -->
<template>
  <div class="executive-classic">
    <!-- 头部 -->
    <div class="ec-header">
      <h1 class="ec-name">{{ basic?.name || '你的名字' }}</h1>
      <div class="ec-contact">
        <span v-if="basic?.email">{{ basic.email }}</span>
        <span v-if="basic?.phone">{{ basic.phone }}</span>
        <span v-if="basic?.city">{{ basic.city }}</span>
        <span v-if="basic?.github">{{ basic.github }}</span>
        <span v-if="basic?.blog">{{ basic.blog }}</span>
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
            <td>{{ item.school }}</td>
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
        <p v-if="item.description" class="ec-desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="ec-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
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
        <p v-if="item.description" class="ec-desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="ec-list">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
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
      <p class="ec-desc ec-summary">{{ summary.content }}</p>
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
  border-bottom: 1.5px solid var(--nb-ink);
  padding-bottom: 4px;
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
</style>
