<template>
  <div class="nb-loading-block" role="status" aria-live="polite">
    <span class="sr-only">加载中</span>
    <div v-if="title" class="nb-loading-block__title">{{ title }}</div>
    <div class="nb-loading-block__rows">
      <div
        v-for="i in rows"
        :key="i"
        class="nb-loading-block__row"
        :style="rowStyle"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    rows?: number
    height?: string
    title?: string
  }>(),
  {
    rows: 3,
    height: 'auto',
    title: undefined,
  },
)

const rowStyle = computed(() => {
  if (props.height === 'auto') return {}
  return { height: props.height }
})
</script>

<style scoped>
.nb-loading-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.nb-loading-block__title {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--nb-muted);
  margin-bottom: 4px;
}

.nb-loading-block__rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nb-loading-block__row {
  height: 14px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  background: var(--nb-muted-surface);
  animation: nb-loading-pulse 1.4s ease-in-out infinite;
}

.nb-loading-block__row:last-child {
  width: 65%;
}

@keyframes nb-loading-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}
</style>
