<!-- src/components/resume/sections/ProjectEditor.vue -->
<template>
  <div class="project-editor">
    <div v-for="(item, index) in items" :key="index" class="project-item">
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
            placeholder="输入后回车添加"
            style="width: 100%;"
          />
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
          <el-input v-model="item.description" type="textarea" :rows="3" placeholder="请描述项目内容" />
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
    </div>
    <el-button class="add-btn" @click="addItem">+ 添加项目经历</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'

const props = defineProps<{
  items: Record<string, unknown>[]
}>()

const emit = defineEmits<{
  'update:items': [value: Record<string, unknown>[]]
}>()

const tagInputVisible = ref<Record<number, boolean>>({})
const tagInputValue = ref<Record<number, string>>({})
const tagInputRefs = ref<Record<number, HTMLInputElement | null>>({})

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
.project-item {
  background: var(--nb-card);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  padding: 20px;
  margin-bottom: 16px;
}

.project-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.project-item__title {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 16px;
}

.add-btn {
  width: 100%;
  border-style: dashed;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.highlight-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.tag-input {
  width: 120px;
}
</style>
