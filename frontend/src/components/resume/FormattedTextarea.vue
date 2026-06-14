<template>
  <div class="fmt-ta">
    <div class="fmt-ta__bar">
      <button type="button" class="fmt-ta__btn" :class="{ 'fmt-ta__btn--active': isBold }" title="加粗" @mousedown.prevent="exec('bold')">
        <svg viewBox="0 0 24 24"><path d="M15.6 10.8c1-.7 1.6-1.8 1.6-2.8 0-2.2-1.8-4-4-4H7v14h7c2.1 0 3.7-1.7 3.7-3.8 0-1.5-.8-2.8-2.1-3.4zM9 7h3c1.1 0 2 .9 2 2s-.9 2-2 2H9V7zm3.5 10H9v-3h3.5c1.1 0 2 .9 2 2s-.9 2-2 2z"/></svg>
      </button>
      <button type="button" class="fmt-ta__btn" :class="{ 'fmt-ta__btn--active': isUnordered }" title="无序列表" @mousedown.prevent="exec('insertUnorderedList')">
        <svg viewBox="0 0 24 24"><path d="M4 10.5c-.8 0-1.5.7-1.5 1.5s.7 1.5 1.5 1.5 1.5-.7 1.5-1.5-.7-1.5-1.5-1.5zm0-6c-.8 0-1.5.7-1.5 1.5S3.2 7.5 4 7.5 5.5 6.8 5.5 6 4.8 4.5 4 4.5zm0 12c-.8 0-1.5.7-1.5 1.5s.7 1.5 1.5 1.5 1.5-.7 1.5-1.5-.7-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z"/></svg>
      </button>
      <button type="button" class="fmt-ta__btn" :class="{ 'fmt-ta__btn--active': isOrdered }" title="有序列表" @mousedown.prevent="exec('insertOrderedList')">
        <svg viewBox="0 0 24 24"><path d="M2 17h2v.5H3v1h1v.5H2v1h3v-4H2v1zm1-9h1V4H2v1h1v3zm-1 3h1.8L2 13.1v.9h3v-1H3.2L5 10.9V10H2v1zm5-6v2h14V5H7zm0 14h14v-2H7v2zm0-6h14v-2H7v2z"/></svg>
      </button>
    </div>
    <div
      ref="editorRef"
      class="fmt-ta__editor"
      contenteditable="true"
      :data-placeholder="placeholder"
      @input="handleInput"
      @keydown="handleKeydown"
      @paste="handlePaste"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: string
  rows?: number
  placeholder?: string
}>(), {
  rows: 5,
  placeholder: '请输入内容',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = ref<HTMLDivElement>()

const isBold = ref(false)
const isUnordered = ref(false)
const isOrdered = ref(false)

function exec(command: string) {
  document.execCommand(command, false)
  editorRef.value?.focus()
  updateState()
  emitHtml()
}

function updateState() {
  isBold.value = document.queryCommandState('bold')
  isUnordered.value = document.queryCommandState('insertUnorderedList')
  isOrdered.value = document.queryCommandState('insertOrderedList')
}

function emitHtml() {
  const html = editorRef.value?.innerHTML || ''
  emit('update:modelValue', html)
}

function handleInput() {
  updateState()
  emitHtml()
}

function handleKeydown(e: KeyboardEvent) {
  updateState()
  if (e.key === 'Enter') {
    nextTick(emitHtml)
  }
}

function handlePaste(e: ClipboardEvent) {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') || ''
  document.execCommand('insertText', false, text)
}

onMounted(() => {
  if (editorRef.value && props.modelValue) {
    editorRef.value.innerHTML = props.modelValue
  }
})

watch(() => props.modelValue, (val) => {
  if (editorRef.value && editorRef.value.innerHTML !== val) {
    editorRef.value.innerHTML = val || ''
  }
})
</script>

<style scoped>
.fmt-ta {
  width: 100%;
}

.fmt-ta__bar {
  display: flex;
  gap: 4px;
  margin-bottom: 0;
  padding: 4px 6px;
  background: var(--nb-bg);
  border: 1px solid var(--nb-border-color-light);
  border-radius: var(--nb-radius) var(--nb-radius) 0 0;
  border-bottom: none;
}

.fmt-ta__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--nb-border-color-light);
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  transition: var(--nb-transition);
}
.fmt-ta__btn:hover {
  background: var(--nb-primary-light);
  border-color: var(--nb-primary);
}
.fmt-ta__btn--active {
  background: var(--nb-primary-light);
  border-color: var(--nb-primary);
}
.fmt-ta__btn svg {
  width: 16px;
  height: 16px;
  fill: var(--nb-muted);
}
.fmt-ta__btn:hover svg,
.fmt-ta__btn--active svg {
  fill: var(--nb-primary);
}

.fmt-ta__editor {
  min-height: 100px;
  max-height: 240px;
  overflow-y: auto;
  padding: 8px 12px;
  border: 1px solid var(--nb-border-color-light);
  border-top: none;
  border-radius: 0 0 var(--nb-radius) var(--nb-radius);
  background: #fff;
  font-size: 13px;
  line-height: 1.7;
  outline: none;
  word-break: break-word;
}
.fmt-ta__editor:empty::before {
  content: attr(data-placeholder);
  color: var(--nb-muted);
  pointer-events: none;
}
.fmt-ta__editor:focus {
  border-color: var(--nb-primary);
  box-shadow: 0 0 0 1px var(--nb-primary);
}
</style>
