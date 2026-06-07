<script setup lang="ts">
import { ref } from 'vue'
import { optimizeWholeResume } from '@/api/resume'
import type { ResumeWholeOptimizeVO, SectionVO } from '@/types/resume'

const props = defineProps<{
  resumeId: number
  jobId?: number
}>()

const emit = defineEmits<{
  apply: [sections: SectionVO[]]
}>()

const visible = ref(false)
const loading = ref(false)
const result = ref<ResumeWholeOptimizeVO | null>(null)

async function handleOptimize() {
  loading.value = true
  try {
    const res = await optimizeWholeResume(props.resumeId, {
      resumeId: props.resumeId,
      jobId: props.jobId,
    })
    result.value = res.data.data
  } finally {
    loading.value = false
  }
}

function handleApply() {
  if (result.value) {
    emit('apply', result.value.optimizedSections)
    visible.value = false
    result.value = null
  }
}

function open() {
  visible.value = true
  handleOptimize()
}

function close() {
  visible.value = false
  result.value = null
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="整份简历 AI 优化" width="720px" @close="close">
    <div v-if="loading" style="text-align: center; padding: 40px;">
      <p>AI 正在分析您的简历...</p>
    </div>
    <div v-else-if="result">
      <div style="display: flex; gap: 24px; margin-bottom: 16px;">
        <div>
          <p style="font-size: 12px; color: #999;">优化前评分</p>
          <p style="font-size: 28px; font-weight: bold;">{{ result.beforeScore }}</p>
        </div>
        <div>
          <p style="font-size: 12px; color: #999;">预估优化后</p>
          <p style="font-size: 28px; font-weight: bold; color: #6C5CE7;">{{ result.estimatedAfterScore }}</p>
        </div>
      </div>
      <div v-if="result.globalSuggestions.length > 0">
        <p style="font-weight: bold; margin-bottom: 8px;">全局优化建议</p>
        <ul>
          <li v-for="(s, i) in result.globalSuggestions" :key="i">{{ s }}</li>
        </ul>
      </div>
      <div style="margin-top: 16px; text-align: right;">
        <NbButton @click="visible = false">取消</NbButton>
        <NbButton @click="handleApply" style="margin-left: 8px;">应用优化</NbButton>
      </div>
    </div>
  </el-dialog>
</template>
