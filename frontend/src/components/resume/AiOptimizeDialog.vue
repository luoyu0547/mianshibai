<!-- src/components/resume/AiOptimizeDialog.vue -->
<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="'AI 优化 — ' + sectionLabel"
    width="860px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div v-if="loading" class="optimize-dialog__loading">
      <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      <span>AI 正在优化中...</span>
    </div>

    <div v-else-if="error" class="optimize-dialog__error">
      <p>{{ error }}</p>
      <el-button type="primary" @click="doOptimize">重试</el-button>
    </div>

    <div v-else class="optimize-dialog__compare">
      <div class="optimize-dialog__side">
        <h4 class="optimize-dialog__side-title">原始内容</h4>
        <div class="optimize-dialog__content optimize-dialog__content--readonly">
          <pre>{{ formatData(sectionData) }}</pre>
        </div>
      </div>
      <div class="optimize-dialog__divider">
        <el-icon :size="20"><Right /></el-icon>
      </div>
      <div class="optimize-dialog__side">
        <h4 class="optimize-dialog__side-title optimize-dialog__side-title--optimized">优化后</h4>
        <div class="optimize-dialog__content">
          <pre>{{ formatData(optimizedData) }}</pre>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :disabled="loading || !!error" @click="handleApply">
        应用优化结果
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Loading, Right } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiOptimizeSection } from '@/api/resume'
import type { SectionType } from '@/types/resume'

const props = defineProps<{
  visible: boolean
  resumeId: number
  sectionType: SectionType
  sectionData: Record<string, unknown>
  sectionLabel: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'applied', sectionType: SectionType, data: Record<string, unknown>): void
}>()

const loading = ref(false)
const error = ref('')
const optimizedData = ref<Record<string, unknown>>({})

watch(() => props.visible, (val) => {
  if (val) {
    doOptimize()
  }
})

async function doOptimize() {
  loading.value = true
  error.value = ''
  try {
    const res = await aiOptimizeSection(props.resumeId, {
      sectionId: 0,
      sectionType: props.sectionType,
      sectionData: props.sectionData,
    })
    if (res.data.code === 0) {
      optimizedData.value = res.data.data?.sectionData as Record<string, unknown> ?? {}
    } else {
      error.value = '优化失败，请重试'
    }
  } catch {
    error.value = '网络错误，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function handleApply() {
  emit('applied', props.sectionType, optimizedData.value)
  emit('update:visible', false)
  ElMessage.success('已应用优化结果')
}

function formatData(data: Record<string, unknown>): string {
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}
</script>

<style scoped>
.optimize-dialog__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 0;
  color: var(--nb-muted);
}

.optimize-dialog__error {
  text-align: center;
  padding: 48px 0;
  color: var(--el-color-danger);
}

.optimize-dialog__compare {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.optimize-dialog__side {
  flex: 1;
  min-width: 0;
}

.optimize-dialog__side-title {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-muted);
  margin: 0 0 8px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--nb-border-color, #ddd);
}

.optimize-dialog__side-title--optimized {
  color: var(--nb-primary);
  border-bottom-color: var(--nb-primary);
}

.optimize-dialog__content {
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.optimize-dialog__content pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
}

.optimize-dialog__divider {
  display: flex;
  align-items: center;
  padding-top: 40px;
  color: var(--nb-primary);
  flex-shrink: 0;
}
</style>
