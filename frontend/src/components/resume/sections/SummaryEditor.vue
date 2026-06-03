<!-- src/components/resume/sections/SummaryEditor.vue -->
<template>
  <div class="summary-editor">
    <el-input
      :model-value="(modelValue.content as string) || ''"
      type="textarea"
      :rows="5"
      placeholder="请输入自我评价..."
      @update:model-value="updateContent"
    />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
}>()

function updateContent(val: string) {
  emit('update:modelValue', { ...props.modelValue, content: val })
}
</script>

<style scoped>
.summary-editor :deep(.el-textarea__inner) {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius);
}
</style>
