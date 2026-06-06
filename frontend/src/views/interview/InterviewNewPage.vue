<template>
  <MainLayout>
    <div class="interview-new-page">
      <div class="interview-new-page__header">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="interview-new-page__title">开始新面试</h1>
      </div>

      <NbCard class="interview-new-page__form-card">
        <el-form label-position="top" class="interview-new-page__form">
          <el-form-item label="选择简历" required>
            <el-select
              v-model="form.resumeId"
              placeholder="请选择一份简历"
              style="width: 100%;"
              :loading="resumeStore.loading"
              @change="handleResumeChange"
            >
              <el-option
                v-for="resume in resumeStore.resumeList"
                :key="resume.id"
                :label="resume.title"
                :value="resume.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="目标岗位" required>
            <el-input
              v-model="form.targetPosition"
              placeholder="例如：Java 开发工程师"
            />
          </el-form-item>

          <el-form-item label="技术方向">
            <el-input
              v-model="form.techDirection"
              placeholder="例如：后端 / 前端 / 全栈"
            />
          </el-form-item>

          <div class="interview-new-page__info-box">
            <el-icon :size="18"><InfoFilled /></el-icon>
            <span>本次面试包含 <strong>5</strong> 道技术主问题，每题最多 <strong>1</strong> 次追问，使用语音面试</span>
          </div>

          <NbButton
            type="primary"
            block
            :loading="submitting"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            {{ submitting ? '创建中...' : '开始面试' }}
          </NbButton>
        </el-form>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useInterviewStore } from '@/stores/interview'
import { useResumeStore } from '@/stores/resume'

const router = useRouter()
const interviewStore = useInterviewStore()
const resumeStore = useResumeStore()

const submitting = ref(false)

const form = reactive({
  resumeId: null as number | null,
  targetPosition: '',
  techDirection: '',
})

const canSubmit = computed(() => form.resumeId !== null && form.targetPosition.trim().length > 0)

onMounted(() => {
  resumeStore.fetchResumeList()
})

function handleResumeChange(resumeId: number) {
  const resume = resumeStore.resumeList.find((r) => r.id === resumeId)
  if (resume) {
    interviewStore.fetchSession(resumeId)
  }
}

async function handleSubmit() {
  if (!form.resumeId || !form.targetPosition.trim()) {
    ElMessage.warning('请选择简历并填写目标岗位')
    return
  }

  submitting.value = true
  try {
    const res = await interviewStore.createSession({
      resumeId: form.resumeId,
      targetPosition: form.targetPosition.trim(),
      techDirection: form.techDirection.trim() || undefined,
    })
    if (res.data.code === 0) {
      const id = res.data.data.id!
      router.push(`/interview/${id}/room`)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.interview-new-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.interview-new-page__header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-new-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.interview-new-page__form-card {
  padding: 32px;
}

.interview-new-page__form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.interview-new-page__info-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 16px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  box-shadow: 2px 2px 0 var(--nb-border);
  font-size: 14px;
  color: var(--nb-muted);
  line-height: 1.6;
}

.interview-new-page__info-box strong {
  color: var(--nb-primary);
}
</style>
