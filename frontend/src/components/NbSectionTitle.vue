<template>
  <div class="nb-section-title">
    <div class="nb-section-title__main">
      <h2 class="nb-section-title__heading">{{ title }}</h2>
      <p v-if="description" class="nb-section-title__description">{{ description }}</p>
    </div>
    <div class="nb-section-title__side">
      <div v-if="$slots.meta" class="nb-section-title__meta">
        <slot name="meta" />
      </div>
      <div v-if="$slots.actions" class="nb-section-title__actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
  }>(),
  {
    title: '',
    description: undefined,
  },
)
</script>

<style scoped>
.nb-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--nb-border-color-light);
  flex-wrap: wrap;
}

.nb-section-title__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.nb-section-title__heading {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 700;
  color: var(--nb-ink);
  letter-spacing: -0.2px;
}

.nb-section-title__description {
  margin: 0;
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--nb-muted);
}

.nb-section-title__side {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.nb-section-title__meta {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--nb-muted);
}

.nb-section-title__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
