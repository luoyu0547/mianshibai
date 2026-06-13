<template>
  <MainLayout>
    <div class="diagnosis-page">
      <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
      <NbCard v-if="coachStore.currentDiagnosis" class="diagnosis-card">
        <div class="diagnosis-header">
          <div>
            <h1>{{ coachStore.currentDiagnosis.title }}</h1>
            <p>{{ formatDate(coachStore.currentDiagnosis.createTime) }} · {{ coachStore.currentDiagnosis.source === 'ai' ? 'AI 生成' : '规则兜底' }}</p>
          </div>
          <div class="score">{{ coachStore.currentDiagnosis.overallScore }}</div>
        </div>
        <p class="summary">{{ coachStore.currentDiagnosis.summary }}</p>
        <el-progress :percentage="coachStore.currentDiagnosis.dataCompleteness" :stroke-width="12" />
        <section><h2>优势</h2><ul><li v-for="item in coachStore.currentDiagnosis.strengths" :key="item">{{ item }}</li></ul></section>
        <section><h2>短板</h2><ul><li v-for="item in coachStore.currentDiagnosis.weaknesses" :key="item">{{ item }}</li></ul></section>
        <section><h2>建议</h2><ul><li v-for="item in coachStore.currentDiagnosis.suggestions" :key="item">{{ item }}</li></ul></section>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import { useCoachStore } from '@/stores/coach'

const route = useRoute()
const router = useRouter()
const coachStore = useCoachStore()

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(() => coachStore.fetchDiagnosis(Number(route.params.id)))
</script>

<style scoped>
.diagnosis-page { display: flex; flex-direction: column; gap: 20px; }
.diagnosis-card { display: flex; flex-direction: column; gap: 20px; }
.diagnosis-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.diagnosis-header h1 { margin: 0; font-family: var(--font-heading); }
.diagnosis-header p, .summary { color: var(--nb-muted); }
.score { width: 88px; height: 88px; display: grid; place-items: center; border: var(--nb-border); box-shadow: var(--nb-shadow); background: var(--nb-primary); color: white; font-size: 34px; font-weight: 800; }
section { border-top: var(--nb-border); padding-top: 16px; }
li { margin-bottom: 8px; }
</style>
