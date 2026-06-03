<!-- src/components/resume/sections/BasicInfoEditor.vue -->
<template>
  <el-form label-position="top" class="basic-info-editor">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="姓名">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="目标岗位">
          <el-input v-model="formData.targetPosition" placeholder="例如：Java 开发工程师" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="城市">
          <el-input v-model="formData.city" placeholder="例如：北京" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="GitHub">
          <el-input v-model="formData.github" placeholder="GitHub 主页链接" />
        </el-form-item>
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="博客">
          <el-input v-model="formData.blog" placeholder="个人博客链接" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
}>()

const formData = computed({
  get: () => props.modelValue,
  set: (val: Record<string, unknown>) => emit('update:modelValue', val),
})
</script>

<style scoped>
.basic-info-editor :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
}
</style>
