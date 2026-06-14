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
      <ul class="skill-list" v-if="cat.items.length">
        <li v-for="(skill, skillIndex) in cat.items" :key="skillIndex" class="skill-list__item">
          <span class="skill-list__dot" />
          <span>{{ skill }}</span>
          <el-button type="danger" text size="small" @click="removeSkill(index, skillIndex)">
            <svg viewBox="0 0 24 24" width="14" height="14"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </el-button>
        </li>
      </ul>
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

.skill-list {
  list-style: none;
  margin: 0 0 10px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skill-list__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
  font-size: 13px;
}

.skill-list__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--nb-primary);
  flex-shrink: 0;
}

.skill-list__item .el-button {
  margin-left: auto;
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
