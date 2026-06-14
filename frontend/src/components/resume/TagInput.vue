<!-- src/components/resume/TagInput.vue -->
<template>
  <div class="tag-input" :class="{ 'is-focused': focused }" @click="focusInput">
    <span v-for="(tag, i) in modelValue" :key="i" class="tag-input__tag">
      {{ tag }}
      <button class="tag-input__close" @click.stop="remove(i)">&times;</button>
    </span>
    <input
      ref="inputRef"
      v-model="text"
      class="tag-input__field"
      :placeholder="modelValue.length ? '' : placeholder"
      :style="{ width: inputWidth }"
      @keydown.enter.prevent="add"
      @keydown.backspace="handleBackspace"
      @focus="focused = true"
      @blur="focused = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string[]
  placeholder?: string
}>(), {
  placeholder: '输入后按回车添加',
})

const emit = defineEmits<{ 'update:modelValue': [v: string[]] }>()

const text = ref('')
const focused = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)

const inputWidth = computed(() => {
  if (!text.value) return '80px'
  return Math.max(80, text.value.length * 14 + 20) + 'px'
})

function focusInput() {
  inputRef.value?.focus()
}

function add() {
  const val = text.value.trim()
  if (val && !props.modelValue.includes(val)) {
    emit('update:modelValue', [...props.modelValue, val])
  }
  text.value = ''
  nextTick(() => inputRef.value?.focus())
}

function remove(index: number) {
  const arr = [...props.modelValue]
  arr.splice(index, 1)
  emit('update:modelValue', arr)
}

function handleBackspace() {
  if (!text.value && props.modelValue.length) {
    remove(props.modelValue.length - 1)
  }
}
</script>

<style scoped>
.tag-input {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 6px 10px;
  min-height: 36px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
  transition: border-color 0.2s;
  cursor: text;
}

.tag-input.is-focused {
  border-color: var(--nb-primary);
  box-shadow: 0 0 0 2px var(--nb-primary-light);
}

.tag-input__tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 24px;
  padding: 0 8px;
  background: var(--nb-primary-light);
  color: var(--nb-primary);
  border: 1px solid color-mix(in srgb, var(--nb-primary) 30%, transparent);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.tag-input__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--nb-primary);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
  margin-left: 1px;
}

.tag-input__close:hover {
  background: color-mix(in srgb, var(--nb-primary) 20%, transparent);
}

.tag-input__field {
  flex: 0 0 auto;
  min-width: 60px;
  height: 24px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  font-family: var(--font-body);
  color: var(--nb-ink);
}

.tag-input__field::placeholder {
  color: var(--nb-muted-light);
  font-size: 12px;
}
</style>
