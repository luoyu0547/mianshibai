<!-- src/components/resume/sections/BasicInfoEditor.vue -->
<template>
  <el-form label-position="top" class="basic-info-editor">
    <div class="basic-info-editor__avatar-row">
      <NbAvatarUploader
        :model-value="String(formData.avatar || '')"
        :fallback-text="String(formData.name || '')"
        @update:model-value="val => { formData.avatar = val }"
      />
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="姓名">
          <el-input v-model="formData.name" placeholder="请输入姓名" />
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
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
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
        <el-form-item label="当前状态">
          <el-input v-model="formData.currentStatus" placeholder="例如：应届毕业生" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="所在城市">
          <el-input v-model="formData.city" placeholder="例如：北京" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="期望工作地">
          <el-input v-model="formData.expectedLocation" placeholder="例如：深圳 / 杭州 / 远程" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="期望薪资">
          <el-input v-model="formData.expectedSalary" placeholder="例如：20k-30k" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="GitHub">
          <el-input v-model="formData.github" placeholder="GitHub 主页链接" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="博客">
          <el-input v-model="formData.blog" placeholder="个人博客链接" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="个人网站">
          <el-input v-model="formData.website" placeholder="作品集 / 个人主页链接" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="微信">
          <el-input v-model="formData.wechat" placeholder="可选" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import NbAvatarUploader from '@/components/NbAvatarUploader.vue'

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
.basic-info-editor {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.basic-info-editor__avatar-row {
  margin-bottom: 12px;
}

.basic-info-editor :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
}
</style>
