<!-- src/views/job/JobImportPage.vue -->
<template>
  <MainLayout>
    <div class="job-import-page">
      <NbPageHeader
        eyebrow="职位情报"
        title="手动补充职位"
        description="当系统推荐之外还有目标职位时，可手动粘贴链接补充。"
      >
        <template #actions>
          <NbButton @click="router.push('/job/favorites')">我的收藏</NbButton>
        </template>
      </NbPageHeader>

      <NbCard class="job-import-page__form-card">
        <el-form label-position="top" @submit.prevent="handleImport">
          <el-form-item label="链接地址">
            <el-input
              v-model="form.url"
              placeholder="粘贴招聘网站职位链接，如 Boss直聘、拉勾、猎聘等"
              size="large"
              clearable
            />
          </el-form-item>
          <el-form-item label="导入类型">
            <el-select v-model="form.importType" size="large" style="width: 100%;">
              <el-option label="职位详情" value="job" />
              <el-option label="公司官网" value="company_website" />
              <el-option label="公司招聘页" value="company_career_page" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <NbButton variant="primary" :loading="jobStore.loading" block @click="handleImport">
              开始导入
            </NbButton>
          </el-form-item>
        </el-form>
      </NbCard>

      <NbCard v-if="lastResult" class="job-import-page__result-card">
        <NbSectionTitle title="导入结果" />
        <div v-if="lastResult.job" class="job-import-page__result-item">
          <span>职位：{{ lastResult.job.title }}</span>
          <NbButton variant="ghost" @click="router.push(`/job/${lastResult.jobId}`)">查看详情</NbButton>
        </div>
        <div v-if="lastResult.company" class="job-import-page__result-item">
          <span>公司：{{ lastResult.company.name }}</span>
          <NbButton variant="ghost" @click="router.push(`/company/${lastResult.companyId}`)">查看详情</NbButton>
        </div>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import { useJobStore } from '@/stores/job'
import type { JobImportResultVO } from '@/types/job'

const router = useRouter()
const jobStore = useJobStore()

const form = reactive({
  url: '',
  importType: 'job' as 'job' | 'company_website' | 'company_career_page',
})

const lastResult = ref<JobImportResultVO | null>(null)

async function handleImport() {
  if (!form.url.trim()) {
    ElMessage.warning('请输入链接地址')
    return
  }

  const result = await jobStore.importUrl({
    url: form.url.trim(),
    importType: form.importType,
  })

  if (result) {
    lastResult.value = result
    ElMessage.success('导入成功')
    if (result.resultType === 'job' && result.jobId) {
      router.push(`/job/${result.jobId}`)
    } else if (result.resultType === 'company' && result.companyId) {
      router.push(`/company/${result.companyId}`)
    }
  }
}
</script>

<style scoped>
.job-import-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 640px;
  margin: 0 auto;
}

.job-import-page__form-card {
  padding: 32px;
}

.job-import-page__result-card {
  padding: 24px;
}

.job-import-page__result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 2px solid var(--nb-bg);
}

.job-import-page__result-item:last-child {
  border-bottom: none;
}
</style>
