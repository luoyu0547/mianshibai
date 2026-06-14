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
          <el-select
            v-model="formData.targetPosition"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入岗位"
          >
            <el-option
              v-for="pos in positionPresets"
              :key="pos"
              :label="pos"
              :value="pos"
            />
          </el-select>
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
          <el-select
            v-model="formData.currentStatus"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入状态"
          >
            <el-option
              v-for="s in statusPresets"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="所在城市">
          <el-select
            v-model="formData.city"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入城市"
          >
            <el-option
              v-for="c in cityPresets"
              :key="c"
              :label="c"
              :value="c"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="期望工作地">
          <el-select
            v-model="formData.expectedLocation"
            filterable
            allow-create
            default-first-option
            multiple
            placeholder="选择或输入城市"
          >
            <el-option
              v-for="c in cityPresets"
              :key="c"
              :label="c"
              :value="c"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="期望薪资">
          <el-select
            v-model="formData.expectedSalary"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入薪资"
          >
            <el-option
              v-for="s in salaryPresets"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
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

const positionPresets = [
  'Java 开发工程师', 'Golang 开发工程师', 'Python 开发工程师',
  '前端开发工程师', '全栈开发工程师', '算法工程师', '测试开发工程师',
  'DevOps / SRE 工程师', '技术经理', '架构师',
]

const statusPresets = [
  '应届毕业生', '实习生', '在职考虑机会', '已离职', '自由职业',
]

const cityPresets = [
  '北京', '上海', '广州', '深圳', '杭州', '成都', '南京', '武汉',
  '西安', '苏州', '重庆', '长沙', '天津', '远程',
]

const salaryPresets = [
  '5k-10k', '10k-15k', '15k-20k', '20k-30k',
  '30k-40k', '40k-50k', '50k-70k', '面议',
]
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
