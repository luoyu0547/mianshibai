<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Job Crawl"
        title="职位采集审核池"
        description="审核待确认的采集职位"
      >
        <template #actions>
          <NbButton variant="ghost" @click="router.push('/admin/job-crawl')">返回列表</NbButton>
        </template>
      </NbPageHeader>

      <NbCard>
        <div class="filters">
          <el-select
            v-model="filterReviewStatus"
            placeholder="审核状态"
            clearable
            style="width: 160px"
            @change="loadItems"
          >
            <el-option label="待审核" value="pending_review" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="重复" value="duplicate" />
            <el-option label="失败" value="failed" />
          </el-select>
          <el-input
            v-model="filterKeyword"
            placeholder="搜索关键词"
            clearable
            style="width: 240px"
            @clear="loadItems"
            @keyup.enter="loadItems"
          />
        </div>
      </NbCard>

      <NbLoadingBlock v-if="jobCrawlStore.reviewLoading" title="加载审核项..." :rows="6" />

      <NbCard v-else-if="jobCrawlStore.reviewItems.length === 0">
        <NbEmptyState
          title="暂无审核项"
          description="当前没有待审核的职位采集项"
        >
          <template #action>
            <NbButton variant="primary" @click="router.push('/admin/job-crawl')">返回列表</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <template v-else>
        <NbCard v-for="item in jobCrawlStore.reviewItems" :key="item.id" class="review-card">
          <div class="review-card__header">
            <div class="review-card__title-row">
              <h3 class="review-card__title">{{ item.rawTitle || '无标题' }}</h3>
              <NbStatusBadge
                :label="reviewStatusLabel(item.reviewStatus)"
                :variant="reviewStatusVariant(item.reviewStatus)"
              />
            </div>
            <div class="review-card__meta">
              <span v-if="item.rawCompanyName" class="review-card__company">{{ item.rawCompanyName }}</span>
              <span class="review-card__city">{{ parseExtracted(item).city }}</span>
              <span class="review-card__salary">{{ parseExtracted(item).salary }}</span>
              <span v-if="item.sourcePlatform" class="review-card__platform">{{ item.sourcePlatform }}</span>
              <span v-if="item.createTime" class="review-card__time">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>

          <div v-if="item.summary" class="review-card__summary">
            <span class="review-card__label">摘要：</span>
            <span>{{ item.summary }}</span>
          </div>

          <div v-if="item.tagsJson" class="review-card__tags">
            <el-tag
              v-for="(tag, i) in parseTags(item.tagsJson)"
              :key="i"
              size="small"
              class="review-card__tag"
            >
              {{ tag }}
            </el-tag>
          </div>

          <div class="review-card__scores">
            <span class="review-card__score">
              质量分：<strong>{{ item.qualityScore ?? '-' }}</strong>
            </span>
            <span class="review-card__score">
              置信度：<strong>{{ item.confidenceScore ?? '-' }}</strong>
            </span>
          </div>

          <div v-if="item.sourceUrl" class="review-card__source">
            <span class="review-card__label">来源：</span>
            <a :href="item.sourceUrl" target="_blank" class="review-card__link">{{ item.sourceUrl }}</a>
          </div>

          <div class="review-card__actions" v-if="item.reviewStatus === 'pending_review'">
            <NbButton variant="primary" @click="handleApprove(item)">通过</NbButton>
            <NbButton variant="danger" @click="handleReject(item)">拒绝</NbButton>
            <NbButton variant="secondary" @click="handleDuplicate(item)">标记重复</NbButton>
          </div>
        </NbCard>
      </template>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import { useJobCrawlStore } from '@/stores/jobCrawl'
import type { AdminJobCrawlItemVO, JobCrawlReviewStatus } from '@/types/jobCrawl'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const jobCrawlStore = useJobCrawlStore()

function parseExtracted(item: AdminJobCrawlItemVO): { city: string; salary: string } {
  try {
    const parsed = JSON.parse(item.extractedJson || '{}')
    return { city: parsed.city || '', salary: parsed.salaryRange || '' }
  } catch {
    return { city: '', salary: '' }
  }
}

const filterReviewStatus = ref<JobCrawlReviewStatus | undefined>('pending_review')
const filterKeyword = ref('')

function reviewStatusLabel(status: JobCrawlReviewStatus): string {
  const map: Record<JobCrawlReviewStatus, string> = {
    pending_review: '待审核',
    approved: '已通过',
    rejected: '已拒绝',
    duplicate: '重复',
    failed: '失败',
  }
  return map[status] ?? status
}

function reviewStatusVariant(status: JobCrawlReviewStatus): 'warning' | 'success' | 'danger' | 'muted' | 'info' {
  const map: Record<JobCrawlReviewStatus, 'warning' | 'success' | 'danger' | 'muted'> = {
    pending_review: 'warning',
    approved: 'success',
    rejected: 'danger',
    duplicate: 'muted',
    failed: 'danger',
  }
  return map[status] ?? 'info'
}

function parseTags(tagsJson: string): string[] {
  try {
    const parsed = JSON.parse(tagsJson)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function loadItems() {
  jobCrawlStore.loadReviewItems({
    reviewStatus: filterReviewStatus.value,
    keyword: filterKeyword.value || undefined,
  })
}

async function handleApprove(item: AdminJobCrawlItemVO) {
  await jobCrawlStore.approveItem(item.id, {})
  ElMessage.success('职位已发布')
}

async function handleReject(item: AdminJobCrawlItemVO) {
  await jobCrawlStore.rejectItem(item.id, { reviewNote: '管理员拒绝' })
  ElMessage.success('已拒绝')
}

async function handleDuplicate(item: AdminJobCrawlItemVO) {
  if (!item.duplicateOfJobId) {
    ElMessage.warning('该卡片没有可关联的重复职位')
    return
  }
  await jobCrawlStore.markDuplicate(item.id, { duplicateOfJobId: item.duplicateOfJobId })
  ElMessage.success('已标记重复')
}

onMounted(() => {
  loadItems()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-card__header {
  margin-bottom: 12px;
}

.review-card__title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.review-card__title {
  margin: 0;
  font-size: 17px;
  font-family: var(--font-heading);
  color: var(--nb-ink);
}

.review-card__meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--nb-muted);
}

.review-card__company {
  font-weight: 600;
  color: var(--nb-ink);
}

.review-card__city,
.review-card__salary {
  font-weight: 500;
  color: var(--nb-primary);
}

.review-card__summary {
  font-size: 14px;
  color: var(--nb-ink);
  margin-bottom: 10px;
  line-height: 1.6;
}

.review-card__label {
  font-weight: 600;
  font-size: 12px;
  color: var(--nb-muted);
  text-transform: uppercase;
}

.review-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.review-card__tag {
  font-size: 12px;
}

.review-card__scores {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.review-card__score {
  font-size: 13px;
  color: var(--nb-muted);
}

.review-card__score strong {
  color: var(--nb-ink);
}

.review-card__source {
  font-size: 13px;
  margin-bottom: 12px;
  word-break: break-all;
}

.review-card__link {
  color: var(--nb-primary);
  text-decoration: none;
}

.review-card__link:hover {
  text-decoration: underline;
}

.review-card__actions {
  display: flex;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--nb-border);
}
</style>
