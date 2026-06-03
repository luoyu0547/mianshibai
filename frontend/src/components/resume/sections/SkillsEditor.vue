<!-- src/components/resume/sections/SkillsEditor.vue -->
<template>
  <div class="skills-editor">
    <div v-for="(cat, index) in categories" :key="index" class="skill-category">
      <div class="skill-category__header">
        <el-input
          v-model="cat.name"
          placeholder="分类名称"
          class="skill-category__name"
        />
        <el-button type="danger" text size="small" @click="removeCategory(index)">删除</el-button>
      </div>
      <div class="tag-list">
        <el-tag
          v-for="(skill, skillIndex) in cat.items"
          :key="skillIndex"
          closable
          class="skill-tag"
          @close="removeSkill(index, skillIndex)"
        >
          {{ skill }}
        </el-tag>
        <el-input
          v-if="skillInputVisible[index]"
          :ref="(el: unknown) => setSkillInputRef(el as HTMLInputElement | null, index)"
          v-model="skillInputValue[index]"
          size="small"
          class="skill-input"
          @keyup.enter="handleSkillConfirm(index)"
          @blur="handleSkillConfirm(index)"
        />
        <el-button v-else size="small" @click="showSkillInput(index)">
          + 添加技能
        </el-button>
      </div>
    </div>
    <el-button class="add-btn" @click="addCategory">+ 添加分类</el-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'

interface SkillCategory {
  name: string
  items: string[]
}

const props = defineProps<{
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
}>()

const categories = computed({
  get: () => (props.modelValue.categories as SkillCategory[]) || [],
  set: (val: SkillCategory[]) => {
    emit('update:modelValue', { ...props.modelValue, categories: val })
  },
})

const skillInputVisible = ref<Record<number, boolean>>({})
const skillInputValue = ref<Record<number, string>>({})
const skillInputRefs = ref<Record<number, HTMLInputElement | null>>({})

function setSkillInputRef(el: HTMLInputElement | null, index: number) {
  skillInputRefs.value[index] = el
}

function addCategory() {
  const newCats = [...categories.value, { name: '', items: [] as string[] }]
  categories.value = newCats
}

function removeCategory(index: number) {
  const newCats = [...categories.value]
  newCats.splice(index, 1)
  categories.value = newCats
}

function removeSkill(catIndex: number, skillIndex: number) {
  const newCats = [...categories.value]
  const cat = newCats[catIndex]
  if (!cat) return
  const items = [...cat.items]
  items.splice(skillIndex, 1)
  newCats[catIndex] = { name: cat.name, items }
  categories.value = newCats
}

function showSkillInput(index: number) {
  skillInputVisible.value[index] = true
  skillInputValue.value[index] = ''
  nextTick(() => {
    skillInputRefs.value[index]?.focus()
  })
}

function handleSkillConfirm(catIndex: number) {
  const val = skillInputValue.value[catIndex]?.trim()
  if (val) {
    const newCats = [...categories.value]
    const cat = newCats[catIndex]
    if (cat) {
      const items = [...cat.items, val]
      newCats[catIndex] = { name: cat.name, items }
      categories.value = newCats
    }
  }
  skillInputVisible.value[catIndex] = false
  skillInputValue.value[catIndex] = ''
}
</script>

<style scoped>
.skill-category {
  background: var(--nb-card);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  padding: 16px;
  margin-bottom: 12px;
}

.skill-category__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.skill-category__name {
  max-width: 240px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.skill-tag {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
}

.skill-input {
  width: 120px;
}

.add-btn {
  width: 100%;
  border-style: dashed;
}
</style>
