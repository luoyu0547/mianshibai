<!-- 现代双栏风 — 侧边栏 + 主内容区 -->
<template>
  <div class="modern-split">
    <!-- 左侧栏 -->
    <aside class="ms-sidebar">
      <div class="ms-avatar">{{ (basic?.name as string)?.[0] || '你' }}</div>
      <h1 class="ms-name">{{ basic?.name || '你的名字' }}</h1>
      <p v-if="basic?.targetPosition" class="ms-title">{{ basic.targetPosition }}</p>

      <div class="ms-contact">
        <div v-if="basic?.email" class="ms-contact-row">{{ basic.email }}</div>
        <div v-if="basic?.phone" class="ms-contact-row">{{ basic.phone }}</div>
        <div v-if="basic?.city" class="ms-contact-row">{{ basic.city }}</div>
        <div v-if="basic?.github" class="ms-contact-row">{{ basic.github }}</div>
        <div v-if="basic?.blog" class="ms-contact-row">{{ basic.blog }}</div>
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
          <div class="ms-edu-school">{{ item.school }}</div>
          <div class="ms-edu-major">{{ item.major }}<template v-if="item.degree"> · {{ item.degree }}</template></div>
          <div class="ms-edu-date" v-if="item.startDate">{{ item.startDate }} — {{ item.endDate || '至今' }}</div>
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
          <p v-if="item.description" class="ms-desc">{{ item.description }}</p>
          <ul v-if="(item.highlights as string[])?.length" class="ms-list">
            <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
          </ul>
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
          <p v-if="item.description" class="ms-desc">{{ item.description }}</p>
          <ul v-if="(item.highlights as string[])?.length" class="ms-list">
            <li v-for="(h, hi) in item.highlights" :key="hi">{{ h }}</li>
          </ul>
        </div>
      </div>

      <div v-if="summary?.content" class="ms-block">
        <h2 class="ms-block-title">自我评价</h2>
        <p class="ms-desc">{{ summary.content }}</p>
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
.modern-split {
  display: flex;
  font-family: var(--font-body);
  color: var(--nb-ink);
  min-height: 100%;
}

/* 左侧栏 */
.ms-sidebar {
  width: 34%;
  background: linear-gradient(180deg, var(--nb-primary-light) 0%, rgba(255,255,255,0) 100%);
  padding: 36px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid var(--nb-border-color-light);
}

.ms-avatar {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  background: var(--nb-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(91, 94, 244, 0.25);
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
  border-bottom: 2px solid var(--nb-primary);
  color: var(--nb-primary);
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
  background: var(--nb-primary);
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
  border-bottom: 2px solid var(--nb-border-color);
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

.ms-list {
  margin: 5px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--nb-muted);
  line-height: 1.7;
}
</style>
