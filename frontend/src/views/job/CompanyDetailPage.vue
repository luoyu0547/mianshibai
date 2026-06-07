<!-- src/views/job/CompanyDetailPage.vue -->
<template>
  <MainLayout>
    <div class="company-detail-page">
      <div v-if="jobStore.loading" class="company-detail-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="company">
        <div class="company-detail-page__header">
          <el-button text @click="router.back()">&larr; 返回</el-button>
        </div>

        <NbCard class="company-detail-page__hero">
          <h1 class="company-detail-page__name">{{ company.name }}</h1>
          <div class="company-detail-page__meta">
            <el-tag v-if="company.industry" effect="plain">{{ company.industry }}</el-tag>
            <span v-if="company.city">{{ company.city }}</span>
            <span v-if="company.scale">{{ company.scale }}</span>
            <el-link
              v-if="company.website"
              :href="company.website"
              target="_blank"
              type="primary"
            >
              官网
            </el-link>
          </div>
        </NbCard>

        <div v-if="certifications.length > 0" class="company-detail-page__section">
          <NbCard>
            <h3 class="company-detail-page__section-title">资质认证</h3>
            <div class="company-detail-page__certs">
              <div v-for="cert in certifications" :key="cert.id" class="company-detail-page__cert">
                <el-tag
                  :type="certStatusTagType(cert.status)"
                  class="company-detail-page__cert-badge"
                >
                  {{ certStatusLabel(cert) }}
                </el-tag>
                <div class="company-detail-page__cert-info">
                  <span v-if="cert.evidenceText" class="company-detail-page__cert-text">
                    {{ cert.evidenceText }}
                  </span>
                  <el-link
                    v-if="cert.evidenceUrl"
                    :href="cert.evidenceUrl"
                    target="_blank"
                    :underline="false"
                    class="company-detail-page__cert-link"
                  >
                    查看来源
                  </el-link>
                </div>
              </div>
            </div>
          </NbCard>
        </div>

        <div v-if="company.mainBusiness" class="company-detail-page__section">
          <NbCard>
            <h3 class="company-detail-page__section-title">主营业务</h3>
            <p class="company-detail-page__text">{{ company.mainBusiness }}</p>
          </NbCard>
        </div>

        <div v-if="company.techDirection" class="company-detail-page__section">
          <NbCard>
            <h3 class="company-detail-page__section-title">技术方向</h3>
            <p class="company-detail-page__text">{{ company.techDirection }}</p>
          </NbCard>
        </div>

        <div v-if="company.description" class="company-detail-page__section">
          <NbCard>
            <h3 class="company-detail-page__section-title">公司简介</h3>
            <p class="company-detail-page__text">{{ company.description }}</p>
          </NbCard>
        </div>
      </template>

      <div v-else class="company-detail-page__empty">
        <p>未找到该公司信息</p>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useJobStore } from '@/stores/job'
import type { CompanyCertificationVO } from '@/types/job'

const route = useRoute()
const router = useRouter()
const jobStore = useJobStore()

const company = computed(() => jobStore.currentCompany)
const certifications = computed(() => company.value?.certifications ?? [])

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    jobStore.fetchCompanyDetail(id)
  }
})

function certStatusLabel(cert: CompanyCertificationVO): string {
  if (cert.status === 'confirmed') return `已确认${cert.certificationType}`
  if (cert.status === 'suspected') return `疑似${cert.certificationType}`
  return `${cert.certificationType}（${cert.status}）`
}

function certStatusTagType(status: string) {
  if (status === 'confirmed') return 'success'
  if (status === 'suspected') return 'warning'
  return 'info'
}
</script>

<style scoped>
.company-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.company-detail-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.company-detail-page__header {
  display: flex;
  align-items: center;
}

.company-detail-page__hero {
  padding: 32px;
}

.company-detail-page__name {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 12px 0;
}

.company-detail-page__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--nb-muted);
}

.company-detail-page__section {
  margin-top: 4px;
}

.company-detail-page__section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.company-detail-page__certs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.company-detail-page__cert {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--nb-bg);
  border-radius: var(--nb-radius);
}

.company-detail-page__cert-badge {
  flex-shrink: 0;
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.company-detail-page__cert-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.company-detail-page__cert-text {
  font-size: 14px;
  color: var(--nb-text);
  line-height: 1.6;
}

.company-detail-page__cert-link {
  font-size: 13px;
}

.company-detail-page__text {
  color: var(--nb-text);
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}

.company-detail-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}
</style>
