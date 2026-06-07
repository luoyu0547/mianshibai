import type { EChartsOption } from 'echarts'

const RADAR_LABELS: Record<string, string> = {
  accuracy: '技术准确性',
  clarity: '表达清晰度',
  depth: '项目深度',
  matching: '岗位匹配度',
  systemDesign: '系统设计',
}

export function radarLabel(key: string) {
  return RADAR_LABELS[key] ?? key
}

export function buildRadarOption(radar: Record<string, number>, title = '能力雷达'): EChartsOption {
  const keys = Object.keys(radar)
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 16 } },
    tooltip: {},
    radar: {
      indicator: keys.map((key) => ({ name: radarLabel(key), max: 100 })),
      radius: '62%',
    },
    series: [
      {
        type: 'radar',
        data: [{ value: keys.map((key) => radar[key]), name: title }],
        areaStyle: { opacity: 0.18 },
      },
    ],
  }
}

export function buildScoreTrendOption(items: Array<Record<string, string | number>>): EChartsOption {
  return {
    title: { text: '近期面试分数', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: items.map((item) => String(item.date)) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ type: 'line', smooth: true, data: items.map((item) => Number(item.score)), itemStyle: { color: '#6C5CE7' } }],
  }
}

export function buildSkillGapOption(items: Array<Record<string, string>>): EChartsOption {
  return {
    title: { text: 'Top 技能缺口', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: items.map((item) => item.name ?? ''), axisLabel: { interval: 0, rotate: 24 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: items.map((item) => Number(item.count ?? 0)), itemStyle: { color: '#FDCB6E' } }],
  }
}
