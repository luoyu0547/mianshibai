<template>
  <div :class="['nb-empty-state', `nb-empty-state--${variant}`]">
    <div class="nb-empty-state__illustration">
      <slot name="illustration">
        <div class="nb-empty-state__decor"></div>
      </slot>
    </div>
    <h3 class="nb-empty-state__title">{{ title }}</h3>
    <p v-if="description" class="nb-empty-state__description">{{ description }}</p>
    <div v-if="$slots.action" class="nb-empty-state__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    variant?: 'default' | 'ai'
  }>(),
  {
    title: '',
    description: undefined,
    variant: 'default',
  },
)
</script>

<style scoped>
.nb-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 48px;
  gap: 16px;
}

.nb-empty-state__illustration {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.nb-empty-state__decor {
  width: 64px;
  height: 64px;
  border: var(--nb-border);
  border-style: dashed;
  border-radius: var(--nb-radius);
  background: var(--nb-muted-surface);
  position: relative;
}

.nb-empty-state__decor::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 20px;
  height: 20px;
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  background: var(--nb-surface);
  transform: translate(-50%, -50%) rotate(45deg);
}

.nb-empty-state__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  color: var(--nb-ink);
}

.nb-empty-state__description {
  margin: 0;
  font-family: var(--font-body);
  font-size: 15px;
  line-height: 1.5;
  color: var(--nb-muted);
  max-width: 48ch;
}

.nb-empty-state__action {
  margin-top: 8px;
}

.nb-empty-state--ai {
  background: linear-gradient(135deg, var(--nb-surface) 0%, rgba(253, 121, 168, 0.06) 100%);
  border-radius: var(--nb-radius-lg);
}

.nb-empty-state--ai .nb-empty-state__decor {
  border-color: var(--nb-accent);
  background: rgba(253, 121, 168, 0.08);
}

.nb-empty-state--ai .nb-empty-state__decor::after {
  border-color: var(--nb-accent);
}
</style>
