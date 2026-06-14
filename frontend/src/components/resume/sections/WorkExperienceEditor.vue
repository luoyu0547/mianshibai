<!-- src/components/resume/sections/WorkExperienceEditor.vue -->
<template>
  <div class="work-editor">
    <NbCard v-for="(item, index) in items" :key="index" variant="muted" compact class="work-item">
      <div class="work-item__header">
        <span class="work-item__title">工作经历 {{ index + 1 }}</span>
        <el-button type="danger" text size="small" @click="removeItem(index)">删除</el-button>
      </div>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="公司">
              <el-input v-model="item.company" placeholder="请输入公司名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位">
              <el-input v-model="item.position" placeholder="请输入职位" />
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
        <el-form-item label="工作描述">
          <FormattedTextarea :model-value="String(item.description || '')" :rows="4" placeholder="请描述工作内容" @update:model-value="val => item.description = val" />
        </el-form-item>

      </el-form>
    </NbCard>
    <NbButton variant="ghost" block class="add-btn" @click="addItem">+ 添加工作经历</NbButton>
  </div>
</template>

<script setup lang="ts">
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import FormattedTextarea from '@/components/resume/FormattedTextarea.vue'

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
      company: '',
      position: '',
      startDate: '',
      endDate: '',
      description: '',
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
.work-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.work-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.work-item__title {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
}

</style>
