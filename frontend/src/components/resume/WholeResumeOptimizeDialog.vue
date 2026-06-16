<script setup lang="ts">
import { ref } from 'vue'
import { optimizeWholeResume } from '@/api/resume'
import type { ResumeWholeOptimizeVO, SectionVO } from '@/types/resume'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'

const props = defineProps<{
  resumeId: number
}>()

const emit = defineEmits<{
  apply: [sections: SectionVO[]]
}>()

const visible = ref(false)
const loading = ref(false)
const optimizeGoal = ref('')
const result = ref<ResumeWholeOptimizeVO | null>(null)

async function handleOptimize() {
  loading.value = true
  try {
    const res = await optimizeWholeResume(props.resumeId, {
      resumeId: props.resumeId,
      optimizeGoal: optimizeGoal.value || undefined,
    })
    result.value = res.data
  } finally {
    loading.value = false
  }
}

function handleApply() {
  if (result.value) {
    emit('apply', result.value.optimizedSections)
    visible.value = false
    result.value = null
  }
}

function open() {
  visible.value = true
  optimizeGoal.value = ''
  handleOptimize()
}

function close() {
  visible.value = false
  result.value = null
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="整份简历 AI 优化" width="760px" @close="close">
    <div v-if="loading" class="whole-optimize__loading">
      <NbCard variant="ai">
        <NbLoadingBlock title="AI 正在逐模块深度优化您的简历..." :rows="4" />
      </NbCard>
    </div>

    <div v-else-if="result" class="whole-optimize">
      <div class="whole-optimize__scores">
        <div class="whole-optimize__score-card">
          <span>优化前评分</span>
          <strong>{{ result.beforeScore }}</strong>
        </div>
        <div class="whole-optimize__arrow">→</div>
        <div class="whole-optimize__score-card whole-optimize__score-card--after">
          <span>优化后评分</span>
          <strong>{{ result.estimatedAfterScore }}</strong>
        </div>
      </div>

      <div v-if="result.globalSuggestions.length > 0" class="whole-optimize__suggestions">
        <h3>全局优化建议</h3>
        <ol>
          <li v-for="(suggestion, index) in result.globalSuggestions" :key="index">
            {{ suggestion }}
          </li>
        </ol>
      </div>
    </div>

    <div v-else class="whole-optimize__empty">
      <p>暂无优化结果</p>
      <NbButton variant="primary" @click="handleOptimize">重新分析</NbButton>
    </div>

    <template #footer>
      <NbButton variant="ghost" @click="visible = false">取消</NbButton>
      <NbButton variant="primary" :disabled="loading || !result" @click="handleApply">应用优化</NbButton>
    </template>
  </el-dialog>
</template>

<style scoped>
.whole-optimize {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.whole-optimize__scores {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
}

.whole-optimize__score-card {
  padding: 18px;
  border: 1px solid var(--nb-border-color);
  border-radius: var(--nb-radius-lg);
  background: #fff;
}

.whole-optimize__score-card span {
  display: block;
  color: var(--nb-muted);
  font-size: 13px;
}

.whole-optimize__score-card strong {
  display: block;
  margin-top: 4px;
  font-family: var(--font-heading);
  font-size: 34px;
  line-height: 1;
}

.whole-optimize__score-card--after {
  border-color: rgba(63, 109, 246, 0.35);
  background: #edf3ff;
}

.whole-optimize__score-card--after strong {
  color: #2f63ef;
}

.whole-optimize__arrow {
  font-family: var(--font-heading);
  font-size: 22px;
  color: var(--nb-muted-light);
}

.whole-optimize__suggestions {
  padding: 18px;
  border-radius: var(--nb-radius-lg);
  background: #f8fafc;
  border: 1px solid var(--nb-border-color-light);
}

.whole-optimize__suggestions h3 {
  margin: 0 0 10px;
  font-family: var(--font-heading);
  font-size: 16px;
}

.whole-optimize__suggestions ol {
  margin: 0;
  padding-left: 20px;
  color: var(--nb-ink);
  line-height: 1.8;
}

.whole-optimize__empty {
  color: var(--nb-muted);
  text-align: center;
  padding: 32px;
}
</style>
