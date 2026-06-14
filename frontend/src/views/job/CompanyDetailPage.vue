<!-- src/views/job/CompanyDetailPage.vue -->
<template>
  <MainLayout>
    <div class="company-detail-page">
      <NbCard v-if="jobStore.loading">
        <NbLoadingBlock title="加载公司信息..." :rows="5" />
      </NbCard>

      <template v-else-if="company">
        <NbButton variant="ghost" @click="router.back()">&larr; 返回</NbButton>

        <NbCard class="company-detail-page__hero">
          <NbPageHeader
            :eyebrow="company.industry"
            :title="company.name"
            :description="company.description || undefined"
          >
            <template #actions>
              <div class="company-detail-page__hero-meta">
                <NbStatusBadge v-if="company.city" :label="company.city" variant="info" />
                <NbStatusBadge v-if="company.scale" :label="company.scale" variant="default" />
                <el-link
                  v-if="company.website"
                  :href="company.website"
                  target="_blank"
                  type="primary"
                >
                  官网
                </el-link>
              </div>
            </template>
          </NbPageHeader>
        </NbCard>

        <div v-if="certifications.length > 0" class="company-detail-page__section">
          <NbCard>
            <NbSectionTitle title="资质认证" description="企业资质与认证信息" />
            <div class="company-detail-page__certs">
              <div v-for="cert in certifications" :key="cert.id" class="company-detail-page__cert">
                <NbStatusBadge
                  :label="certStatusLabel(cert)"
                  :variant="certStatusVariant(cert.status)"
                />
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
            <NbSectionTitle title="主营业务" />
            <p class="company-detail-page__text">{{ company.mainBusiness }}</p>
          </NbCard>
        </div>

        <div v-if="company.techDirection" class="company-detail-page__section">
          <NbCard>
            <NbSectionTitle title="技术方向" />
            <p class="company-detail-page__text">{{ company.techDirection }}</p>
          </NbCard>
        </div>
      </template>

      <NbCard v-else>
        <NbEmptyState title="未找到该公司信息" />
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
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

function certStatusVariant(status: string) {
  if (status === 'confirmed') return 'success'
  if (status === 'suspected') return 'warning'
  return 'muted'
}
</script>

<style scoped>
.company-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.company-detail-page__hero {
  padding: 32px;
}

.company-detail-page__hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.company-detail-page__section {
  margin-top: 4px;
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
</style>
