<!-- src/components/resume/VersionHistory.vue -->
<template>
  <div class="version-history">
    <el-button @click="drawerVisible = true">
      <el-icon><Clock /></el-icon>
      版本历史
    </el-button>

    <el-drawer v-model="drawerVisible" title="版本历史" direction="rtl" size="360px">
      <div v-if="loading" class="version-history__loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <el-timeline v-else-if="versions.length" class="version-history__timeline">
        <el-timeline-item
          v-for="version in versions"
          :key="version.id"
          :timestamp="formatTime(version.createTime)"
          placement="top"
        >
          <div class="version-history__item">
            <span class="version-history__version">v{{ version.version }}</span>
            <span class="version-history__summary">{{ version.changeSummary || '自动保存' }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>

      <div v-else class="version-history__empty">
        <p>暂无版本记录</p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Clock, Loading } from '@element-plus/icons-vue'
import { getResumeVersions } from '@/api/resume'
import type { VersionVO } from '@/types/resume'

const props = defineProps<{
  resumeId: number
}>()

const drawerVisible = ref(false)
const loading = ref(false)
const versions = ref<VersionVO[]>([])

watch(drawerVisible, async (visible) => {
  if (visible && props.resumeId) {
    loading.value = true
    try {
      const res = await getResumeVersions(props.resumeId)
      if (res.data.code === 0) {
        versions.value = res.data.data || []
      }
    } finally {
      loading.value = false
    }
  }
})

function formatTime(time: string): string {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.version-history__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  padding: 32px 0;
  color: var(--nb-muted);
}

.version-history__timeline {
  padding: 8px 0;
}

.version-history__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.version-history__version {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 14px;
  color: var(--nb-primary);
}

.version-history__summary {
  font-size: 13px;
  color: var(--nb-text);
}

.version-history__empty {
  text-align: center;
  padding: 32px 0;
  color: var(--nb-muted);
  font-size: 14px;
}
</style>
