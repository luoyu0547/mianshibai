<!-- src/components/NbAvatarUploader.vue -->
<template>
  <div class="nb-avatar-uploader" :style="rootStyle">
    <div
      class="nb-avatar-uploader__preview"
      :class="[`nb-avatar-uploader__preview--${shape}`]"
      :style="previewStyle"
      role="button"
      tabindex="0"
      @click="handleClick"
      @keydown.enter.space.prevent="handleClick"
    >
      <img
        v-if="modelValue"
        :src="modelValue"
        :alt="fallbackText || '头像'"
        class="nb-avatar-uploader__img"
      />
      <span v-else class="nb-avatar-uploader__fallback">{{ displayFallback }}</span>

      <div v-if="isUploading" class="nb-avatar-uploader__overlay">
        <span class="nb-avatar-uploader__spinner" />
      </div>
      <div v-else class="nb-avatar-uploader__overlay nb-avatar-uploader__overlay--hover">
        <span class="nb-avatar-uploader__hint">{{ modelValue ? '更换头像' : '上传头像' }}</span>
      </div>
    </div>

    <div v-if="clearable && modelValue" class="nb-avatar-uploader__actions">
      <button type="button" class="nb-avatar-uploader__remove" @click.stop="handleRemove">
        移除头像
      </button>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      class="nb-avatar-uploader__input"
      :accept="accept"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Props {
  modelValue: string
  uploadFn?: (file: File) => Promise<string>
  fallbackText?: string
  size?: number
  shape?: 'circle' | 'rounded' | 'square'
  accept?: string
  maxSize?: number
  clearable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fallbackText: '',
  size: 120,
  shape: 'circle',
  accept: 'image/jpeg,image/png,image/webp',
  maxSize: 2 * 1024 * 1024,
  clearable: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'upload-success': [url: string]
  'upload-error': [error: unknown]
  'remove': []
}>()

const fileInputRef = ref<HTMLInputElement>()
const isUploading = ref(false)

const rootStyle = computed(() => ({
  width: `${props.size}px`,
  '--preview-size': `${props.size}px`,
  '--fallback-size': `${Math.max(16, props.size * 0.4)}px`,
} as Record<string, string>))

const previewStyle = computed(() => ({
  width: `${props.size}px`,
  height: props.shape === 'rounded' ? `${Math.round(props.size * 1.17)}px` : `${props.size}px`,
}))

const displayFallback = computed(() => {
  if (props.fallbackText) return props.fallbackText.slice(0, 1)
  return 'U'
})

function handleClick() {
  if (isUploading.value) return
  fileInputRef.value?.click()
}

function handleRemove() {
  emit('update:modelValue', '')
  emit('remove')
}

function validateFile(file: File): boolean {
  const acceptedTypes = props.accept.split(',').map((t) => t.trim())
  if (!acceptedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WebP 图片')
    return false
  }

  if (file.size > props.maxSize) {
    ElMessage.warning('头像不能超过 2MB')
    return false
  }

  return true
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!validateFile(file)) {
    resetInput(input)
    return
  }

  isUploading.value = true
  try {
    const url = await resolveUrl(file)
    emit('update:modelValue', url)
    emit('upload-success', url)
    ElMessage.success('头像上传成功')
  } catch (error) {
    emit('upload-error', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    isUploading.value = false
    resetInput(input)
  }
}

async function resolveUrl(file: File): Promise<string> {
  if (props.uploadFn) {
    return await props.uploadFn(file)
  }
  return await readFileAsDataUrl(file)
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

function resetInput(input: HTMLInputElement) {
  input.value = ''
}
</script>

<style scoped>
.nb-avatar-uploader {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.nb-avatar-uploader__preview {
  position: relative;
  overflow: hidden;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  background: var(--nb-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--nb-transition);
}

.nb-avatar-uploader__preview--circle {
  border-radius: 50%;
}

.nb-avatar-uploader__preview--rounded {
  border-radius: 10px;
}

.nb-avatar-uploader__preview--square {
  border-radius: var(--nb-radius);
}

.nb-avatar-uploader__preview:hover {
  box-shadow: var(--nb-shadow-sm);
}

.nb-avatar-uploader__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.nb-avatar-uploader__fallback {
  font-family: var(--font-heading);
  font-size: var(--fallback-size);
  font-weight: 700;
}

.nb-avatar-uploader__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 200ms ease;
}

.nb-avatar-uploader__preview:hover .nb-avatar-uploader__overlay--hover,
.nb-avatar-uploader__preview:focus-visible .nb-avatar-uploader__overlay--hover {
  opacity: 1;
}

.nb-avatar-uploader__overlay:not(.nb-avatar-uploader__overlay--hover) {
  opacity: 1;
}

.nb-avatar-uploader__hint {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.nb-avatar-uploader__spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: nb-avatar-uploader-spin 1s linear infinite;
}

@keyframes nb-avatar-uploader-spin {
  to {
    transform: rotate(360deg);
  }
}

.nb-avatar-uploader__input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.nb-avatar-uploader__actions {
  display: flex;
  justify-content: center;
}

.nb-avatar-uploader__remove {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--nb-muted);
  font-size: 12px;
  cursor: pointer;
  transition: var(--nb-transition);
}

.nb-avatar-uploader__remove:hover {
  color: var(--nb-danger);
}
</style>
