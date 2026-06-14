<!-- src/components/resume/sections/SkillsEditor.vue -->
<template>
  <div class="skills-editor">
    <NbCard v-for="(cat, index) in categories" :key="index" variant="muted" compact class="skill-category">
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
      </div>
      <div class="skill-category__add-row">
        <el-input
          v-model="skillInputValue[index]"
          placeholder="输入技能名称"
          size="small"
          class="skill-category__add-input"
          @keyup.enter="handleSkillConfirm(index)"
        />
        <el-button size="small" @click="handleSkillConfirm(index)">添加</el-button>
      </div>
    </NbCard>
    <NbButton variant="ghost" block class="add-btn" @click="addCategory">+ 添加分类</NbButton>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'

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

const skillInputValue = ref<Record<number, string>>({})

function addCategory() {
  categories.value = [...categories.value, { name: '', items: [] }]
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
  newCats[catIndex] = { name: cat.name, items: cat.items.filter((_, i) => i !== skillIndex) }
  categories.value = newCats
}

function handleSkillConfirm(catIndex: number) {
  const val = (skillInputValue.value[catIndex] || '').trim()
  if (!val) return
  const newCats = [...categories.value]
  const cat = newCats[catIndex]
  if (cat) {
    newCats[catIndex] = { name: cat.name, items: [...cat.items, val] }
    categories.value = newCats
  }
  skillInputValue.value[catIndex] = ''
}
</script>

<style scoped>
.skills-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-category__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.skill-category__name {
  max-width: 240px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.skill-tag {
  border: var(--nb-border);
  box-shadow: var(--nb-shadow-xs);
}

.skill-category__add-row {
  display: flex;
  gap: 8px;
}

.skill-category__add-input {
  flex: 1;
  max-width: 260px;
}
</style>
