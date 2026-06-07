<!-- src/views/job/JobDetailPage.vue -->
<template>
  <MainLayout>
    <div class="job-detail-page">
      <div v-if="jobStore.loading" class="job-detail-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="job">
        <div class="job-detail-page__header">
          <el-button text @click="router.back()">&larr; 返回</el-button>
        </div>

        <div class="job-detail-page__hero">
          <div class="job-detail-page__hero-left">
            <h1 class="job-detail-page__title">{{ job.title }}</h1>
            <div class="job-detail-page__meta">
              <span
                v-if="job.company"
                class="job-detail-page__company-link"
                @click="router.push(`/company/${job.company.id}`)"
              >
                {{ job.companyName }}
              </span>
              <span v-else>{{ job.companyName }}</span>
              <span class="job-detail-page__meta-divider">|</span>
              <span>{{ job.city }}</span>
              <span class="job-detail-page__meta-divider">|</span>
              <span class="job-detail-page__salary">{{ job.salaryRange }}</span>
              <span v-if="job.experienceRequirement" class="job-detail-page__meta-divider">|</span>
              <span v-if="job.experienceRequirement">{{ job.experienceRequirement }}</span>
              <span v-if="job.educationRequirement" class="job-detail-page__meta-divider">|</span>
              <span v-if="job.educationRequirement">{{ job.educationRequirement }}</span>
            </div>
          </div>
          <div class="job-detail-page__hero-actions">
            <NbButton
              :type="job.favorited ? 'secondary' : 'primary'"
              @click="jobStore.toggleFavorite(job.id)"
            >
              {{ job.favorited ? '已收藏' : '收藏' }}
            </NbButton>
            <el-button @click="router.push('/interview/new')">开始面试</el-button>
          </div>
        </div>

        <el-tag
          v-if="match && match.recommendation"
          :type="recommendationTagType(match.recommendation)"
          class="job-detail-page__rec-badge"
          size="large"
        >
          {{ match.recommendation }} — 总分 {{ match.totalScore }}
        </el-tag>

        <div v-if="job.techStack" class="job-detail-page__section">
          <NbCard>
            <h3 class="job-detail-page__section-title">技术栈</h3>
            <div class="job-detail-page__tags">
              <el-tag
                v-for="tech in parseTechStack(job.techStack)"
                :key="tech"
                effect="plain"
                class="job-detail-page__tech-tag"
              >
                {{ tech }}
              </el-tag>
            </div>
          </NbCard>
        </div>

        <div class="job-detail-page__section">
          <NbCard>
            <h3 class="job-detail-page__section-title">职位描述</h3>
            <div class="job-detail-page__content" v-html="formatNewlines(job.jobDescription)"></div>
            <template v-if="job.jobRequirement">
              <h3 class="job-detail-page__section-title" style="margin-top: 24px;">任职要求</h3>
              <div class="job-detail-page__content" v-html="formatNewlines(job.jobRequirement)"></div>
            </template>
          </NbCard>
        </div>

        <div v-if="job.analysis" class="job-detail-page__section">
          <NbCard>
            <h3 class="job-detail-page__section-title">AI 分析</h3>
            <el-descriptions :column="1" border>
              <el-descriptions-item v-if="job.analysis.requirementSummary" label="需求摘要">
                {{ job.analysis.requirementSummary }}
              </el-descriptions-item>
              <el-descriptions-item v-if="job.analysis.coreSkills" label="核心技能">
                {{ job.analysis.coreSkills }}
              </el-descriptions-item>
              <el-descriptions-item v-if="job.analysis.hiddenRequirements" label="隐藏要求">
                {{ job.analysis.hiddenRequirements }}
              </el-descriptions-item>
              <el-descriptions-item v-if="job.analysis.riskPoints" label="风险点">
                {{ job.analysis.riskPoints }}
              </el-descriptions-item>
              <el-descriptions-item v-if="job.analysis.interviewFocus" label="面试重点">
                {{ job.analysis.interviewFocus }}
              </el-descriptions-item>
              <el-descriptions-item v-if="job.analysis.resumeSuggestions" label="简历建议">
                {{ job.analysis.resumeSuggestions }}
              </el-descriptions-item>
            </el-descriptions>
          </NbCard>
        </div>

        <div class="job-detail-page__section">
          <NbCard>
            <h3 class="job-detail-page__section-title">简历匹配</h3>
            <div class="job-detail-page__match-row">
              <el-select v-model="selectedResumeId" placeholder="选择简历" style="width: 240px;">
                <el-option
                  v-for="r in resumeStore.resumeList"
                  :key="r.id"
                  :label="r.title"
                  :value="r.id"
                />
              </el-select>
              <NbButton
                type="primary"
                :loading="jobStore.loading"
                :disabled="!selectedResumeId"
                @click="handleMatch"
              >
                开始匹配
              </NbButton>
            </div>
            <div v-if="match" class="job-detail-page__match-scores">
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">综合评分</span>
                <span class="job-detail-page__score-value">{{ match.totalScore }}</span>
              </div>
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">匹配度</span>
                <span class="job-detail-page__score-value">{{ match.matchScore }}</span>
              </div>
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">成长性</span>
                <span class="job-detail-page__score-value">{{ match.growthScore }}</span>
              </div>
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">技术成长</span>
                <span class="job-detail-page__score-value">{{ match.techGrowthScore }}</span>
              </div>
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">薪资城市</span>
                <span class="job-detail-page__score-value">{{ match.salaryCityScore }}</span>
              </div>
              <div class="job-detail-page__score-item">
                <span class="job-detail-page__score-label">经验匹配</span>
                <span class="job-detail-page__score-value">{{ match.experienceFitScore }}</span>
              </div>
            </div>
            <div v-if="match && match.reason" class="job-detail-page__match-detail">
              <p><strong>推荐理由：</strong>{{ match.reason }}</p>
            </div>
            <div v-if="match && match.gaps" class="job-detail-page__match-detail">
              <p><strong>差距分析：</strong>{{ match.gaps }}</p>
            </div>
          </NbCard>
        </div>

        <div v-if="job.sourceUrl" class="job-detail-page__section">
          <el-link :href="job.sourceUrl" target="_blank" type="primary">
            查看原始链接（{{ job.sourcePlatform }}）
          </el-link>
        </div>
      </template>

      <div v-else class="job-detail-page__empty">
        <p>未找到该职位信息</p>
        <el-button type="primary" text @click="router.push('/job/import')">去导入</el-button>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useJobStore } from '@/stores/job'
import { useResumeStore } from '@/stores/resume'

const route = useRoute()
const router = useRouter()
const jobStore = useJobStore()
const resumeStore = useResumeStore()

const selectedResumeId = ref<number | undefined>(undefined)

const job = computed(() => jobStore.currentJob)
const match = computed(() => jobStore.currentMatch)

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    jobStore.fetchJobDetail(id)
  }
  resumeStore.fetchResumeList()
})

async function handleMatch() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择简历')
    return
  }
  const jobId = Number(route.params.id)
  const result = await jobStore.matchResume(jobId, { resumeId: selectedResumeId.value })
  if (result) {
    ElMessage.success('匹配完成')
  }
}

function parseTechStack(techStack: string): string[] {
  return techStack.split(/[,，、;；\n]/).map((s) => s.trim()).filter(Boolean)
}

function formatNewlines(text: string): string {
  return text.replace(/\n/g, '<br/>')
}

function recommendationTagType(rec: string) {
  if (rec.includes('强烈推荐') || rec.includes('高度匹配')) return 'success'
  if (rec.includes('推荐') || rec.includes('匹配')) return ''
  if (rec.includes('谨慎') || rec.includes('一般')) return 'warning'
  return 'danger'
}
</script>

<style scoped>
.job-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.job-detail-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.job-detail-page__header {
  display: flex;
  align-items: center;
}

.job-detail-page__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.job-detail-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.job-detail-page__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--nb-muted);
  font-size: 15px;
}

.job-detail-page__company-link {
  color: var(--nb-primary);
  cursor: pointer;
  font-weight: 500;
}

.job-detail-page__company-link:hover {
  text-decoration: underline;
}

.job-detail-page__meta-divider {
  color: var(--nb-border);
}

.job-detail-page__salary {
  color: var(--nb-accent);
  font-weight: 600;
}

.job-detail-page__hero-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.job-detail-page__rec-badge {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
  font-weight: 600;
}

.job-detail-page__section {
  margin-top: 4px;
}

.job-detail-page__section-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
}

.job-detail-page__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.job-detail-page__tech-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.job-detail-page__content {
  color: var(--nb-text);
  line-height: 1.8;
  white-space: pre-wrap;
}

.job-detail-page__match-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}

.job-detail-page__match-scores {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.job-detail-page__score-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-bg);
}

.job-detail-page__score-label {
  font-size: 13px;
  color: var(--nb-muted);
  margin-bottom: 4px;
}

.job-detail-page__score-value {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  color: var(--nb-primary);
}

.job-detail-page__match-detail {
  padding: 12px 0;
  color: var(--nb-text);
  line-height: 1.6;
}

.job-detail-page__match-detail p {
  margin: 0;
}

.job-detail-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}
</style>
