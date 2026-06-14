<template>
  <button
    :class="[
      'nb-button',
      `nb-button--${resolvedVariant}`,
      {
        'nb-button--loading': loading,
        'nb-button--block': block,
        'nb-button--outlined': outlined,
      },
    ]"
    :type="nativeType"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="nb-button__spinner"></span>
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    type?: 'primary' | 'secondary' | 'accent' | 'success'
    variant?: 'primary' | 'secondary' | 'accent' | 'success' | 'warning' | 'danger' | 'ghost'
    nativeType?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    block?: boolean
    outlined?: boolean
  }>(),
  {
    type: 'primary',
    variant: undefined,
    nativeType: 'button',
    loading: false,
    disabled: false,
    block: false,
    outlined: false,
  },
)

const resolvedVariant = computed(() => props.variant ?? props.type)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

function handleClick(event: MouseEvent) {
  if (!props.loading && !props.disabled) {
    emit('click', event)
  }
}
</script>

<style scoped>
.nb-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  line-height: 1;
  padding: 10px 24px;
  border: 1.5px solid transparent;
  border-radius: var(--nb-radius);
  cursor: pointer;
  transition: var(--nb-transition);
  background: var(--nb-surface);
  color: var(--nb-ink);
  box-shadow: var(--nb-shadow-sm);
  position: relative;
  overflow: hidden;
}

.nb-button:hover:not(:disabled) {
  box-shadow: var(--nb-shadow);
  transform: translateY(-1px);
}

.nb-button:active:not(:disabled) {
  box-shadow: var(--nb-shadow-xs);
  transform: translateY(0);
}

.nb-button:focus-visible {
  outline: 2px solid var(--nb-primary);
  outline-offset: 2px;
}

.nb-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: var(--nb-shadow-xs) !important;
}

/* 实心按钮 */
.nb-button--primary {
  background: var(--nb-primary);
  color: #fff;
  border-color: var(--nb-primary);
}

.nb-button--primary:hover:not(:disabled) {
  background: var(--nb-primary-dark);
  border-color: var(--nb-primary-dark);
}

.nb-button--secondary {
  background: var(--nb-secondary);
  color: #fff;
  border-color: var(--nb-secondary);
}

.nb-button--accent {
  background: var(--nb-accent);
  color: #fff;
  border-color: var(--nb-accent);
}

.nb-button--success {
  background: var(--nb-success);
  color: #fff;
  border-color: var(--nb-success);
}

.nb-button--warning {
  background: var(--nb-warning);
  color: var(--nb-ink);
  border-color: var(--nb-warning);
}

.nb-button--danger {
  background: var(--nb-danger);
  color: #fff;
  border-color: var(--nb-danger);
}

/* 描边按钮 */
.nb-button--ghost {
  background: transparent;
  color: var(--nb-ink);
  border-color: var(--nb-border-color);
  box-shadow: none;
}

.nb-button--ghost:hover:not(:disabled) {
  background: var(--nb-muted-surface);
  border-color: var(--nb-muted-light);
  box-shadow: var(--nb-shadow-sm);
}

.nb-button--outlined {
  background: transparent;
  border-color: var(--nb-primary);
  color: var(--nb-primary);
}

.nb-button--outlined:hover:not(:disabled) {
  background: var(--nb-primary-light);
}

.nb-button--block {
  width: 100%;
}

/* 加载旋转器 */
.nb-button__spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: nb-spin 0.7s linear infinite;
}

@keyframes nb-spin {
  to { transform: rotate(360deg); }
}
</style>
