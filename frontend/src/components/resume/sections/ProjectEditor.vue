<!-- src/components/resume/sections/ProjectEditor.vue -->
<template>
  <div class="project-editor">
    <NbCard v-for="(item, index) in items" :key="index" variant="muted" compact class="project-item">
      <div class="project-item__header">
        <span class="project-item__title">项目经历 {{ index + 1 }}</span>
        <el-button type="danger" text size="small" @click="removeItem(index)">删除</el-button>
      </div>
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="项目名称">
              <el-input v-model="item.name" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色">
              <el-input v-model="item.role" placeholder="例如：核心开发" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="技术栈">
          <el-select
            v-model="item.techStack"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入技术栈"
            style="width: 100%;"
          >
            <el-option
              v-for="t in techPresets"
              :key="t"
              :label="t"
              :value="t"
            />
          </el-select>
        </el-form-item>
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
        <el-form-item label="项目描述">
          <FormattedTextarea :model-value="String(item.description || '')" :rows="4" placeholder="请描述项目内容" @update:model-value="val => item.description = val" />
        </el-form-item>

      </el-form>
    </NbCard>
    <NbButton variant="ghost" block class="add-btn" @click="addItem">+ 添加项目经历</NbButton>
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

const techPresets = [
  'Java', 'Spring Boot', 'MyBatis', 'MySQL', 'Redis',
  'Go', 'Python', 'FastAPI', 'Django',
  'TypeScript', 'Vue 3', 'React', 'Node.js',
  'Docker', 'Kubernetes', 'Linux', 'Git',
  'Kafka', 'RabbitMQ', 'Elasticsearch', 'MongoDB',
  'Nginx', 'Jenkins', 'CI/CD', '微服务',
]

function getItems(): Record<string, unknown>[] {
  return props.items
}

function addItem() {
  const newList = [
    ...getItems(),
    {
      name: '',
      role: '',
      techStack: [],
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
.project-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.project-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.project-item__title {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
}

</style>
