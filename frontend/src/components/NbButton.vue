<template>
  <button
    :class="['nb-button', `nb-button--${type}`, { 'nb-button--loading': loading, 'nb-button--block': block }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="nb-button__spinner"></span>
    <slot />
  </button>
</template>

<script setup lang="ts">
interface Props {
  type?: 'primary' | 'secondary' | 'accent' | 'success'
  loading?: boolean
  disabled?: boolean
  block?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
})

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
  font-size: 15px;
  padding: 12px 28px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  box-shadow: var(--nb-shadow);
  cursor: pointer;
  transition: var(--nb-transition);
  background: var(--nb-card);
  color: var(--nb-text);
}

.nb-button:hover:not(:disabled) {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-1px, -1px);
}

.nb-button:active:not(:disabled) {
  box-shadow: none;
  transform: translate(4px, 4px);
}

.nb-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.nb-button--primary {
  background: var(--nb-primary);
  color: #fff;
}

.nb-button--secondary {
  background: var(--nb-secondary);
  color: var(--nb-text);
}

.nb-button--accent {
  background: var(--nb-accent);
  color: #fff;
}

.nb-button--success {
  background: var(--nb-success);
  color: #fff;
}

.nb-button--block {
  width: 100%;
}

.nb-button__spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: nb-spin 0.8s linear infinite;
}

@keyframes nb-spin {
  to { transform: rotate(360deg); }
}
</style>
