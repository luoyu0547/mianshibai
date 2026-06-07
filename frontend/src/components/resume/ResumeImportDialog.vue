<script setup lang="ts">
import { ref } from 'vue'
import { importResumePreview } from '@/api/resume'
import type { ResumeImportPreviewVO } from '@/types/resume'

const emit = defineEmits<{
  created: [preview: ResumeImportPreviewVO]
}>()

const visible = ref(false)
const rawText = ref('')
const loading = ref(false)
const preview = ref<ResumeImportPreviewVO | null>(null)

async function handleParse() {
  if (!rawText.value.trim()) return
  loading.value = true
  try {
    const res = await importResumePreview({ rawText: rawText.value, fileType: 'text' })
    preview.value = res.data.data
  } finally {
    loading.value = false
  }
}

function handleConfirm() {
  if (preview.value) {
    emit('created', preview.value)
    visible.value = false
    rawText.value = ''
    preview.value = null
  }
}

function open() {
  visible.value = true
}

function close() {
  visible.value = false
  rawText.value = ''
  preview.value = null
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="导入简历" width="640px" @close="close">
    <div v-if="!preview">
      <el-input
        v-model="rawText"
        type="textarea"
        :rows="10"
        placeholder="请粘贴简历文本内容"
      />
      <div style="margin-top: 16px; text-align: right;">
        <NbButton @click="handleParse" :loading="loading">
          解析预览
        </NbButton>
      </div>
    </div>
    <div v-else>
      <p style="font-weight: bold; margin-bottom: 8px;">解析结果预览</p>
      <p>标题：{{ preview.title }}</p>
      <p>识别到 {{ preview.sections.length }} 个模块</p>
      <div v-if="preview.warnings.length > 0" style="color: #e6a23c;">
        <p v-for="w in preview.warnings" :key="w">{{ w }}</p>
      </div>
      <div style="margin-top: 16px; text-align: right;">
        <NbButton @click="preview = null">返回修改</NbButton>
        <NbButton @click="handleConfirm" style="margin-left: 8px;">确认创建</NbButton>
      </div>
    </div>
  </el-dialog>
</template>
