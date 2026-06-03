<!-- src/views/resume/ResumeListPage.vue -->
<template>
  <MainLayout>
    <div class="resume-list-page">
      <div class="resume-list-page__header">
        <h1 class="resume-list-page__title">我的简历</h1>
        <el-dropdown trigger="click" @command="handleCreateCommand">
          <NbButton type="primary">+ 新建简历</NbButton>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="blank">新建空白简历</el-dropdown-item>
              <el-dropdown-item command="ai">AI 生成简历</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div v-if="resumeStore.loading" class="resume-list-page__loading">
        <el-icon class="is-loading" :size="32"><LoadingIcon /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="resumeStore.resumeList.length === 0" class="resume-list-page__empty">
        <div class="resume-list-page__empty-icon">📄</div>
        <p class="resume-list-page__empty-text">还没有简历，立即创建一份吧！</p>
        <NbButton type="primary" @click="openCreateDialog('blank')">立即创建</NbButton>
      </div>

      <div v-else class="resume-list-page__grid">
        <NbCard
          v-for="resume in resumeStore.resumeList"
          :key="resume.id"
          hoverable
          class="resume-card"
        >
          <div class="resume-card__header">
            <h3 class="resume-card__title">{{ resume.title }}</h3>
            <el-tag class="resume-card__badge" :type="templateTagType(resume.templateType)" size="small">
              {{ templateLabel(resume.templateType) }}
            </el-tag>
          </div>
          <div class="resume-card__meta">
            <span class="resume-card__status" :class="{ 'resume-card__status--draft': resume.status === 'draft' }">
              {{ resume.status === 'draft' ? '草稿' : '已完成' }}
            </span>
            <span class="resume-card__time">{{ formatTime(resume.updateTime) }}</span>
          </div>
          <div class="resume-card__actions">
            <el-button type="primary" text @click="router.push(`/resume/${resume.id}/edit`)">
              编辑
            </el-button>
            <el-button text @click="router.push(`/resume/${resume.id}/preview`)">
              预览
            </el-button>
            <el-button type="danger" text @click="handleDelete(resume.id, resume.title)">
              删除
            </el-button>
          </div>
        </NbCard>
      </div>

      <el-dialog v-model="createDialogVisible" :title="createDialogTitle" width="480px">
        <el-form label-position="top">
          <el-form-item label="简历标题">
            <el-input v-model="createForm.title" placeholder="请输入简历标题" />
          </el-form-item>
          <template v-if="createMode === 'ai'">
            <el-form-item label="目标岗位">
              <el-input v-model="createForm.targetPosition" placeholder="例如：Java 开发工程师" />
            </el-form-item>
            <el-form-item label="技术方向">
              <el-input v-model="createForm.techDirection" placeholder="例如：后端 / 前端 / 全栈" />
            </el-form-item>
            <el-form-item label="工作年限">
              <el-input-number v-model="createForm.workYears" :min="0" :max="60" style="width: 100%;" />
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="createDialogVisible = false">取消</el-button>
          <NbButton type="primary" :loading="isCreating" @click="handleCreate">创建</NbButton>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useResumeStore } from '@/stores/resume'
import { aiGenerateResume as aiGenerateResumeApi } from '@/api/resume'

const router = useRouter()
const resumeStore = useResumeStore()

const createDialogVisible = ref(false)
const createMode = ref<'blank' | 'ai'>('blank')
const isCreating = ref(false)

const createForm = reactive({
  title: '',
  targetPosition: '',
  techDirection: '',
  workYears: 0,
})

const createDialogTitle = computed(() =>
  createMode.value === 'blank' ? '新建空白简历' : 'AI 生成简历',
)

onMounted(() => {
  resumeStore.fetchResumeList()
})

function handleCreateCommand(command: string) {
  openCreateDialog(command as 'blank' | 'ai')
}

function openCreateDialog(mode: 'blank' | 'ai') {
  createMode.value = mode
  createForm.title = ''
  createForm.targetPosition = ''
  createForm.techDirection = ''
  createForm.workYears = 0
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入简历标题')
    return
  }

  isCreating.value = true
  try {
    if (createMode.value === 'blank') {
      const res = await resumeStore.createResume({ title: createForm.title.trim() })
      if (res.data.code === 0) {
        const id = res.data.data.id!
        createDialogVisible.value = false
        router.push(`/resume/${id}/edit`)
      }
    } else {
      const res = await aiGenerateResumeApi({
        targetPosition: createForm.targetPosition,
        techDirection: createForm.techDirection || undefined,
        workYears: createForm.workYears || undefined,
      })
      if (res.data.code === 0) {
        const id = res.data.data.id!
        createDialogVisible.value = false
        router.push(`/resume/${id}/edit`)
      }
    }
  } finally {
    isCreating.value = false
  }
}

async function handleDelete(id: number, title: string) {
  try {
    await ElMessageBox.confirm(`确定删除简历「${title}」吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const success = await resumeStore.deleteResume(id)
    if (success) {
      ElMessage.success('删除成功')
    }
  } catch {
    // cancelled
  }
}

function templateLabel(type: string) {
  const map: Record<string, string> = {
    minimal_tech: '简约技术风',
    modern_two_col: '现代双栏',
    classic_formal: '经典正式',
  }
  return map[type] || type
}

function templateTagType(type: string) {
  const map: Record<string, string> = {
    minimal_tech: '',
    modern_two_col: 'success',
    classic_formal: 'warning',
  }
  return (map[type] || '') as '' | 'success' | 'warning'
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.resume-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.resume-list-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.resume-list-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.resume-list-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--nb-muted);
}

.resume-list-page__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 64px 0;
}

.resume-list-page__empty-icon {
  font-size: 64px;
}

.resume-list-page__empty-text {
  font-size: 16px;
  color: var(--nb-muted);
  margin: 0;
}

.resume-list-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.resume-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.resume-card__title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.resume-card__badge {
  border: var(--nb-border);
  box-shadow: 2px 2px 0 var(--nb-border);
  flex-shrink: 0;
}

.resume-card__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--nb-muted);
}

.resume-card__status {
  padding: 2px 8px;
  border-radius: var(--nb-radius);
  background: var(--nb-success);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.resume-card__status--draft {
  background: var(--nb-warning);
  color: var(--nb-text);
}

.resume-card__actions {
  display: flex;
  gap: 8px;
  border-top: 2px solid var(--nb-bg);
  padding-top: 12px;
}
</style>
