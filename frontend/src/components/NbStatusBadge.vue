<template>
  <span
    :class="['nb-status-badge', `nb-status-badge--${variant}`]"
    :title="title"
    :aria-label="title"
  >
    {{ label }}
  </span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'ai'
    title?: string
  }>(),
  {
    label: '',
    variant: 'default',
    title: undefined,
  },
)
</script>

<style scoped>
.nb-status-badge {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  padding: 3px 10px;
  border: 1px solid transparent;
  border-radius: 20px;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.nb-status-badge--default {
  background: var(--nb-muted-surface);
  color: var(--nb-muted);
  border-color: var(--nb-border-color);
}

.nb-status-badge--primary {
  background: var(--nb-primary-light);
  color: var(--nb-primary);
}

.nb-status-badge--success {
  background: var(--nb-success-light);
  color: var(--nb-success);
}

.nb-status-badge--warning {
  background: var(--nb-warning-light);
  color: #B45309;
}

.nb-status-badge--danger {
  background: var(--nb-danger-light);
  color: var(--nb-danger);
}

.nb-status-badge--info {
  background: var(--nb-secondary-light);
  color: var(--nb-secondary);
}

.nb-status-badge--muted {
  background: var(--nb-muted-surface);
  color: var(--nb-muted);
  border-color: var(--nb-border-color);
}

.nb-status-badge--ai {
  background: linear-gradient(135deg, var(--nb-primary-light), var(--nb-accent-light));
  color: var(--nb-primary);
}
</style>
