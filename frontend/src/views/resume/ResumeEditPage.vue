<!-- src/views/resume/ResumeEditPage.vue -->
<template>
  <div class="rep">
    <!-- 顶部工具栏 -->
    <header class="rep-topbar">
      <div class="rep-topbar__left">
        <NbButton variant="ghost" class="rep-back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </NbButton>
        <el-input
          v-model="title"
          class="rep-title-input"
          placeholder="简历标题"
          size="large"
        />
      </div>
      <div class="rep-topbar__right">
        <TemplateSelector v-model="templateType" />
        <NbButton variant="ghost" @click="router.push(`/resume/${resumeId}/preview`)">预览</NbButton>
        <NbButton variant="primary" :loading="isSaving" @click="handleSave">保存</NbButton>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="rep-body">
      <aside class="rep-module-rail">
        <div class="rep-module-rail__title">模块选择</div>
        <a
          v-for="section in moduleSections"
          :key="section.type"
          class="rep-module-item"
          :href="`#resume-section-${section.type}`"
        >
          <span>{{ section.label }}</span>
          <i :class="hasSectionContent(section.type) ? 'is-on' : ''"></i>
        </a>
        <NbButton variant="primary" block @click="wholeOptimizeRef?.open()">整份 AI 优化</NbButton>
      </aside>

      <!-- 左侧编辑区 -->
      <div class="rep-forms">
        <NbCard id="resume-section-basic" class="rep-section">
          <NbSectionTitle title="基本信息" description="个人联系方式与求职意向">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('basic')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <BasicInfoEditor v-model="basicData" />
        </NbCard>

        <NbCard id="resume-section-education" class="rep-section">
          <NbSectionTitle title="教育经历" description="学校、专业与学术亮点">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('education')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <EducationEditor :items="educationItems" @update:items="educationItems = $event" />
        </NbCard>

        <NbCard id="resume-section-work" class="rep-section">
          <NbSectionTitle title="工作经历" description="公司、职位与工作成果">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('work')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <WorkExperienceEditor :items="workItems" @update:items="workItems = $event" />
        </NbCard>

        <NbCard id="resume-section-project" class="rep-section">
          <NbSectionTitle title="项目经历" description="项目名称、技术栈与亮点">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('project')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <ProjectEditor :items="projectItems" @update:items="projectItems = $event" />
        </NbCard>

        <NbCard id="resume-section-skills" class="rep-section">
          <NbSectionTitle title="技能标签" description="按分类组织你的技能">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('skills')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <SkillsEditor v-model="skillsData" />
        </NbCard>

        <NbCard id="resume-section-summary" class="rep-section">
          <NbSectionTitle title="自我评价" description="一段简短的个人亮点">
            <template #actions>
              <NbButton variant="ghost" @click="openOptimize('summary')">AI 优化</NbButton>
            </template>
          </NbSectionTitle>
          <SummaryEditor v-model="summaryData" />
        </NbCard>

        <NbCard variant="ai" class="rep-ai-bar">
          <div class="rep-ai-bar__inner">
            <div class="rep-ai-bar__text">
              <h4>AI 全局优化</h4>
              <p>一键智能优化全部模块</p>
            </div>
            <NbButton variant="primary" block @click="wholeOptimizeRef?.open()">一键 AI 优化</NbButton>
          </div>
        </NbCard>
      </div>

      <!-- 右侧面板 -->
      <div class="rep-aside">
        <div class="rep-preview">
          <NbSectionTitle title="实时预览" description="所见即所得的简历效果" class="rep-preview-title">
            <template #actions>
              <VersionHistory :resume-id="resumeId" />
            </template>
          </NbSectionTitle>
          <div class="rep-preview-tools">
            <span>基础布局</span>
            <span>智能纠错</span>
            <span>字号 14</span>
            <span>行距 1.7</span>
            <span class="rep-preview-tools__color"></span>
            <span>间距配置</span>
          </div>
          <div class="rep-a4-wrap">
            <div class="rep-a4">
              <component
                :is="templateComponent"
                v-if="templateComponent"
                :basic="basicData"
                :education="educationItems"
                :work="workItems"
                :project="projectItems"
                :skills="skillsData"
                :summary="summaryData"
              />
              <div v-else class="rep-a4-empty">
                <p>简历预览区域</p>
                <p class="rep-a4-empty__hint">选择模板后在此显示预览</p>
              </div>
            </div>
          </div>
        </div>

        <NbCard variant="ai" class="rep-ai-panel">
          <el-tabs v-model="activeTab" class="rep-tabs">
            <el-tab-pane label="AI 对话" name="chat">
              <AiChatPanel
                :resume-id="resumeId"
                @extracted="handleExtracted"
              @proposal="handlePatchProposal"
              />
            </el-tab-pane>
            <el-tab-pane label="AI 评分" name="score">
              <AiScorePanel :resume-id="resumeId" />
            </el-tab-pane>
          </el-tabs>
        </NbCard>
      </div>

      <AiOptimizeDialog
        v-model:visible="optimizeVisible"
        :resume-id="resumeId"
        :section-type="optimizeSectionType"
        :section-data="optimizeSectionData"
        :section-label="optimizeSectionLabel"
        @applied="handleOptimizeApplied"
      />

      <WholeResumeOptimizeDialog
        ref="wholeOptimizeRef"
        :resume-id="resumeId"
        @apply="handleWholeOptimizeApplied"
      />

      <ResumePatchConfirmDialog
        v-model:visible="patchConfirmVisible"
        :proposal="pendingPatchProposal"
        :current-data="pendingPatchCurrentData"
        @apply="handlePatchProposalApplied"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import BasicInfoEditor from '@/components/resume/sections/BasicInfoEditor.vue'
import EducationEditor from '@/components/resume/sections/EducationEditor.vue'
import WorkExperienceEditor from '@/components/resume/sections/WorkExperienceEditor.vue'
import ProjectEditor from '@/components/resume/sections/ProjectEditor.vue'
import SkillsEditor from '@/components/resume/sections/SkillsEditor.vue'
import SummaryEditor from '@/components/resume/sections/SummaryEditor.vue'
import AiOptimizeDialog from '@/components/resume/AiOptimizeDialog.vue'
import WholeResumeOptimizeDialog from '@/components/resume/WholeResumeOptimizeDialog.vue'
import AiChatPanel from '@/components/resume/AiChatPanel.vue'
import AiScorePanel from '@/components/resume/AiScorePanel.vue'
import VersionHistory from '@/components/resume/VersionHistory.vue'
import TemplateSelector from '@/components/resume/TemplateSelector.vue'
import ResumePatchConfirmDialog from '@/components/resume/ResumePatchConfirmDialog.vue'
import MinimalTech from '@/templates/MinimalTech.vue'
import ModernTwoCol from '@/templates/ModernTwoCol.vue'
import ClassicFormal from '@/templates/ClassicFormal.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import { useResumeStore } from '@/stores/resume'
import {
  addSection as addSectionApi,
  updateSection as updateSectionApi,
} from '@/api/resume'
import type { SectionVO, SectionType, ResumePatchProposal } from '@/types/resume'

const route = useRoute()
const router = useRouter()
const resumeStore = useResumeStore()

const resumeId = computed(() => Number(route.params.id) || 0)

const title = ref('')
const templateType = ref('minimal_tech')
const isSaving = ref(false)

const basicData = ref<Record<string, unknown>>({})
const educationItems = ref<Record<string, unknown>[]>([])
const workItems = ref<Record<string, unknown>[]>([])
const projectItems = ref<Record<string, unknown>[]>([])
const skillsData = ref<Record<string, unknown>>({ categories: [] })
const summaryData = ref<Record<string, unknown>>({})

const sectionIds = ref<Record<SectionType, number[]>>({
  basic: [],
  education: [],
  work: [],
  project: [],
  skills: [],
  summary: [],
})

const activeTab = ref('chat')
const wholeOptimizeRef = ref<InstanceType<typeof WholeResumeOptimizeDialog> | null>(null)

const patchConfirmVisible = ref(false)
const pendingPatchProposal = ref<ResumePatchProposal | null>(null)

const optimizeVisible = ref(false)
const optimizeSectionType = ref<SectionType>('basic')
const optimizeSectionData = ref<Record<string, unknown>>({})
const optimizeSectionLabel = ref('')

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const moduleSections: { type: SectionType; label: string }[] = [
  { type: 'basic', label: '基本信息' },
  { type: 'education', label: '教育经历' },
  { type: 'skills', label: '专业技能' },
  { type: 'work', label: '工作经历' },
  { type: 'project', label: '项目经历' },
  { type: 'summary', label: '个人简介' },
]

const sectionDataMap = computed(() => ({
  basic: basicData.value,
  education: educationItems.value as unknown as Record<string, unknown>,
  work: workItems.value as unknown as Record<string, unknown>,
  project: projectItems.value as unknown as Record<string, unknown>,
  skills: skillsData.value,
  summary: summaryData.value,
}))

const pendingPatchCurrentData = computed<Record<string, unknown> | Record<string, unknown>[]>(() => {
  const type = pendingPatchProposal.value?.sectionType
  if (!type) return {}
  return sectionDataMap.value[type]
})

function openOptimize(type: SectionType) {
  optimizeSectionType.value = type
  optimizeSectionLabel.value = sectionLabelMap[type]
  optimizeSectionData.value = sectionDataMap.value[type]
  optimizeVisible.value = true
}

function handleOptimizeApplied(type: SectionType, data: Record<string, unknown>) {
  switch (type) {
    case 'basic':
      basicData.value = data
      break
    case 'education':
      educationItems.value = Array.isArray(data) ? data : [data]
      break
    case 'work':
      workItems.value = Array.isArray(data) ? data : [data]
      break
    case 'project':
      projectItems.value = Array.isArray(data) ? data : [data]
      break
    case 'skills':
      skillsData.value = data
      break
    case 'summary':
      summaryData.value = data
      break
  }
}

function handleWholeOptimizeApplied(sections: SectionVO[]) {
  splitSections(sections)
  ElMessage.success('已应用整份简历优化结果，请记得保存')
}

function hasSectionContent(type: SectionType) {
  const value = sectionDataMap.value[type]
  if (Array.isArray(value)) return value.length > 0
  return Object.values(value || {}).some((item) => {
    if (Array.isArray(item)) return item.length > 0
    return item !== undefined && item !== null && item !== ''
  })
}

const templateMap: Record<string, Component> = {
  minimal_tech: MinimalTech,
  modern_two_col: ModernTwoCol,
  classic_formal: ClassicFormal,
}

const templateComponent = computed(() => templateMap[templateType.value])

onMounted(async () => {
  if (resumeId.value) {
    await resumeStore.fetchResumeDetail(resumeId.value)
    const detail = resumeStore.currentResume
    if (detail) {
      title.value = detail.title
      templateType.value = detail.templateType || 'minimal_tech'
      splitSections(detail.sections)
    }
  }
})

function splitSections(sections: SectionVO[]) {
  const grouped: Record<string, Record<string, unknown>[]> = {}
  for (const section of sections) {
    const type = section.sectionType
    if (!grouped[type]) grouped[type] = []
    grouped[type].push(section.sectionData)
    const ids = sectionIds.value[type as SectionType] || []
    if (!ids.includes(section.id)) {
      sectionIds.value[type as SectionType] = [...ids, section.id]
    }
  }

  basicData.value = grouped.basic?.[0] || {}
  educationItems.value = grouped.education || []
  workItems.value = grouped.work || []
  projectItems.value = grouped.project || []
  skillsData.value = grouped.skills?.[0] || { categories: [] }
  summaryData.value = grouped.summary?.[0] || {}
}

function handleExtracted(sectionType: SectionType, sectionData: Record<string, unknown>) {
  switch (sectionType) {
    case 'basic':
      basicData.value = sectionData
      break
    case 'education':
      educationItems.value = Array.isArray(sectionData) ? sectionData : [sectionData]
      break
    case 'work':
      workItems.value = Array.isArray(sectionData) ? sectionData : [sectionData]
      break
    case 'project':
      projectItems.value = Array.isArray(sectionData) ? sectionData : [sectionData]
      break
    case 'skills':
      skillsData.value = sectionData
      break
    case 'summary':
      summaryData.value = sectionData
      break
  }
  ElMessage.success(`已更新${sectionType}内容`)
}

function handlePatchProposal(proposal: ResumePatchProposal) {
  if (proposal.operation !== 'replace_section') {
    ElMessage.warning('暂不支持该 AI 修改类型')
    return
  }
  pendingPatchProposal.value = proposal
  patchConfirmVisible.value = true
}

function handlePatchProposalApplied(proposal: ResumePatchProposal) {
  handleOptimizeApplied(proposal.sectionType, proposal.sectionData)
  ElMessage.success('已应用 AI 修改结果，请检查后保存')
}

async function handleSave() {
  if (!resumeId.value) {
    ElMessage.warning('请先创建简历')
    return
  }

  isSaving.value = true
  try {
    const sectionMap: Record<string, { ids: number[]; data: Record<string, unknown>[] }> = {
      basic: { ids: sectionIds.value.basic, data: [basicData.value] },
      education: { ids: sectionIds.value.education, data: educationItems.value },
      work: { ids: sectionIds.value.work, data: workItems.value },
      project: { ids: sectionIds.value.project, data: projectItems.value },
      skills: { ids: sectionIds.value.skills, data: [skillsData.value] },
      summary: { ids: sectionIds.value.summary, data: [summaryData.value] },
    }

    for (const [type, { ids, data }] of Object.entries(sectionMap)) {
      if (ids.length > 0) {
        for (let i = 0; i < ids.length; i++) {
          const sectionData = i < data.length ? data[i] : data[0]
          await updateSectionApi(resumeId.value, ids[i]!, { sectionData })
        }
      } else {
        const sectionData = data[0]
        if (sectionData && Object.keys(sectionData).length > 0) {
          await addSectionApi(resumeId.value, {
            sectionType: type as SectionType,
            sectionData,
          })
        }
      }
    }

    ElMessage.success('保存成功')
    await resumeStore.fetchResumeDetail(resumeId.value)
    if (resumeStore.currentResume) {
      splitSections(resumeStore.currentResume.sections)
    }
  } catch {
    ElMessage.error('保存失败')
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.rep {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  margin: -28px -24px;
  background: var(--nb-bg);
}

/* 顶部工具栏 */
.rep-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 24px;
  background: var(--nb-surface);
  border-bottom: var(--nb-border);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 20;
}

.rep-topbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.rep-back {
  flex-shrink: 0;
}

.rep-title-input {
  flex: 1;
  max-width: 360px;
}

.rep-title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border: none !important;
  background: transparent !important;
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  padding: 0 !important;
}

.rep-topbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 主体 */
.rep-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.rep-module-rail {
  width: 230px;
  flex: 0 0 230px;
  padding: 18px 12px;
  background: #fff;
  border-right: var(--nb-border);
  overflow-y: auto;
}

.rep-module-rail__title {
  margin: 0 10px 14px;
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 800;
  color: var(--nb-ink);
}

.rep-module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 10px;
  border-bottom: 1px solid var(--nb-border-color-light);
  color: var(--nb-ink);
  font-weight: 600;
  text-decoration: none;
  transition: var(--nb-transition);
}

.rep-module-item:hover {
  color: var(--nb-primary);
  background: var(--nb-primary-light);
  border-radius: var(--nb-radius);
}

.rep-module-item i {
  width: 34px;
  height: 20px;
  border-radius: 999px;
  background: #d1d5db;
  position: relative;
  flex-shrink: 0;
}

.rep-module-item i::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  transition: var(--nb-transition);
}

.rep-module-item i.is-on {
  background: var(--nb-primary);
}

.rep-module-item i.is-on::after {
  left: 17px;
}

.rep-module-rail .nb-button {
  margin-top: 18px;
}

/* 左侧编辑区 */
.rep-forms {
  width: 43%;
  overflow-y: auto;
  padding: 20px;
  border-right: var(--nb-border);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rep-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* AI 工具栏 */
.rep-ai-bar__inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rep-ai-bar__text {
  flex-shrink: 0;
}

.rep-ai-bar__text h4 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--nb-primary-dark);
}

.rep-ai-bar__text p {
  margin: 2px 0 0;
  font-size: 12.5px;
  color: var(--nb-primary);
}

/* 右侧面板 */
.rep-aside {
  width: calc(57% - 230px);
  min-width: 520px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--nb-bg);
}

/* 预览区 */
.rep-preview {
  flex: 1;
  overflow: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rep-preview-title {
  background: var(--nb-surface);
  margin: -4px -4px 0;
  padding: 4px 8px;
  border-radius: var(--nb-radius);
}

.rep-preview-tools {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  background: #fff;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  box-shadow: var(--nb-shadow-xs);
}

.rep-preview-tools span {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--nb-border-color);
  border-radius: 8px;
  color: var(--nb-ink);
  font-size: 12px;
  font-weight: 600;
  background: #fff;
}

.rep-preview-tools__color {
  width: 28px;
  min-width: 28px;
  padding: 0 !important;
  background: #3f6df6 !important;
  border-color: #3f6df6 !important;
}

.rep-a4-wrap {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
}

.rep-a4 {
  background: #fff;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow-lg);
  border-radius: 4px;
  padding: 44px 36px;
  min-height: 600px;
  font-size: 13px;
  line-height: 1.6;
}

.rep-a4-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--nb-muted);
  font-size: 15px;
  font-family: var(--font-body);
}

.rep-a4-empty__hint {
  font-size: 13px;
  margin-top: 8px;
  color: var(--nb-muted-light);
}

/* AI 面板 */
.rep-ai-panel {
  flex: 0 0 38%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: var(--nb-radius-lg) var(--nb-radius-lg) 0 0;
  margin: 0 16px 0 0;
}

.rep-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.rep-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.rep-tabs :deep(.el-tab-pane) {
  height: 100%;
}

/* 响应式 */
@media (max-width: 960px) {
  .rep {
    height: auto;
    min-height: calc(100vh - 60px);
  }

  .rep-body {
    flex-direction: column;
    overflow: visible;
  }

  .rep-module-rail {
    width: 100%;
    flex: none;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    border-right: none;
    border-bottom: var(--nb-border);
  }

  .rep-module-rail__title {
    grid-column: 1 / -1;
  }

  .rep-forms {
    width: 100%;
    border-right: none;
    border-bottom: var(--nb-border);
    overflow: visible;
  }

  .rep-aside {
    width: 100%;
    min-width: 0;
    overflow: visible;
  }

  .rep-ai-panel {
    max-height: none;
    margin: 16px;
    border-radius: var(--nb-radius-lg);
  }
}
</style>
