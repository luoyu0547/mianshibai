<template>
  <NbCard :class="['nb-stat-card', variant !== 'default' && `nb-stat-card--${variant}`]">
    <div class="nb-stat-card__top">
      <div class="nb-stat-card__label-row">
        <span v-if="$slots.icon" class="nb-stat-card__icon">
          <slot name="icon" />
        </span>
        <span class="nb-stat-card__label">{{ label }}</span>
      </div>
      <div v-if="$slots.action" class="nb-stat-card__action">
        <slot name="action" />
      </div>
    </div>
    <div class="nb-stat-card__value">{{ value }}</div>
    <div v-if="hint" class="nb-stat-card__hint">{{ hint }}</div>
  </NbCard>
</template>

<script setup lang="ts">
import NbCard from '@/components/NbCard.vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'accent'
  }>(),
  {
    label: '',
    value: '',
    hint: undefined,
    variant: 'default',
  },
)
</script>

<style scoped>
.nb-stat-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nb-stat-card__label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.nb-stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--nb-muted-light);
}

.nb-stat-card--primary .nb-stat-card__icon { color: var(--nb-primary); }
.nb-stat-card--success .nb-stat-card__icon { color: var(--nb-success); }
.nb-stat-card--warning .nb-stat-card__icon { color: var(--nb-warning); }
.nb-stat-card--danger .nb-stat-card__icon { color: var(--nb-danger); }
.nb-stat-card--accent .nb-stat-card__icon { color: var(--nb-accent); }

.nb-stat-card__label {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--nb-muted);
}

.nb-stat-card__action {
  flex-shrink: 0;
}

.nb-stat-card__value {
  font-family: var(--font-heading);
  font-size: 30px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--nb-ink);
  margin-top: 10px;
  letter-spacing: -0.5px;
}

.nb-stat-card__hint {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--nb-muted);
  margin-top: 4px;
}

/* 顶部强调色条 */
.nb-stat-card--primary { border-top: 3px solid var(--nb-primary) !important; }
.nb-stat-card--success { border-top: 3px solid var(--nb-success) !important; }
.nb-stat-card--warning { border-top: 3px solid var(--nb-warning) !important; }
.nb-stat-card--danger { border-top: 3px solid var(--nb-danger) !important; }
.nb-stat-card--accent { border-top: 3px solid var(--nb-accent) !important; }
</style>
