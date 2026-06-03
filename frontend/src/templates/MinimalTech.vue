<!-- src/templates/MinimalTech.vue -->
<template>
  <div class="minimal-tech">
    <div class="minimal-tech__header">
      <h1 class="minimal-tech__name">{{ basic?.name || '你的名字' }}</h1>
      <div class="minimal-tech__contact">
        <span v-if="basic?.email">{{ basic.email }}</span>
        <span v-if="basic?.phone">{{ basic.phone }}</span>
        <span v-if="basic?.city">{{ basic.city }}</span>
        <span v-if="basic?.github">{{ basic.github }}</span>
        <span v-if="basic?.blog">{{ basic.blog }}</span>
      </div>
    </div>

    <div v-if="skillCategories.length" class="minimal-tech__section">
      <h2 class="minimal-tech__section-title">技能</h2>
      <div class="minimal-tech__skills">
        <template v-for="(cat, index) in skillCategories" :key="index">
          <div v-if="cat.items?.length" class="minimal-tech__skill-group">
            <span class="minimal-tech__skill-cat">{{ cat.name }}：</span>
            <span>{{ cat.items.join(' / ') }}</span>
          </div>
        </template>
      </div>
    </div>

    <div v-if="work.length" class="minimal-tech__section">
      <h2 class="minimal-tech__section-title">工作经历</h2>
      <div v-for="(item, index) in work" :key="index" class="minimal-tech__entry">
        <div class="minimal-tech__entry-header">
          <strong>{{ item.company }}</strong>
          <span v-if="item.startDate || item.endDate" class="minimal-tech__date">
            {{ item.startDate }} - {{ item.endDate || '至今' }}
          </span>
        </div>
        <div v-if="item.position" class="minimal-tech__sub">{{ item.position }}</div>
        <p v-if="item.description" class="minimal-tech__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="minimal-tech__highlights">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <div v-if="project.length" class="minimal-tech__section">
      <h2 class="minimal-tech__section-title">项目经历</h2>
      <div v-for="(item, index) in project" :key="index" class="minimal-tech__entry">
        <div class="minimal-tech__entry-header">
          <strong>{{ item.name }}</strong>
          <span v-if="item.startDate || item.endDate" class="minimal-tech__date">
            {{ item.startDate }} - {{ item.endDate || '至今' }}
          </span>
        </div>
        <div v-if="item.role" class="minimal-tech__sub">{{ item.role }}</div>
        <p v-if="item.description" class="minimal-tech__desc">{{ item.description }}</p>
        <ul v-if="(item.highlights as string[])?.length" class="minimal-tech__highlights">
          <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
        </ul>
      </div>
    </div>

    <div v-if="education.length" class="minimal-tech__section">
      <h2 class="minimal-tech__section-title">教育经历</h2>
      <div v-for="(item, index) in education" :key="index" class="minimal-tech__entry">
        <div class="minimal-tech__entry-header">
          <strong>{{ item.school }}</strong>
          <span v-if="item.startDate || item.endDate" class="minimal-tech__date">
            {{ item.startDate }} - {{ item.endDate || '至今' }}
          </span>
        </div>
        <div class="minimal-tech__sub">
          <span v-if="item.major">{{ item.major }}</span>
          <span v-if="item.degree"> · {{ item.degree }}</span>
          <span v-if="item.gpa"> · GPA: {{ item.gpa }}</span>
        </div>
      </div>
    </div>

    <div v-if="summary?.content" class="minimal-tech__section">
      <h2 class="minimal-tech__section-title">自我评价</h2>
      <p class="minimal-tech__desc">{{ summary.content }}</p>
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
.minimal-tech {
  font-family: 'Public Sans', sans-serif;
  color: #2d3436;
}

.minimal-tech__header {
  margin-bottom: 20px;
  border-bottom: 3px solid #2d3436;
  padding-bottom: 12px;
}

.minimal-tech__name {
  font-family: 'Lexend Mega', sans-serif;
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.minimal-tech__contact {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #636e72;
}

.minimal-tech__section {
  margin-bottom: 16px;
}

.minimal-tech__section-title {
  font-family: 'Lexend Mega', sans-serif;
  font-size: 14px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1.5px solid #2d3436;
  padding-bottom: 4px;
  margin: 0 0 10px;
}

.minimal-tech__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
}

.minimal-tech__skill-group {
  line-height: 1.6;
}

.minimal-tech__skill-cat {
  font-weight: 600;
}

.minimal-tech__entry {
  margin-bottom: 12px;
}

.minimal-tech__entry-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.minimal-tech__date {
  font-size: 11px;
  color: #636e72;
  white-space: nowrap;
}

.minimal-tech__sub {
  font-size: 12px;
  color: #636e72;
}

.minimal-tech__desc {
  font-size: 12px;
  margin: 4px 0;
  line-height: 1.6;
}

.minimal-tech__highlights {
  margin: 4px 0;
  padding-left: 16px;
  font-size: 12px;
  line-height: 1.6;
}
</style>
