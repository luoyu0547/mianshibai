<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { predictJobQuestions } from '@/api/job'
import type { JobQuestionPredictionVO } from '@/types/job'
import NbCard from '@/components/NbCard.vue'

const route = useRoute()
const jobId = Number(route.params.id)
const questions = ref<JobQuestionPredictionVO | null>(null)
const loading = ref(false)

async function loadQuestions() {
  loading.value = true
  try {
    const res = await predictJobQuestions(jobId)
    questions.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<template>
  <div class="job-questions-page">
    <h2>预测面试题</h2>
    <div v-loading="loading">
      <div v-if="questions">
        <NbCard style="margin-bottom: 16px;">
          <h3>技术面试题</h3>
          <ul><li v-for="(q, i) in questions.technicalQuestions" :key="i">{{ q }}</li></ul>
        </NbCard>
        <NbCard style="margin-bottom: 16px;">
          <h3>项目相关题</h3>
          <ul><li v-for="(q, i) in questions.projectQuestions" :key="i">{{ q }}</li></ul>
        </NbCard>
        <NbCard style="margin-bottom: 16px;" v-if="questions.systemDesignQuestions.length > 0">
          <h3>系统设计题</h3>
          <ul><li v-for="(q, i) in questions.systemDesignQuestions" :key="i">{{ q }}</li></ul>
        </NbCard>
        <NbCard style="margin-bottom: 16px;">
          <h3>HR 面试题</h3>
          <ul><li v-for="(q, i) in questions.hrQuestions" :key="i">{{ q }}</li></ul>
        </NbCard>
      </div>
    </div>
  </div>
</template>
