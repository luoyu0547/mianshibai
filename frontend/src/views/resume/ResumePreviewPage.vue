<!-- src/views/resume/ResumePreviewPage.vue -->
<template>
  <MainLayout>
    <div class="resume-preview-page">
      <h1 class="resume-preview-page__title">简历预览</h1>
      <p>此页面将在后续任务中实现。</p>
      <el-button @click="router.push('/resume')">返回列表</el-button>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const router = useRouter()
</script>

<style scoped>
.resume-preview-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.resume-preview-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}
</style>
