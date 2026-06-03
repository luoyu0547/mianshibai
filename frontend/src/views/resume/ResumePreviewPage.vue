<!-- src/views/resume/ResumePreviewPage.vue -->
<template>
  <MainLayout>
    <div class="resume-preview-page">
      <div class="resume-preview-page__toolbar">
        <el-button @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回编辑
        </el-button>
        <TemplateSelector v-model="templateType" />
        <el-button type="primary" :loading="exporting" @click="exportPdf">
          <el-icon><Download /></el-icon>
          导出 PDF
        </el-button>
      </div>

      <div class="resume-preview-page__content">
        <div class="resume-preview-page__a4-wrapper">
          <div ref="previewContentRef" class="resume-preview-page__a4">
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
    const imgWidth = canvas.width
    const imgHeight = canvas.height
    const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight)
    const imgX = (pdfWidth - imgWidth * ratio) / 2
    pdf.addImage(imgData, 'PNG', imgX, 0, imgWidth * ratio, imgHeight * ratio)
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
  min-height: calc(100vh - 64px);
}

.resume-preview-page__toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  box-shadow: var(--nb-shadow);
  position: sticky;
  top: 64px;
  z-index: 10;
  margin: -32px -24px 0;
}

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
  box-shadow: var(--nb-shadow);
  border: var(--nb-border);
  padding: 40px 32px;
  min-height: 297mm;
  font-size: 12px;
  line-height: 1.6;
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
