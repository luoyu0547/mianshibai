<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { predictJobQuestions } from '@/api/job'
import type { JobQuestionPredictionVO } from '@/types/job'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'

const route = useRoute()
const router = useRouter()
const jobId = Number(route.params.id)
const questions = ref<JobQuestionPredictionVO | null>(null)
const loading = ref(false)

async function loadQuestions() {
  loading.value = true
  try {
    const res = await predictJobQuestions(jobId)
    questions.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadQuestions()
})
</script>

<template>
  <MainLayout>
    <div class="job-questions-page">
      <NbPageHeader
        eyebrow="职位情报"
        title="预测面试题"
        description="基于职位需求 AI 预测的高频面试问题"
      >
        <template #actions>
          <NbButton @click="router.push(`/job/${jobId}`)">返回职位</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="loading">
        <NbLoadingBlock title="生成预测面试题..." :rows="6" />
      </NbCard>

      <template v-else-if="questions">
        <NbCard variant="ai" class="job-questions-page__card">
          <NbSectionTitle title="技术面试题" description="考察核心技术栈基础知识" />
          <ul class="job-questions-page__list">
            <li v-for="(q, i) in questions.technicalQuestions" :key="i">{{ q }}</li>
          </ul>
        </NbCard>

        <NbCard variant="ai" class="job-questions-page__card">
          <NbSectionTitle title="项目相关题" description="针对项目经验的深挖方向" />
          <ul class="job-questions-page__list">
            <li v-for="(q, i) in questions.projectQuestions" :key="i">{{ q }}</li>
          </ul>
        </NbCard>

        <NbCard
          v-if="questions.systemDesignQuestions.length > 0"
          variant="ai"
          class="job-questions-page__card"
        >
          <NbSectionTitle title="系统设计题" description="架构与系统设计能力考察" />
          <ul class="job-questions-page__list">
            <li v-for="(q, i) in questions.systemDesignQuestions" :key="i">{{ q }}</li>
          </ul>
        </NbCard>

        <NbCard variant="ai" class="job-questions-page__card">
          <NbSectionTitle title="HR 面试题" description="软素质与文化匹配度问题" />
          <ul class="job-questions-page__list">
            <li v-for="(q, i) in questions.hrQuestions" :key="i">{{ q }}</li>
          </ul>
        </NbCard>
      </template>
    </div>
  </MainLayout>
</template>

<style scoped>
.job-questions-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.job-questions-page__card {
  padding: 28px;
}

.job-questions-page__list {
  margin: 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--nb-text);
  line-height: 1.7;
}

.job-questions-page__list li {
  font-size: 15px;
}
</style>
