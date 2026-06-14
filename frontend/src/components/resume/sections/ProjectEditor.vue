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
        <el-form-item label="亮点标签">
          <div class="tag-list">
            <el-tag
              v-for="(tag, tagIndex) in (item.highlights as string[])"
              :key="tagIndex"
              closable
              class="highlight-tag"
              @close="(item.highlights as string[]).splice(tagIndex, 1)"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="tagInputVisible[index]"
              :ref="(el: unknown) => setTagInputRef(el as HTMLInputElement | null, index)"
              v-model="tagInputValue[index]"
              size="small"
              class="tag-input"
              @keyup.enter="handleTagConfirm(index)"
              @blur="handleTagConfirm(index)"
            />
            <el-button v-else size="small" @click="showTagInput(index)">
              + 添加标签
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </NbCard>
    <NbButton variant="ghost" block class="add-btn" @click="addItem">+ 添加项目经历</NbButton>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import FormattedTextarea from '@/components/resume/FormattedTextarea.vue'

const props = defineProps<{
  items: Record<string, unknown>[]
}>()

const emit = defineEmits<{
  'update:items': [value: Record<string, unknown>[]]
}>()

const tagInputVisible = ref<Record<number, boolean>>({})
const tagInputValue = ref<Record<number, string>>({})
const tagInputRefs = ref<Record<number, HTMLInputElement | null>>({})

const techPresets = [
  'Java', 'Spring Boot', 'MyBatis', 'MySQL', 'Redis',
  'Go', 'Python', 'FastAPI', 'Django',
  'TypeScript', 'Vue 3', 'React', 'Node.js',
  'Docker', 'Kubernetes', 'Linux', 'Git',
  'Kafka', 'RabbitMQ', 'Elasticsearch', 'MongoDB',
  'Nginx', 'Jenkins', 'CI/CD', '微服务',
]

function setTagInputRef(el: HTMLInputElement | null, index: number) {
  tagInputRefs.value[index] = el
}

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
      highlights: [],
    },
  ]
  emit('update:items', newList)
}

function removeItem(index: number) {
  const newList = [...getItems()]
  newList.splice(index, 1)
  emit('update:items', newList)
}

function showTagInput(index: number) {
  tagInputVisible.value[index] = true
  tagInputValue.value[index] = ''
  nextTick(() => {
    tagInputRefs.value[index]?.focus()
  })
}

function handleTagConfirm(itemIndex: number) {
  const val = tagInputValue.value[itemIndex]?.trim()
  if (val) {
    const newList = [...getItems()]
    const item = newList[itemIndex]
    if (item) {
      const highlights = (item.highlights as string[]) || []
      newList[itemIndex] = { ...item, highlights: [...highlights, val] }
      emit('update:items', newList)
    }
  }
  tagInputVisible.value[itemIndex] = false
  tagInputValue.value[itemIndex] = ''
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

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.highlight-tag {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow-xs);
}

.tag-input {
  width: 120px;
}
</style>
