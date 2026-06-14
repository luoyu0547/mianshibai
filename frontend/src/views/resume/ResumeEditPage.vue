<!-- src/views/resume/ResumeEditPage.vue -->
<template>
  <div class="rep">
    <!-- 顶部工具栏 -->
    <header class="rep-topbar">
      <div class="rep-topbar__left">
        <NbButton variant="ghost" class="rep-back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </NbButton>
        <button
          class="rep-module-toggle"
          :title="moduleRailCollapsed ? '展开模块栏' : '收起模块栏'"
          @click="moduleRailCollapsed = !moduleRailCollapsed"
        >
          <el-icon><component :is="moduleRailCollapsed ? Expand : Fold" /></el-icon>
        </button>
        <el-input
          v-model="title"
          class="rep-title-input"
          placeholder="简历标题"
          size="large"
        />
      </div>
      <div class="rep-topbar__right">
        <TemplateSelector v-model="templateType" />
        <NbButton variant="ghost" size="small" @click="toggleAiPanel('chat')">
          <el-icon><ChatDotSquare /></el-icon> AI 对话
        </NbButton>
        <NbButton variant="ghost" size="small" @click="toggleAiPanel('score')">
          <el-icon><DataAnalysis /></el-icon> AI 评分
        </NbButton>
        <NbButton variant="ghost" @click="router.push(`/resume/${resumeId}/preview`)">预览</NbButton>
        <NbButton variant="primary" :loading="isSaving" @click="handleSave">保存</NbButton>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="rep-body">
      <aside class="rep-module-rail" :class="{ 'is-collapsed': moduleRailCollapsed }">
        <div v-if="!moduleRailCollapsed" class="rep-module-rail__title">模块选择</div>
        <a
          v-for="section in moduleSections"
          :key="section.type"
          class="rep-module-item"
          :href="`#resume-section-${section.type}`"
          :title="moduleRailCollapsed ? section.label : ''"
        >
          <span class="rep-module-item__short">{{ section.short }}</span>
          <span v-if="!moduleRailCollapsed">{{ section.label }}</span>
          <i v-if="!moduleRailCollapsed" :class="hasSectionContent(section.type) ? 'is-on' : ''"></i>
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
            <el-select v-model="templateType" size="small" class="rep-tool-select" placeholder="基础布局">
              <el-option label="ATS 精英" value="minimal_tech" />
              <el-option label="技术双栏" value="modern_two_col" />
              <el-option label="经典正式" value="classic_formal" />
            </el-select>
            <NbButton variant="ghost" size="small" @click="wholeOptimizeRef?.open()">智能纠错</NbButton>
            <el-select v-model="previewFontSize" size="small" class="rep-tool-select rep-tool-select--sm">
              <el-option v-for="n in fontSizes" :key="n" :label="'字号 ' + n" :value="n" />
            </el-select>
            <el-select v-model="previewLineHeight" size="small" class="rep-tool-select rep-tool-select--sm">
              <el-option v-for="lh in lineHeights" :key="lh" :label="'行距 ' + lh" :value="lh" />
            </el-select>
            <el-color-picker v-model="previewAccentColor" size="small" show-alpha />
            <el-select v-model="previewSpacing" size="small" class="rep-tool-select rep-tool-select--sm">
              <el-option label="紧凑" value="compact" />
              <el-option label="标准" value="normal" />
              <el-option label="宽松" value="relaxed" />
            </el-select>
          </div>
          <div class="rep-a4-wrap">
            <div class="rep-a4" :style="previewA4Style">
              <component
                :is="templateComponent"
                v-if="templateComponent"
                :basic="basicData"
                :education="educationItems"
                :work="workItems"
                :project="projectItems"
                :skills="skillsData"
                :summary="summaryData"
                :accent-color="previewAccentColor"
              />
              <div v-else class="rep-a4-empty">
                <p>简历预览区域</p>
                <p class="rep-a4-empty__hint">选择模板后在此显示预览</p>
              </div>
            </div>
          </div>
        </div>
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

      <transition name="rep-ai-slide">
        <div v-if="aiPanelOpen" class="rep-ai-overlay">
          <div class="rep-ai-overlay__header">
            <span>{{ aiPanelMode === 'chat' ? 'AI 对话' : 'AI 评分' }}</span>
            <NbButton variant="ghost" size="small" @click="aiPanelOpen = false">
              <el-icon><Close /></el-icon>
            </NbButton>
          </div>
          <div class="rep-ai-overlay__body">
            <AiChatPanel
              v-if="aiPanelMode === 'chat'"
              :resume-id="resumeId"
              :section-data-map="sectionDataMap"
              @extracted="handleExtracted"
              @proposal="handlePatchProposal"
            />
            <AiScorePanel
              v-else
              :resume-id="resumeId"
            />
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Expand, Fold, ChatDotSquare, DataAnalysis, Close } from '@element-plus/icons-vue'
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
  saveVersion,
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

const wholeOptimizeRef = ref<InstanceType<typeof WholeResumeOptimizeDialog> | null>(null)

const moduleRailCollapsed = ref(false)
const aiPanelOpen = ref(false)
const aiPanelMode = ref<'chat' | 'score'>('chat')

function toggleAiPanel(mode: 'chat' | 'score') {
  if (aiPanelOpen.value && aiPanelMode.value === mode) {
    aiPanelOpen.value = false
  } else {
    aiPanelMode.value = mode
    aiPanelOpen.value = true
  }
}

const previewFontSize = ref(14)
const previewLineHeight = ref(1.7)
const previewAccentColor = ref('#3F6DF6')
const previewSpacing = ref('normal')
const fontSizes = [12, 13, 14, 15, 16]
const lineHeights = [1.4, 1.5, 1.6, 1.7, 1.8, 2.0]
const spacingScale: Record<string, number> = { compact: 0.85, normal: 1, relaxed: 1.2 }
const previewA4Style = computed(() => ({
  fontSize: previewFontSize.value + 'px',
  lineHeight: previewLineHeight.value,
  '--nb-accent': previewAccentColor.value,
  '--nb-spacing-mult': spacingScale[previewSpacing.value] ?? 1,
}))

// unused — kept for reference, will clean up with ResumePatchConfirmDialog
const patchConfirmVisible = ref(false)
const pendingPatchProposal = ref<ResumePatchProposal | null>(null)

const optimizeVisible = ref(false)
const optimizeSectionType = ref<SectionType>('basic')
const optimizeSectionData = ref<Record<string, unknown> | Record<string, unknown>[]>({})
const optimizeSectionLabel = ref('')

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

const moduleSections: { type: SectionType; label: string; short: string }[] = [
  { type: 'basic', label: '基本信息', short: '基' },
  { type: 'education', label: '教育经历', short: '教' },
  { type: 'skills', label: '专业技能', short: '技' },
  { type: 'work', label: '工作经历', short: '工' },
  { type: 'project', label: '项目经历', short: '项' },
  { type: 'summary', label: '个人简介', short: '简' },
]

const sectionDataMap = computed<Record<string, Record<string, unknown> | Record<string, unknown>[]>>(() => ({
  basic: basicData.value,
  education: educationItems.value,
  work: workItems.value,
  project: projectItems.value,
  skills: skillsData.value,
  summary: summaryData.value,
}))

const pendingPatchCurrentData = computed<Record<string, unknown> | Record<string, unknown>[]>(() => {
  const type = pendingPatchProposal.value?.sectionType
  if (!type) return {}
  return sectionDataMap.value[type] ?? {}
})

function openOptimize(type: SectionType) {
  optimizeSectionType.value = type
  optimizeSectionLabel.value = sectionLabelMap[type]
  optimizeSectionData.value = sectionDataMap.value[type] ?? {}
  optimizeVisible.value = true
}

function handleOptimizeApplied(type: SectionType, data: Record<string, unknown> | Record<string, unknown>[]) {
  switch (type) {
    case 'basic': {
      const obj = Array.isArray(data) ? Object.assign({}, ...data) : data as Record<string, unknown>
      basicData.value = { ...basicData.value, ...obj, avatar: obj.avatar ?? basicData.value.avatar }
      break
    }
    case 'education': {
      const optimized = Array.isArray(data) ? data as Record<string, unknown>[] : [data as Record<string, unknown>]
      educationItems.value = educationItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'work': {
      const optimized = Array.isArray(data) ? data as Record<string, unknown>[] : [data as Record<string, unknown>]
      workItems.value = workItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'project': {
      const optimized = Array.isArray(data) ? data as Record<string, unknown>[] : [data as Record<string, unknown>]
      projectItems.value = projectItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'skills': {
      const obj = Array.isArray(data) ? Object.assign({}, ...data) : data as Record<string, unknown>
      skillsData.value = { ...skillsData.value, ...obj }
      break
    }
    case 'summary': {
      const obj = Array.isArray(data) ? Object.assign({}, ...data) : data as Record<string, unknown>
      summaryData.value = { ...summaryData.value, ...obj }
      break
    }
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
    if (section.id != null) {
      const ids = sectionIds.value[type as SectionType] || []
      if (!ids.includes(section.id)) {
        sectionIds.value[type as SectionType] = [...ids, section.id]
      }
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
    case 'basic': {
      const obj = Array.isArray(sectionData) ? Object.assign({}, ...sectionData) : sectionData
      basicData.value = { ...basicData.value, ...obj, avatar: obj.avatar ?? basicData.value.avatar }
      break
    }
    case 'education': {
      const optimized = Array.isArray(sectionData) ? sectionData : [sectionData]
      educationItems.value = educationItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'work': {
      const optimized = Array.isArray(sectionData) ? sectionData : [sectionData]
      workItems.value = workItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'project': {
      const optimized = Array.isArray(sectionData) ? sectionData : [sectionData]
      projectItems.value = projectItems.value.map((orig, i) => ({ ...orig, ...(optimized[i] ?? {}) }))
      break
    }
    case 'skills': {
      const obj = Array.isArray(sectionData) ? Object.assign({}, ...sectionData) : sectionData
      skillsData.value = { ...skillsData.value, ...obj }
      break
    }
    case 'summary': {
      const obj = Array.isArray(sectionData) ? Object.assign({}, ...sectionData) : sectionData
      summaryData.value = { ...summaryData.value, ...obj }
      break
    }
  }
  ElMessage.success(`已更新${sectionType}内容`)
}

function handlePatchProposal(proposal: ResumePatchProposal) {
  if (proposal.operation !== 'replace_section') {
    ElMessage.warning('暂不支持该 AI 修改类型')
    return
  }
  handlePatchProposalApplied(proposal)
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
          const id = ids[i]
          if (id == null) continue
          const sectionData = i < data.length ? data[i] : data[0]
          await updateSectionApi(resumeId.value, id, { sectionData })
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
    try {
      await saveVersion(resumeId.value)
    } catch {
      // 版本保存失败不影响主流程
    }
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
  margin: 0;
  background: var(--nb-bg);
}

.rep-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 24px;
  background: var(--nb-surface);
  border-bottom: var(--nb-border);
  flex-shrink: 0;
  z-index: 20;
}

.rep-topbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.rep-back { flex-shrink: 0; }

.rep-module-toggle {
  flex-shrink: 0;
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border: var(--nb-border); border-radius: var(--nb-radius);
  background: var(--nb-surface); color: var(--nb-ink);
  cursor: pointer; font-size: 16px;
  transition: var(--nb-transition);
}
.rep-module-toggle:hover { background: var(--nb-primary-light); color: var(--nb-primary); }

.rep-title-input { flex: 1; max-width: 360px; }
.rep-title-input :deep(.el-input__wrapper) {
  box-shadow: none !important; border: none !important;
  background: transparent !important; font-family: var(--font-heading);
  font-size: 18px; font-weight: 600; padding: 0 !important;
}

.rep-topbar__right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.rep-body { display: flex; flex: 1; overflow: hidden; position: relative; }

/* Module rail */
.rep-module-rail {
  width: 220px; flex: 0 0 220px;
  padding: 18px 12px; background: #fff;
  border-right: var(--nb-border); overflow-y: auto;
  transition: width 0.2s ease, flex-basis 0.2s ease, padding 0.2s ease;
}
.rep-module-rail.is-collapsed { width: 52px; flex: 0 0 52px; padding: 12px 6px; }

.rep-module-rail__title {
  margin: 0 10px 14px; font-family: var(--font-heading);
  font-size: 15px; font-weight: 800; color: var(--nb-ink); white-space: nowrap;
}

.rep-module-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 10px; border-bottom: 1px solid var(--nb-border-color-light);
  color: var(--nb-ink); font-weight: 600; text-decoration: none;
  transition: var(--nb-transition);
}
.rep-module-rail.is-collapsed .rep-module-item {
  justify-content: center; padding: 10px 4px;
  border-bottom: none; border-radius: var(--nb-radius);
}
.rep-module-item:hover { color: var(--nb-primary); background: var(--nb-primary-light); border-radius: var(--nb-radius); }

.rep-module-item__short {
  flex-shrink: 0; width: 28px; height: 28px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--nb-radius); background: var(--nb-bg);
  font-size: 13px; font-family: var(--font-heading);
}
.rep-module-rail.is-collapsed .rep-module-item__short {
  background: var(--nb-primary-light); color: var(--nb-primary);
}

.rep-module-item i {
  width: 34px; height: 20px; border-radius: 999px;
  background: #d1d5db; position: relative; flex-shrink: 0; margin-left: auto;
}
.rep-module-item i::after {
  content: ''; position: absolute; top: 3px; left: 3px;
  width: 14px; height: 14px; border-radius: 50%;
  background: #fff; transition: var(--nb-transition);
}
.rep-module-item i.is-on { background: var(--nb-primary); }
.rep-module-item i.is-on::after { left: 17px; }
.rep-module-rail .nb-button { margin-top: 18px; }

/* Forms */
.rep-forms {
  flex: 1 1 45%;
  min-width: 380px;
  overflow-y: auto;
  padding: 20px;
  border-right: var(--nb-border);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.rep-section { display: flex; flex-direction: column; gap: 14px; }

.rep-ai-bar__inner { display: flex; align-items: center; gap: 16px; }
.rep-ai-bar__text { flex-shrink: 0; }
.rep-ai-bar__text h4 { margin: 0; font-family: var(--font-heading); font-size: 14px; font-weight: 700; color: var(--nb-primary-dark); }
.rep-ai-bar__text p { margin: 2px 0 0; font-size: 12.5px; color: var(--nb-primary); }

/* Aside */
.rep-aside {
  flex: 1 1 55%;
  min-width: 480px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--nb-bg);
}

.rep-preview {
  flex: 1; min-height: 0; overflow: auto;
  padding: 20px; display: flex; flex-direction: column; gap: 14px;
}
.rep-preview-title { background: var(--nb-surface); margin: -4px -4px 0; padding: 4px 8px; border-radius: var(--nb-radius); }

.rep-preview-tools {
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
  padding: 8px 10px; background: #fff;
  border: var(--nb-border); border-radius: var(--nb-radius); box-shadow: var(--nb-shadow-xs);
}
.rep-tool-select { width: 108px; }
.rep-tool-select--sm { width: 88px; }

.rep-a4-wrap { width: 100%; max-width: 680px; margin: 0 auto; }
.rep-a4 {
  background: #fff; border: var(--nb-border);
  box-shadow: var(--nb-shadow-lg); border-radius: 4px;
  padding: 44px 36px; min-height: 600px;
  font-size: 13px; line-height: 1.6;
}
.rep-a4-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; color: var(--nb-muted); font-size: 15px; font-family: var(--font-body); }
.rep-a4-empty__hint { font-size: 13px; margin-top: 8px; color: var(--nb-muted-light); }

/* AI overlay */
.rep-ai-overlay {
  position: absolute; top: 0; right: 0; bottom: 0;
  width: 420px; background: var(--nb-surface);
  border-left: var(--nb-border); box-shadow: var(--nb-shadow-lg);
  display: flex; flex-direction: column; z-index: 30;
}
.rep-ai-overlay__header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 18px; border-bottom: var(--nb-border);
  font-family: var(--font-heading); font-size: 15px; font-weight: 700;
  flex-shrink: 0;
}
.rep-ai-overlay__body { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.rep-ai-slide-enter-active,
.rep-ai-slide-leave-active { transition: transform 0.25s ease, opacity 0.25s ease; }
.rep-ai-slide-enter-from,
.rep-ai-slide-leave-to { transform: translateX(100%); opacity: 0; }

/* Responsive */
@media (max-width: 960px) {
  .rep { height: auto; min-height: calc(100vh - 60px); }
  .rep-body { flex-direction: column; overflow: visible; }
  .rep-module-rail { width: 100%; flex: none; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; border-right: none; border-bottom: var(--nb-border); }
  .rep-module-rail__title { grid-column: 1 / -1; }
  .rep-forms { width: 100%; border-right: none; border-bottom: var(--nb-border); overflow: visible; }
  .rep-aside { width: 100%; min-width: 0; flex: 1 1 auto; }
  .rep-ai-overlay { width: 100%; position: static; border-left: none; border-top: var(--nb-border); }
}
</style>
