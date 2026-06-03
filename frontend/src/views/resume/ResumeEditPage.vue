<!-- src/views/resume/ResumeEditPage.vue -->
<template>
  <div class="resume-edit-page">
    <div class="resume-edit-page__topbar">
      <el-input
        v-model="title"
        class="resume-edit-page__title-input"
        placeholder="简历标题"
        size="large"
      />
      <TemplateSelector v-model="templateType" />
      <div class="resume-edit-page__topbar-actions">
        <el-button :loading="isSaving" type="primary" @click="handleSave">保存</el-button>
        <el-button @click="router.push(`/resume/${resumeId}/preview`)">预览</el-button>
      </div>
    </div>

    <div class="resume-edit-page__body">
      <div class="resume-edit-page__left">
        <el-collapse v-model="activeSections" class="section-collapse">
          <el-collapse-item name="basic">
            <template #title>
              <div class="section-header">
                <span>基本信息</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <BasicInfoEditor v-model="basicData" />
          </el-collapse-item>

          <el-collapse-item name="education">
            <template #title>
              <div class="section-header">
                <span>教育经历</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <EducationEditor :items="educationItems" @update:items="educationItems = $event" />
          </el-collapse-item>

          <el-collapse-item name="work">
            <template #title>
              <div class="section-header">
                <span>工作经历</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <WorkExperienceEditor :items="workItems" @update:items="workItems = $event" />
          </el-collapse-item>

          <el-collapse-item name="project">
            <template #title>
              <div class="section-header">
                <span>项目经历</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <ProjectEditor :items="projectItems" @update:items="projectItems = $event" />
          </el-collapse-item>

          <el-collapse-item name="skills">
            <template #title>
              <div class="section-header">
                <span>技能标签</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <SkillsEditor v-model="skillsData" />
          </el-collapse-item>

          <el-collapse-item name="summary">
            <template #title>
              <div class="section-header">
                <span>自我评价</span>
                <el-button size="small" text class="ai-btn" @click.stop>AI 优化</el-button>
              </div>
            </template>
            <SummaryEditor v-model="summaryData" />
          </el-collapse-item>
        </el-collapse>

        <div class="resume-edit-page__ai-bar">
          <el-button type="primary" class="ai-generate-btn">一键 AI 生成</el-button>
        </div>
      </div>

      <div class="resume-edit-page__right">
        <div class="resume-edit-page__preview">
          <div class="a4-container">
            <div class="a4-page">
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
              <div v-else class="a4-placeholder">
                <p>简历预览区域</p>
                <p class="a4-placeholder__hint">选择模板后在此显示预览</p>
              </div>
            </div>
          </div>
        </div>
        <div class="resume-edit-page__chat-panel">
          <el-tabs v-model="activeTab" class="chat-tabs">
            <el-tab-pane label="AI 对话" name="chat">
              <AiChatPanel
                :resume-id="resumeId"
                @extracted="handleExtracted"
              />
            </el-tab-pane>
            <el-tab-pane label="AI 评分" name="score">
              <AiScorePanel :resume-id="resumeId" />
            </el-tab-pane>
          </el-tabs>
        </div>
        <div class="resume-edit-page__version-bar">
          <VersionHistory :resume-id="resumeId" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BasicInfoEditor from '@/components/resume/sections/BasicInfoEditor.vue'
import EducationEditor from '@/components/resume/sections/EducationEditor.vue'
import WorkExperienceEditor from '@/components/resume/sections/WorkExperienceEditor.vue'
import ProjectEditor from '@/components/resume/sections/ProjectEditor.vue'
import SkillsEditor from '@/components/resume/sections/SkillsEditor.vue'
import SummaryEditor from '@/components/resume/sections/SummaryEditor.vue'
import AiChatPanel from '@/components/resume/AiChatPanel.vue'
import AiScorePanel from '@/components/resume/AiScorePanel.vue'
import VersionHistory from '@/components/resume/VersionHistory.vue'
import TemplateSelector from '@/components/resume/TemplateSelector.vue'
import MinimalTech from '@/templates/MinimalTech.vue'
import ModernTwoCol from '@/templates/ModernTwoCol.vue'
import ClassicFormal from '@/templates/ClassicFormal.vue'
import { useResumeStore } from '@/stores/resume'
import {
  addSection as addSectionApi,
  updateSection as updateSectionApi,
} from '@/api/resume'
import type { SectionVO, SectionType } from '@/types/resume'

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

const activeSections = ref(['basic'])
const activeTab = ref('chat')

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

watch(basicData, () => { }, { deep: true })

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
.resume-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  margin: -32px -24px;
}

.resume-edit-page__topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  flex-shrink: 0;
}

.resume-edit-page__title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border: none !important;
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
}

.resume-edit-page__topbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.resume-edit-page__body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.resume-edit-page__left {
  width: 50%;
  overflow-y: auto;
  padding: 24px;
  border-right: var(--nb-border);
}

.section-collapse :deep(.el-collapse-item__header) {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 16px;
}

.ai-btn {
  color: var(--nb-primary);
  font-size: 12px;
}

.resume-edit-page__ai-bar {
  margin-top: 16px;
  padding-top: 16px;
  border-top: var(--nb-border);
}

.ai-generate-btn {
  width: 100%;
}

.resume-edit-page__right {
  width: 50%;
  display: flex;
  flex-direction: column;
}

.resume-edit-page__preview {
  flex: 6;
  overflow: auto;
  padding: 24px;
  background: var(--nb-bg);
  display: flex;
  justify-content: center;
}

.a4-container {
  width: 100%;
  max-width: 680px;
}

.a4-page {
  background: #fff;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: 2px;
  padding: 40px 32px;
  min-height: 600px;
  font-size: 12px;
  line-height: 1.6;
}

.a4-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--nb-muted);
  font-size: 16px;
}

.a4-placeholder__hint {
  font-size: 13px;
  margin-top: 8px;
  color: var(--nb-muted);
}

.resume-edit-page__chat-panel {
  flex: 4;
  border-top: var(--nb-border);
  background: var(--nb-card);
  overflow: hidden;
}

.chat-tabs :deep(.el-tabs__content) {
  height: calc(100% - 40px);
  overflow: hidden;
}

.chat-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.resume-edit-page__version-bar {
  padding: 8px 12px;
  border-top: var(--nb-border);
  background: var(--nb-card);
}

.chat-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--nb-muted);
  font-size: 14px;
}
</style>
