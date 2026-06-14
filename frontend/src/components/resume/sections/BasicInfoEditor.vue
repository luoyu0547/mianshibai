<!-- src/components/resume/sections/BasicInfoEditor.vue -->
<template>
  <el-form label-position="top" class="basic-info-editor">
    <div class="basic-info-editor__group">
      <div class="basic-info-editor__avatar">
        <div class="basic-info-editor__avatar-preview">
          <img v-if="formData.avatar" :src="String(formData.avatar)" alt="头像预览" />
          <span v-else>{{ String(formData.name || '你').slice(0, 1) }}</span>
        </div>
        <el-form-item label="头像 URL" class="basic-info-editor__avatar-input">
          <el-input v-model="formData.avatar" placeholder="粘贴头像图片链接，可选" />
        </el-form-item>
      </div>
    </div>

    <div class="basic-info-editor__group">
      <h4>身份信息</h4>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="姓名">
            <el-input v-model="formData.name" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="手机号">
            <el-input v-model="formData.phone" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="邮箱">
            <el-input v-model="formData.email" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="当前状态">
            <el-input v-model="formData.currentStatus" placeholder="例如：应届毕业生" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="目标岗位">
            <el-input v-model="formData.targetPosition" placeholder="例如：Java 开发工程师" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所在城市">
            <el-input v-model="formData.city" placeholder="例如：北京" />
          </el-form-item>
        </el-col>
      </el-row>
    </div>

    <div class="basic-info-editor__group">
      <h4>求职意向</h4>
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
    </div>

    <div class="basic-info-editor__group">
      <h4>在线资料</h4>
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
    </div>
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
.basic-info-editor {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.basic-info-editor__group {
  padding: 16px;
  background: #fff;
  border: 1px solid var(--nb-border-color-light);
  border-radius: var(--nb-radius-lg);
}

.basic-info-editor__group h4 {
  margin: 0 0 14px;
  font-family: var(--font-heading);
  font-size: 15px;
  color: var(--nb-ink);
}

.basic-info-editor__avatar {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.basic-info-editor__avatar-preview {
  width: 96px;
  height: 112px;
  border: 1px solid var(--nb-border-color);
  border-radius: 10px;
  overflow: hidden;
  background: var(--nb-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--nb-primary);
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 700;
  flex-shrink: 0;
}

.basic-info-editor__avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.basic-info-editor__avatar-input {
  flex: 1;
  margin-bottom: 0;
}

.basic-info-editor :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
}

@media (max-width: 768px) {
  .basic-info-editor__avatar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
