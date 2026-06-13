<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <div>
          <p class="admin-page__eyebrow">User Detail</p>
          <h1>{{ user?.userName || user?.userAccount || '用户详情' }}</h1>
        </div>
        <el-button @click="router.push('/admin/users')">返回列表</el-button>
      </div>

      <el-skeleton v-if="adminStore.loading && !user" :rows="6" animated />
      <template v-else-if="user">
        <NbCard class="profile-card">
          <div>
            <span>账号</span>
            <strong>{{ user.userAccount }}</strong>
          </div>
          <div>
            <span>角色</span>
            <el-select v-model="selectedRole" @change="changeRole">
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </div>
          <div>
            <span>状态</span>
            <el-tag :type="user.userStatus === 0 ? 'success' : 'warning'">{{ user.userStatus === 0 ? '正常' : '禁用' }}</el-tag>
          </div>
          <div class="profile-card__actions">
            <el-button v-if="user.userStatus === 0" type="warning" @click="disableUser">禁用用户</el-button>
            <el-button v-else type="success" @click="enableUser">启用用户</el-button>
          </div>
        </NbCard>

        <div class="metric-grid">
          <NbCard v-for="metric in metrics" :key="metric.label" class="metric-card">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </NbCard>
        </div>

        <NbCard class="info-card">
          <h2>基础信息</h2>
          <dl>
            <dt>邮箱</dt><dd>{{ user.email || '-' }}</dd>
            <dt>手机号</dt><dd>{{ user.phone || '-' }}</dd>
            <dt>目标岗位</dt><dd>{{ user.targetPosition || '-' }}</dd>
            <dt>技术方向</dt><dd>{{ user.techDirection || '-' }}</dd>
            <dt>工作年限</dt><dd>{{ user.workYears ?? '-' }}</dd>
            <dt>城市</dt><dd>{{ user.city || '-' }}</dd>
            <dt>求职状态</dt><dd>{{ user.jobStatus || '-' }}</dd>
            <dt>注册时间</dt><dd>{{ user.createTime || '-' }}</dd>
          </dl>
        </NbCard>
      </template>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const selectedRole = ref('user')

const userId = computed(() => Number(route.params.id))
const user = computed(() => adminStore.currentUser)

const metrics = computed(() => {
  const current = user.value
  return [
    { label: '简历', value: current?.resumeCount ?? 0 },
    { label: '面试', value: current?.interviewCount ?? 0 },
    { label: '已完成面试', value: current?.completedInterviewCount ?? 0 },
    { label: '投递', value: current?.applicationCount ?? 0 },
    { label: '训练计划', value: current?.trainingPlanCount ?? 0 },
    { label: '八股作答', value: current?.trainingAnswerCount ?? 0 },
    { label: 'AI 批改', value: current?.trainingReviewCount ?? 0 },
  ]
})

watch(user, (value) => {
  if (value) {
    selectedRole.value = value.userRole
  }
})

async function loadUser() {
  await adminStore.fetchUser(userId.value)
}

async function changeRole(role: string) {
  await adminStore.updateUserRole(userId.value, role)
  ElMessage.success('角色已更新')
}

async function disableUser() {
  await adminStore.disableUser(userId.value)
  ElMessage.success('已禁用用户')
}

async function enableUser() {
  await adminStore.enableUser(userId.value)
  ElMessage.success('已启用用户')
}

onMounted(loadUser)
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-page__header h1 {
  margin: 0;
  font-size: 34px;
}

.admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--nb-primary);
  font-weight: 800;
  text-transform: uppercase;
}

.profile-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  align-items: center;
}

.profile-card span,
.metric-card span {
  display: block;
  margin-bottom: 6px;
  color: var(--nb-muted);
  font-weight: 700;
}

.profile-card strong,
.metric-card strong {
  font-size: 26px;
}

.profile-card__actions {
  display: flex;
  justify-content: flex-end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.info-card h2 {
  margin: 0 0 16px;
}

.info-card dl {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px 18px;
  margin: 0;
}

.info-card dt {
  color: var(--nb-muted);
  font-weight: 700;
}

.info-card dd {
  margin: 0;
}

@media (max-width: 900px) {
  .profile-card {
    grid-template-columns: 1fr;
  }

  .profile-card__actions {
    justify-content: flex-start;
  }
}
</style>
