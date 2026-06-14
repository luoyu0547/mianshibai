<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = withDefaults(defineProps<{
  option: EChartsOption
  height?: string
  title?: string
}>(), {
  height: '320px',
  title: undefined,
})

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  chart.setOption(props.option)
  resizeObserver = new ResizeObserver(resizeChart)
  resizeObserver.observe(chartEl.value)
})

watch(
  () => props.option,
  (option) => {
    chart?.setOption(option, true)
  },
  { deep: true },
)

onUnmounted(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div class="chart-shell">
    <div v-if="title" class="chart-shell__title">{{ title }}</div>
    <div ref="chartEl" class="base-chart" :style="{ height: height }" />
  </div>
</template>

<style scoped>
.chart-shell {
  border: var(--nb-border);
  border-radius: var(--nb-radius-lg);
  box-shadow: var(--nb-shadow);
  background: var(--nb-surface);
  padding: 16px;
}

.chart-shell__title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  color: var(--nb-ink);
  margin-bottom: 12px;
}

.base-chart {
  width: 100%;
  min-height: 240px;
}
</style>
