<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Users"
        title="用户管理"
      >
        <template #actions>
          <NbButton variant="primary" @click="loadUsers">刷新</NbButton>
        </template>
      </NbPageHeader>

      <NbCard class="filter-card">
        <div class="filter-row">
          <el-input v-model="query.keyword" placeholder="搜索账号、昵称、邮箱" clearable @keyup.enter="loadUsers" />
          <el-select v-model="query.userStatus" placeholder="状态" clearable>
            <el-option label="正常" :value="0" />
            <el-option label="禁用" :value="1" />
          </el-select>
          <el-select v-model="query.userRole" placeholder="角色" clearable>
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
          <NbButton variant="primary" @click="loadUsers">查询</NbButton>
        </div>
      </NbCard>

      <NbCard v-if="adminStore.loading">
        <NbLoadingBlock title="加载用户列表..." :rows="6" />
      </NbCard>

      <NbCard v-else>
        <el-table :data="adminStore.users" empty-text="暂无用户">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="账号" min-width="140" />
          <el-table-column prop="userName" label="昵称" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="userRole" label="角色" width="120">
            <template #default="{ row }">
              <NbStatusBadge
                :label="getStatusDescriptor(userRoleMap, row.userRole).label"
                :variant="getStatusDescriptor(userRoleMap, row.userRole).variant"
              />
            </template>
          </el-table-column>
          <el-table-column prop="userStatus" label="状态" width="110">
            <template #default="{ row }">
              <NbStatusBadge
                :label="getStatusDescriptor(userStatusMap, row.userStatus).label"
                :variant="getStatusDescriptor(userStatusMap, row.userStatus).variant"
              />
            </template>
          </el-table-column>
          <el-table-column prop="targetPosition" label="目标岗位" min-width="140" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <NbButton @click="router.push(`/admin/users/${row.id}`)">详情</NbButton>
              <NbButton v-if="row.userStatus === 0" variant="warning" @click="disableUser(row.id)">禁用</NbButton>
              <NbButton v-else variant="success" @click="enableUser(row.id)">启用</NbButton>
            </template>
          </el-table-column>
        </el-table>
      </NbCard>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import NbLoadingBlock from '@/components/NbLoadingBlock.vue'
import { useAdminStore } from '@/stores/admin'
import type { AdminUserQueryRequest } from '@/types/admin'
import { userRoleMap, userStatusMap, getStatusDescriptor } from '@/utils/statusMaps'

const router = useRouter()
const adminStore = useAdminStore()
const query = reactive<AdminUserQueryRequest>({})

async function loadUsers() {
  await adminStore.fetchUsers({ ...query })
}

async function disableUser(id: number) {
  const result = await adminStore.disableUser(id)
  if (result) {
    ElMessage.success('已禁用用户')
    await loadUsers()
  } else {
    ElMessage.error('禁用失败，请重试')
  }
}

async function enableUser(id: number) {
  const result = await adminStore.enableUser(id)
  if (result) {
    ElMessage.success('已启用用户')
    await loadUsers()
  } else {
    ElMessage.error('启用失败，请重试')
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 160px auto;
  gap: 12px;
  align-items: center;
}

@media (max-width: 900px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
