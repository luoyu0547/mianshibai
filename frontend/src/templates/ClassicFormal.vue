<!-- src/templates/ClassicFormal.vue -->
<template>
  <div class="classic-formal">
    <div class="classic-formal__header">
      <h1 class="classic-formal__name">{{ basic?.name || '你的名字' }}</h1>
      <div class="classic-formal__contact">
        <span v-if="basic?.email">{{ basic.email }}</span>
        <span v-if="basic?.phone">{{ basic.phone }}</span>
        <span v-if="basic?.city">{{ basic.city }}</span>
        <span v-if="basic?.github">{{ basic.github }}</span>
        <span v-if="basic?.blog">{{ basic.blog }}</span>
      </div>
      <div class="classic-formal__divider"></div>
    </div>

    <div v-if="education.length" class="classic-formal__section">
      <h2 class="classic-formal__section-title">教育经历</h2>
      <table class="classic-formal__table">
        <thead>
          <tr>
            <th>学校</th>
            <th>专业</th>
            <th>学历</th>
            <th>时间</th>
            <th>GPA</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in education" :key="index">
            <td>{{ item.school }}</td>
            <td>{{ item.major }}</td>
            <td>{{ item.degree }}</td>
            <td>{{ item.startDate }} - {{ item.endDate || '至今' }}</td>
            <td>{{ item.gpa }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="work.length" class="classic-formal__section">
      <h2 class="classic-formal__section-title">工作经历</h2>
      <div v-for="(item, index) in work" :key="index" class="classic-formal__entry">
        <div class="classic-formal__entry-header">
          <strong>{{ item.company }}</strong>
          <em v-if="item.position">{{ item.position }}</em>
          <span class="classic-formal__date">{{ item.startDate }} - {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="classic-formal__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="classic-formal__highlights">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <div v-if="project.length" class="classic-formal__section">
      <h2 class="classic-formal__section-title">项目经历</h2>
      <div v-for="(item, index) in project" :key="index" class="classic-formal__entry">
        <div class="classic-formal__entry-header">
          <strong>{{ item.name }}</strong>
          <em v-if="item.role">{{ item.role }}</em>
          <span class="classic-formal__date">{{ item.startDate }} - {{ item.endDate || '至今' }}</span>
        </div>
        <p v-if="item.description" class="classic-formal__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="classic-formal__highlights">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <div v-if="skillCategories.length" class="classic-formal__section">
      <h2 class="classic-formal__section-title">专业技能</h2>
      <div v-for="(cat, index) in skillCategories" :key="index" class="classic-formal__skill-line">
        <strong>{{ cat.name }}：</strong>
        <span>{{ cat.items?.join('、') }}</span>
      </div>
    </div>

    <div v-if="summary?.content" class="classic-formal__section">
      <h2 class="classic-formal__section-title">自我评价</h2>
      <p class="classic-formal__desc">{{ summary.content }}</p>
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
.classic-formal {
  font-family: Georgia, 'Times New Roman', serif;
  color: #2d3436;
}

.classic-formal__header {
  text-align: center;
  margin-bottom: 20px;
}

.classic-formal__name {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 2px;
}

.classic-formal__contact {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
  font-size: 12px;
  font-family: 'Public Sans', sans-serif;
  color: #636e72;
  margin-bottom: 12px;
}

.classic-formal__divider {
  height: 2px;
  background: #2d3436;
  margin: 0 40px;
}

.classic-formal__section {
  margin-bottom: 16px;
}

.classic-formal__section-title {
  font-size: 15px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid #2d3436;
  padding-bottom: 3px;
  margin: 0 0 10px;
}

.classic-formal__table {
  width: 100%;
  border-collapse: collapse;
  font-family: 'Public Sans', sans-serif;
  font-size: 12px;
}

.classic-formal__table th {
  text-align: left;
  padding: 4px 8px;
  border-bottom: 1px solid #ddd;
  font-weight: 600;
  font-size: 11px;
  color: #636e72;
}

.classic-formal__table td {
  padding: 6px 8px;
  border-bottom: 1px solid #eee;
}

.classic-formal__entry {
  margin-bottom: 14px;
}

.classic-formal__entry-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
}

.classic-formal__entry-header em {
  font-style: normal;
  font-family: 'Public Sans', sans-serif;
  color: #636e72;
  font-size: 12px;
}

.classic-formal__date {
  margin-left: auto;
  font-family: 'Public Sans', sans-serif;
  font-size: 11px;
  color: #636e72;
  white-space: nowrap;
}

.classic-formal__desc {
  font-size: 12px;
  font-family: 'Public Sans', sans-serif;
  margin: 6px 0;
  line-height: 1.8;
  text-indent: 2em;
}

.classic-formal__highlights {
  margin: 4px 0;
  padding-left: 20px;
  font-family: 'Public Sans', sans-serif;
  font-size: 12px;
  line-height: 1.8;
}

.classic-formal__skill-line {
  font-size: 12px;
  font-family: 'Public Sans', sans-serif;
  line-height: 1.8;
  margin-bottom: 4px;
}
</style>
