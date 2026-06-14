<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="User Detail"
        :title="user?.userName || user?.userAccount || '用户详情'"
      >
        <template #actions>
          <NbButton @click="router.push('/admin/users')">返回列表</NbButton>
        </template>
      </NbPageHeader>

      <NbCard v-if="adminStore.loading && !user">
        <NbLoadingBlock title="加载用户信息..." :rows="6" />
      </NbCard>

      <template v-else-if="user">
        <NbCard>
          <div class="profile">
            <div class="profile__field">
              <span class="profile__label">账号</span>
              <strong class="profile__value">{{ user.userAccount }}</strong>
            </div>
            <div class="profile__field">
              <span class="profile__label">角色</span>
              <el-select v-model="selectedRole" @change="changeRole">
                <el-option label="普通用户" value="user" />
                <el-option label="管理员" value="admin" />
              </el-select>
            </div>
            <div class="profile__field">
              <span class="profile__label">状态</span>
              <NbStatusBadge
                :label="getStatusDescriptor(userStatusMap, user.userStatus).label"
                :variant="getStatusDescriptor(userStatusMap, user.userStatus).variant"
              />
            </div>
            <div class="profile__actions">
              <NbButton v-if="user.userStatus === 0" variant="warning" @click="disableUser">禁用用户</NbButton>
              <NbButton v-else variant="success" @click="enableUser">启用用户</NbButton>
            </div>
          </div>
        </NbCard>

        <div class="metric-grid">
          <NbStatCard
            v-for="metric in metrics"
            :key="metric.key"
            :label="metric.label"
            :value="metric.value"
          />
        </div>

        <NbCard>
          <NbSectionTitle title="基础信息" />
          <dl class="info-grid">
            <dt>邮箱</dt><dd>{{ displayText(user.email) }}</dd>
            <dt>手机号</dt><dd>{{ displayText(user.phone) }}</dd>
            <dt>目标岗位</dt><dd>{{ displayText(user.targetPosition) }}</dd>
            <dt>技术方向</dt><dd>{{ displayText(user.techDirection) }}</dd>
            <dt>工作年限</dt><dd>{{ user.workYears != null ? user.workYears : '-' }}</dd>
            <dt>城市</dt><dd>{{ displayText(user.city) }}</dd>
            <dt>求职状态</dt><dd>{{ displayText(user.jobStatus) }}</dd>
            <dt>注册时间</dt><dd>{{ formatDateTime(user.createTime) }}</dd>
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
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbSectionTitle from '@/components/NbSectionTitle.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbStatCard from '@/components/NbStatCard.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import { useAdminStore } from '@/stores/admin'
import { userStatusMap, getStatusDescriptor } from '@/utils/statusMaps'
import { displayText } from '@/utils/text'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const selectedRole = ref('user')

const userId = computed(() => Number(route.params.id))
const user = computed(() => adminStore.currentUser)

const metrics = computed(() => {
  const current = user.value
  return [
    { key: 'resume', label: '简历', value: current?.resumeCount ?? 0 },
    { key: 'interview', label: '面试', value: current?.interviewCount ?? 0 },
    { key: 'completed', label: '已完成面试', value: current?.completedInterviewCount ?? 0 },
    { key: 'application', label: '投递', value: current?.applicationCount ?? 0 },
    { key: 'trainingPlan', label: '训练计划', value: current?.trainingPlanCount ?? 0 },
    { key: 'trainingAnswer', label: '八股作答', value: current?.trainingAnswerCount ?? 0 },
    { key: 'trainingReview', label: 'AI 批改', value: current?.trainingReviewCount ?? 0 },
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
  const result = await adminStore.updateUserRole(userId.value, role)
  if (result) {
    ElMessage.success('角色已更新')
  } else {
    ElMessage.error('角色更新失败，请重试')
  }
}

async function disableUser() {
  const result = await adminStore.disableUser(userId.value)
  if (result) {
    ElMessage.success('已禁用用户')
  } else {
    ElMessage.error('禁用失败，请重试')
  }
}

async function enableUser() {
  const result = await adminStore.enableUser(userId.value)
  if (result) {
    ElMessage.success('已启用用户')
  } else {
    ElMessage.error('启用失败，请重试')
  }
}

onMounted(loadUser)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  align-items: center;
}

.profile__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile__label {
  color: var(--nb-muted);
  font-weight: 700;
  font-size: 13px;
}

.profile__value {
  font-size: 18px;
}

.profile__actions {
  display: flex;
  justify-content: flex-end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px 18px;
  margin: 16px 0 0;
}

.info-grid dt {
  color: var(--nb-muted);
  font-weight: 700;
}

.info-grid dd {
  margin: 0;
}

@media (max-width: 900px) {
  .profile {
    grid-template-columns: 1fr;
  }

  .profile__actions {
    justify-content: flex-start;
  }
}
</style>
