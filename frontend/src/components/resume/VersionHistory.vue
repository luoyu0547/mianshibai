<!-- src/components/resume/VersionHistory.vue -->
<template>
  <div class="version-history">
    <NbButton variant="ghost" @click="drawerVisible = true">
      <el-icon><Clock /></el-icon>
      版本历史
    </NbButton>

    <el-drawer v-model="drawerVisible" title="版本历史" direction="rtl" size="380px">
      <NbLoadingBlock v-if="loading" title="加载版本记录..." :rows="4" />

      <div v-else-if="versions.length" class="version-history__timeline">
        <div
          v-for="(version, index) in versions"
          :key="version.id"
          class="version-history__entry"
        >
          <div class="version-history__node">
            <span class="version-history__dot" :class="{ 'version-history__dot--latest': index === 0 }"></span>
            <span v-if="index < versions.length - 1" class="version-history__line"></span>
          </div>
          <div class="version-history__content">
            <div class="version-history__entry-head">
              <span class="version-history__version">v{{ version.version }}</span>
              <span v-if="index === 0" class="version-history__latest-tag">当前</span>
            </div>
            <p class="version-history__summary">{{ version.changeSummary || '自动保存' }}</p>
            <span class="version-history__time">{{ formatTime(version.createTime) }}</span>
          </div>
        </div>
      </div>

      <NbEmptyState
        v-else
        title="暂无版本记录"
        description="保存简历后会自动生成版本快照"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import { getResumeVersions } from '@/api/resume'
import type { VersionVO } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'

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
      if (res.code === 0) {
        versions.value = res.data || []
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
.version-history__timeline {
  padding: 8px 0;
}

.version-history__entry {
  display: flex;
  gap: 14px;
  padding-bottom: 4px;
}

.version-history__node {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  padding-top: 4px;
}

.version-history__dot {
  width: 14px;
  height: 14px;
  border: var(--nb-border);
  border-radius: 50%;
  background: var(--nb-surface);
  flex-shrink: 0;
}

.version-history__dot--latest {
  background: var(--nb-primary);
}

.version-history__line {
  width: 2px;
  flex: 1;
  min-height: 32px;
  background: var(--nb-border-color);
  opacity: 0.3;
  margin-top: 4px;
}

.version-history__content {
  flex: 1;
  min-width: 0;
  padding-bottom: 20px;
}

.version-history__entry-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.version-history__version {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 15px;
  color: var(--nb-primary);
}

.version-history__latest-tag {
  display: inline-block;
  padding: 1px 8px;
  background: rgba(108, 92, 231, 0.12);
  color: var(--nb-primary);
  border: 1px solid var(--nb-primary);
  border-radius: var(--nb-radius-sm);
  font-size: 11px;
  font-weight: 600;
}

.version-history__summary {
  font-size: 13px;
  line-height: 1.5;
  color: var(--nb-ink);
  margin: 0 0 4px;
}

.version-history__time {
  font-size: 12px;
  color: var(--nb-muted);
}
</style>
