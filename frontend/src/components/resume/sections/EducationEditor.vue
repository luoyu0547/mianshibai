<!-- src/components/resume/sections/EducationEditor.vue -->
<template>
  <div class="education-editor">
    <NbCard v-for="(item, index) in items" :key="index" variant="muted" compact class="education-item">
      <div class="education-item__header">
        <span class="education-item__title">教育经历 {{ index + 1 }}</span>
        <el-button type="danger" text size="small" @click="removeItem(index)">删除</el-button>
      </div>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学校">
              <el-input v-model="item.school" placeholder="请输入学校名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="item.major" placeholder="请输入专业" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="item.degree" placeholder="请选择学历" style="width: 100%;">
                <el-option label="本科" value="本科" />
                <el-option label="硕士" value="硕士" />
                <el-option label="博士" value="博士" />
                <el-option label="大专" value="大专" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="GPA">
              <el-input v-model="item.gpa" placeholder="例如：3.8/4.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="item.startDate"
                type="month"
                format="YYYY-MM"
                value-format="YYYY-MM"
                placeholder="选择开始时间"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="item.endDate"
                type="month"
                format="YYYY-MM"
                value-format="YYYY-MM"
                placeholder="选择结束时间"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>

      </el-form>
    </NbCard>
    <NbButton variant="ghost" block class="add-btn" @click="addItem">+ 添加教育经历</NbButton>
  </div>
</template>

<script setup lang="ts">
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'

const props = defineProps<{
  items: Record<string, unknown>[]
}>()

const emit = defineEmits<{
  'update:items': [value: Record<string, unknown>[]]
}>()

function getItems(): Record<string, unknown>[] {
  return props.items
}

function addItem() {
  const newList = [
    ...getItems(),
    {
      school: '',
      major: '',
      degree: '',
      startDate: '',
      endDate: '',
      gpa: '',
    },
  ]
  emit('update:items', newList)
}

function removeItem(index: number) {
  const newList = [...getItems()]
  newList.splice(index, 1)
  emit('update:items', newList)
}

</script>

<style scoped>
.education-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.education-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.education-item__title {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
}

</style>
