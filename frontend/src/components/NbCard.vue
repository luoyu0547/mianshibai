<template>
  <div :class="['nb-card', { 'nb-card--hoverable': hoverable }]">
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  hoverable?: boolean
}

defineProps<Props>()
</script>

<style scoped>
.nb-card {
  background: var(--nb-card);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  border-radius: var(--nb-radius-lg);
  padding: 24px;
  transition: var(--nb-transition);
}

.nb-card--hoverable:hover {
  box-shadow: var(--nb-shadow-hover);
  transform: translate(-2px, -2px);
}
</style>
