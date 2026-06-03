<!-- src/components/resume/sections/WorkExperienceEditor.vue -->
<template>
  <div class="work-editor">
    <div v-for="(item, index) in items" :key="index" class="work-item">
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
          <el-input v-model="item.description" type="textarea" :rows="3" placeholder="请描述工作内容" />
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
    <el-button class="add-btn" @click="addItem">+ 添加工作经历</el-button>
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
      company: '',
      position: '',
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
.work-item {
  background: var(--nb-card);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  padding: 20px;
  margin-bottom: 16px;
}

.work-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.work-item__title {
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
