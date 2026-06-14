<template>
  <div
    :class="[
      'nb-card',
      `nb-card--${variant}`,
      {
        'nb-card--hoverable': hoverable,
        'nb-card--clickable': clickable,
        'nb-card--compact': compact,
        'nb-card--glass': glass,
      },
    ]"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    hoverable?: boolean
    clickable?: boolean
    compact?: boolean
    glass?: boolean
    variant?: 'default' | 'elevated' | 'accent' | 'success' | 'warning' | 'danger' | 'ai' | 'muted' | 'bordered'
  }>(),
  {
    hoverable: false,
    clickable: false,
    compact: false,
    glass: false,
    variant: 'default',
  },
)
</script>

<style scoped>
.nb-card {
  background: var(--nb-surface);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow-sm);
  border-radius: var(--nb-radius-lg);
  padding: 24px;
  transition: var(--nb-transition);
}

.nb-card--compact {
  padding: 16px;
}

.nb-card--glass {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: var(--nb-shadow-glass);
}

.nb-card--elevated {
  box-shadow: var(--nb-shadow-lg);
}

.nb-card--bordered {
  border: 2px solid var(--nb-primary-light);
  box-shadow: none;
}

.nb-card--hoverable {
  cursor: pointer;
}

.nb-card--hoverable:hover {
  box-shadow: var(--nb-shadow-lg);
  transform: translateY(-2px);
}

.nb-card--clickable {
  cursor: pointer;
}

.nb-card--clickable:hover {
  box-shadow: var(--nb-shadow-lg);
  transform: translateY(-2px);
}

/* 强调色条变体 */
.nb-card--accent {
  border-left: 3px solid var(--nb-accent);
}

.nb-card--success {
  border-left: 3px solid var(--nb-success);
}

.nb-card--warning {
  border-left: 3px solid var(--nb-warning);
}

.nb-card--danger {
  border-left: 3px solid var(--nb-danger);
}

/* AI 卡片 — 渐变玻璃态 */
.nb-card--ai {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(236, 237, 255, 0.6) 100%);
  border: 1px solid rgba(91, 94, 244, 0.15);
  box-shadow: var(--nb-shadow-glass);
}

/* 弱化卡片 */
.nb-card--muted {
  background: var(--nb-muted-surface);
  border-color: var(--nb-border-color-light);
  box-shadow: var(--nb-shadow-xs);
}
</style>
