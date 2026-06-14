import type { EChartsOption } from 'echarts'

const RADAR_LABELS: Record<string, string> = {
  accuracy: '技术准确性',
  clarity: '表达清晰度',
  depth: '项目深度',
  matching: '岗位匹配度',
  systemDesign: '系统设计',
}

const COLOR_PRIMARY = '#6C5CE7'
const COLOR_SECONDARY = '#00CEC9'
const COLOR_INK = '#2D3436'
const COLOR_MUTED = '#6B7280'

const AXIS_LINE_STYLE = { lineStyle: { color: COLOR_INK } }
const AXIS_LABEL_STYLE = { color: COLOR_MUTED }
const TOOLTIP_STYLE = {
  backgroundColor: '#FFFFFF',
  borderColor: COLOR_INK,
  borderWidth: 2,
  textStyle: { color: COLOR_INK },
}

export function radarLabel(key: string) {
  return RADAR_LABELS[key] ?? key
}

export function buildRadarOption(radar: Record<string, number>, title = '能力雷达'): EChartsOption {
  const keys = Object.keys(radar)
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 16, color: COLOR_INK } },
    tooltip: TOOLTIP_STYLE,
    radar: {
      indicator: keys.map((key) => ({ name: radarLabel(key), max: 100 })),
      radius: '62%',
      axisName: { color: COLOR_MUTED },
      axisLine: AXIS_LINE_STYLE,
      splitLine: { lineStyle: { color: COLOR_MUTED, opacity: 0.3 } },
    },
    series: [
      {
        type: 'radar',
        data: [{ value: keys.map((key) => radar[key]), name: title }],
        areaStyle: { color: 'rgba(108, 92, 231, 0.2)' },
        lineStyle: { color: COLOR_PRIMARY, width: 2 },
        itemStyle: { color: COLOR_PRIMARY },
      },
    ],
  }
}

export function buildScoreTrendOption(items: Array<Record<string, string | number>>): EChartsOption {
  return {
    title: { text: '近期面试分数', left: 'center', textStyle: { fontSize: 16, color: COLOR_INK } },
    tooltip: { trigger: 'axis', ...TOOLTIP_STYLE },
    xAxis: {
      type: 'category',
      data: items.map((item) => String(item.date)),
      axisLine: AXIS_LINE_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: AXIS_LINE_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
      splitLine: { lineStyle: { color: COLOR_MUTED, opacity: 0.2 } },
    },
    series: [{
      type: 'line',
      smooth: true,
      data: items.map((item) => Number(item.score)),
      itemStyle: { color: COLOR_PRIMARY },
      lineStyle: { color: COLOR_PRIMARY, width: 2 },
      areaStyle: { color: 'rgba(108, 92, 231, 0.1)' },
    }],
  }
}

export function buildSkillGapOption(items: Array<Record<string, string>>): EChartsOption {
  return {
    title: { text: 'Top 技能缺口', left: 'center', textStyle: { fontSize: 16, color: COLOR_INK } },
    tooltip: { trigger: 'axis', ...TOOLTIP_STYLE },
    xAxis: {
      type: 'category',
      data: items.map((item) => item.name ?? ''),
      axisLine: AXIS_LINE_STYLE,
      axisLabel: { interval: 0, rotate: 24, color: COLOR_MUTED },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: AXIS_LINE_STYLE,
      axisLabel: AXIS_LABEL_STYLE,
      splitLine: { lineStyle: { color: COLOR_MUTED, opacity: 0.2 } },
    },
    series: [{
      type: 'bar',
      data: items.map((item) => Number(item.count ?? 0)),
      itemStyle: { color: COLOR_SECONDARY },
    }],
  }
}
