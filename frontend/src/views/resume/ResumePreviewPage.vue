<!-- src/views/resume/ResumePreviewPage.vue -->
<template>
  <MainLayout>
    <div class="resume-preview-page">
      <header class="resume-preview-page__toolbar">
        <div class="resume-preview-page__toolbar-left">
          <NbButton variant="ghost" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回编辑
          </NbButton>
        </div>
        <div class="resume-preview-page__toolbar-right">
          <el-select v-model="previewFontSize" size="small" class="pp-tool-select pp-tool-select--sm">
            <el-option v-for="n in fontSizes" :key="n" :label="'字号 ' + n" :value="n" />
          </el-select>
          <el-select v-model="previewLineHeight" size="small" class="pp-tool-select pp-tool-select--sm">
            <el-option v-for="lh in lineHeights" :key="lh" :label="'行距 ' + lh" :value="lh" />
          </el-select>
          <el-color-picker v-model="previewAccentColor" size="small" show-alpha />
          <el-select v-model="previewSpacing" size="small" class="pp-tool-select pp-tool-select--sm">
            <el-option label="紧凑" value="compact" />
            <el-option label="标准" value="normal" />
            <el-option label="宽松" value="relaxed" />
          </el-select>
          <TemplateSelector v-model="templateType" />
          <NbButton variant="primary" :loading="exporting" @click="exportPdf">
            <el-icon><Download /></el-icon>
            导出 PDF
          </NbButton>
        </div>
      </header>

      <div class="resume-preview-page__content">
        <div class="resume-preview-page__a4-wrapper">
          <div ref="previewContentRef" class="resume-preview-page__a4" :style="previewA4Style">
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
            <div v-else class="resume-preview-page__placeholder">
              <p>简历预览区域</p>
              <p class="resume-preview-page__placeholder-hint">选择模板后在此显示预览</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'
import MainLayout from '@/layouts/MainLayout.vue'
import TemplateSelector from '@/components/resume/TemplateSelector.vue'
import MinimalTech from '@/templates/MinimalTech.vue'
import ModernTwoCol from '@/templates/ModernTwoCol.vue'
import ClassicFormal from '@/templates/ClassicFormal.vue'
import NbButton from '@/components/NbButton.vue'
import { useResumeStore } from '@/stores/resume'
import type { SectionVO } from '@/types/resume'

const route = useRoute()
const router = useRouter()
const resumeStore = useResumeStore()

const resumeId = computed(() => Number(route.params.id) || 0)
const templateType = ref('minimal_tech')
const exporting = ref(false)
const previewContentRef = ref<HTMLElement>()

const basicData = ref<Record<string, unknown>>({})
const educationItems = ref<Record<string, unknown>[]>([])
const workItems = ref<Record<string, unknown>[]>([])
const projectItems = ref<Record<string, unknown>[]>([])
const skillsData = ref<Record<string, unknown>>({ categories: [] })
const summaryData = ref<Record<string, unknown>>({})

const previewFontSize = ref(14)
const previewLineHeight = ref(1.7)
const previewAccentColor = ref('#3F6DF6')
const previewSpacing = ref('normal')
const fontSizes = [12, 13, 14, 15, 16]
const lineHeights = [1.4, 1.5, 1.6, 1.7, 1.8, 2.0]
const spacingScale: Record<string, number> = { compact: 0.85, normal: 1, relaxed: 1.2 }
const previewA4Style = computed(() => ({
  '--rs-font-size': previewFontSize.value + 'px',
  lineHeight: previewLineHeight.value,
  '--rs-line-height': previewLineHeight.value,
  '--rs-accent': previewAccentColor.value,
  '--rs-spacing-mult': spacingScale[previewSpacing.value] ?? 1,
  '--rs-text-color': '#111827',
  '--rs-muted-color': '#4b5563',
  '--rs-muted-light': '#9ca3af',
  '--rs-border-color': '#dbe3ef',
  '--rs-border-light': '#f3f4f6',
}))

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
      templateType.value = detail.templateType || 'minimal_tech'
      splitSections(detail.sections)
      if (detail.styleSettings) {
        const s = detail.styleSettings
        if (s.fontSize) previewFontSize.value = s.fontSize
        if (s.lineHeight) previewLineHeight.value = s.lineHeight
        if (s.accentColor) previewAccentColor.value = s.accentColor
        if (s.spacing) previewSpacing.value = s.spacing
      }
    }
  }
})

function splitSections(sections: SectionVO[]) {
  const grouped: Record<string, Record<string, unknown>[]> = {}
  for (const section of sections) {
    const type = section.sectionType
    if (!grouped[type]) grouped[type] = []
    grouped[type].push(section.sectionData)
  }

  basicData.value = grouped.basic?.[0] || {}
  educationItems.value = grouped.education || []
  workItems.value = grouped.work || []
  projectItems.value = grouped.project || []
  skillsData.value = grouped.skills?.[0] || { categories: [] }
  summaryData.value = grouped.summary?.[0] || {}
}

async function exportPdf() {
  const el = previewContentRef.value
  if (!el) return

  exporting.value = true
  try {
    const canvas = await html2canvas(el, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    })
    const imgData = canvas.toDataURL('image/png')
    const pdf = new jsPDF('p', 'mm', 'a4')
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = pdf.internal.pageSize.getHeight()
    const imgHeight = (canvas.height * pdfWidth) / canvas.width
    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, imgHeight)
    heightLeft -= pdfHeight

    while (heightLeft > 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, imgHeight)
      heightLeft -= pdfHeight
    }
    pdf.save(`${resumeStore.currentResume?.title || 'resume'}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch {
    ElMessage.error('PDF 导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.resume-preview-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 60px);
}

.resume-preview-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 24px;
  background: var(--nb-surface);
  border-bottom: var(--nb-border);
  box-shadow: var(--nb-shadow-sm);
  position: sticky;
  top: 60px;
  z-index: 10;
  margin: -28px -24px 0;
}

.resume-preview-page__toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resume-preview-page__toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pp-tool-select { width: auto; }
.pp-tool-select--sm { width: 96px; }

.resume-preview-page__content {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 32px 24px;
  background: var(--nb-bg);
}

.resume-preview-page__a4-wrapper {
  width: 210mm;
  min-height: 297mm;
}

.resume-preview-page__a4 {
  background: #fff;
  box-shadow: var(--nb-shadow-lg);
  border: var(--nb-border);
  padding: 40px 32px;
  min-height: 297mm;
  font-size: 14px;
  line-height: 1.7;
}

.resume-preview-page__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: var(--nb-muted);
  font-size: 16px;
}

.resume-preview-page__placeholder-hint {
  font-size: 13px;
  margin-top: 8px;
  color: var(--nb-muted);
}
</style>
