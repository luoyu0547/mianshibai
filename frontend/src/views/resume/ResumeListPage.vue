<!-- src/views/resume/ResumeListPage.vue -->
<template>
  <MainLayout>
    <div class="resume-list-page">
      <NbPageHeader
        eyebrow="简历工作台"
        title="我的简历"
        description="管理你的求职简历，AI 助你打造完美作品集"
      >
        <template #actions>
          <el-dropdown trigger="click" @command="handleCreateCommand">
            <NbButton variant="primary">+ 新建简历</NbButton>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="blank">新建空白简历</el-dropdown-item>
                <el-dropdown-item command="ai">AI 生成简历</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </NbPageHeader>

      <NbCard v-if="resumeStore.loading">
        <NbLoadingBlock title="加载简历列表..." :rows="4" />
      </NbCard>

      <NbCard v-else-if="resumeStore.resumeList.length === 0">
        <NbEmptyState
          title="还没有简历"
          description="立即创建一份简历，开启你的求职之旅"
        >
          <template #action>
            <NbButton variant="primary" @click="openCreateDialog('blank')">立即创建</NbButton>
          </template>
        </NbEmptyState>
      </NbCard>

      <div v-else class="rlp-grid">
        <NbCard
          v-for="resume in resumeStore.resumeList"
          :key="resume.id"
          hoverable
          class="rlp-card"
          @click="router.push(`/resume/${resume.id}/edit`)"
        >
          <div class="rlp-card__head">
            <div class="rlp-card__top">
              <h3 class="rlp-card__title">{{ resume.title }}</h3>
              <NbStatusBadge
                :label="templateLabel(resume.templateType)"
                variant="muted"
              />
            </div>
            <div class="rlp-card__meta">
              <NbStatusBadge
                :label="resume.status === 'draft' ? '草稿' : '已完成'"
                :variant="resume.status === 'draft' ? 'warning' : 'success'"
              />
              <span class="rlp-card__time">更新于 {{ formatTime(resume.updateTime) }}</span>
            </div>
          </div>
          <div class="rlp-card__actions" @click.stop>
            <NbButton variant="primary" @click="router.push(`/resume/${resume.id}/edit`)">编辑</NbButton>
            <NbButton variant="ghost" @click="router.push(`/resume/${resume.id}/preview`)">预览</NbButton>
            <NbButton variant="ghost" @click="handleDelete(resume.id, resume.title)">删除</NbButton>
          </div>
        </NbCard>
      </div>

      <el-dialog v-model="createDialogVisible" :title="createDialogTitle" width="560px">
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
            <el-form-item label="个人背景描述">
              <el-input
                v-model="createForm.backgroundDescription"
                type="textarea"
                :rows="6"
                placeholder="请简述你的真实教育经历、工作经历、项目经验和技能，以便 AI 基于你的实际情况生成简历&#10;&#10;例如：&#10;XX大学 计算机科学与技术 本科 2022年毕业&#10;某某科技公司 Java后端开发 1年&#10;负责用户模块开发，使用 Spring Boot + MyBatis，日均处理10万请求&#10;熟悉 Java、Spring Boot、MySQL、Redis、Vue"
              />
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <NbButton variant="ghost" @click="createDialogVisible = false">取消</NbButton>
          <NbButton variant="primary" :loading="isCreating" @click="handleCreate">创建</NbButton>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import { useResumeStore } from '@/stores/resume'
import { useUserStore } from '@/stores/user'
import { aiGenerateResume as aiGenerateResumeApi } from '@/api/resume'

const router = useRouter()
const resumeStore = useResumeStore()
const userStore = useUserStore()

const createDialogVisible = ref(false)
const createMode = ref<'blank' | 'ai'>('blank')
const isCreating = ref(false)

const createForm = reactive({
  title: '',
  targetPosition: '',
  techDirection: '',
  workYears: 0,
  backgroundDescription: '',
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
  createForm.targetPosition = userStore.userInfo?.targetPosition || ''
  createForm.techDirection = userStore.userInfo?.techDirection || ''
  createForm.workYears = userStore.userInfo?.workYears || 0
  createForm.backgroundDescription = ''
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
      if (res.code === 0) {
        const id = res.data.id!
        createDialogVisible.value = false
        router.push(`/resume/${id}/edit`)
      }
    } else {
      const res = await aiGenerateResumeApi({
        targetPosition: createForm.targetPosition,
        techDirection: createForm.techDirection || undefined,
        workYears: createForm.workYears || undefined,
        backgroundDescription: createForm.backgroundDescription || undefined,
      })
      if (res.code === 0) {
        const id = res.data.id!
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
    minimal_tech: 'ATS 精英单栏',
    modern_two_col: '技术品牌双栏',
    classic_formal: '长篇正式简历',
  }
  return map[type] || type
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.resume-list-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.rlp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.rlp-card {
  display: flex;
  flex-direction: column;
}

.rlp-card__head {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.rlp-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.rlp-card__title {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 700;
  margin: 0;
  flex: 1;
  min-width: 0;
  word-break: break-word;
  color: var(--nb-ink);
}

.rlp-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rlp-card__time {
  font-size: 12px;
  color: var(--nb-muted-light);
}

.rlp-card__actions {
  display: flex;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--nb-border-color-light);
  flex-wrap: wrap;
}
</style>
