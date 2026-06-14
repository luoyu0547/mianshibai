<!-- src/components/resume/TemplateSelector.vue -->
<template>
  <el-select
    :model-value="modelValue"
    placeholder="选择模板"
    style="width: 200px;"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-option label="ATS 精英单栏" value="minimal_tech" />
    <el-option label="技术品牌双栏" value="modern_two_col" />
    <el-option label="长篇正式简历" value="classic_formal" />
  </el-select>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>
