<template>
  <div class="nb-page-header">
    <div class="nb-page-header__main">
      <span v-if="eyebrow" class="nb-page-header__eyebrow">{{ eyebrow }}</span>
      <h1 class="nb-page-header__title">{{ title }}</h1>
      <p v-if="description" class="nb-page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="nb-page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description?: string
  }>(),
  {
    eyebrow: undefined,
    title: '',
    description: undefined,
  },
)
</script>

<style scoped>
.nb-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding-bottom: 20px;
}

.nb-page-header__main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.nb-page-header__eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--nb-primary);
}

.nb-page-header__title {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--nb-ink);
  letter-spacing: -0.3px;
}

.nb-page-header__description {
  margin: 4px 0 0;
  font-family: var(--font-body);
  font-size: 15px;
  line-height: 1.5;
  color: var(--nb-muted);
  max-width: 60ch;
}

.nb-page-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
</style>
