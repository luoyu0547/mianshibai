<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <div>
          <p class="admin-page__eyebrow">Users</p>
          <h1>用户管理</h1>
        </div>
        <el-button type="primary" @click="loadUsers">刷新</el-button>
      </div>

      <NbCard class="filter-card">
        <el-input v-model="query.keyword" placeholder="搜索账号、昵称、邮箱" clearable @keyup.enter="loadUsers" />
        <el-select v-model="query.userStatus" placeholder="状态" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
        <el-select v-model="query.userRole" placeholder="角色" clearable>
          <el-option label="普通用户" value="user" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </NbCard>

      <NbCard>
        <el-table :data="adminStore.users" v-loading="adminStore.loading" empty-text="暂无用户">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="账号" min-width="140" />
          <el-table-column prop="userName" label="昵称" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="userRole" label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.userRole === 'admin' ? 'danger' : 'info'">{{ roleLabel(row.userRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userStatus" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.userStatus === 0 ? 'success' : 'warning'">{{ statusLabel(row.userStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetPosition" label="目标岗位" min-width="140" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="router.push(`/admin/users/${row.id}`)">详情</el-button>
              <el-button v-if="row.userStatus === 0" size="small" type="warning" @click="disableUser(row.id)">禁用</el-button>
              <el-button v-else size="small" type="success" @click="enableUser(row.id)">启用</el-button>
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
import { useAdminStore } from '@/stores/admin'
import type { AdminUserQueryRequest } from '@/types/admin'

const router = useRouter()
const adminStore = useAdminStore()
const query = reactive<AdminUserQueryRequest>({})

function roleLabel(role: string) {
  return role === 'admin' ? '管理员' : '普通用户'
}

function statusLabel(status: number) {
  return status === 0 ? '正常' : '禁用'
}

async function loadUsers() {
  await adminStore.fetchUsers({ ...query })
}

async function disableUser(id: number) {
  await adminStore.disableUser(id)
  ElMessage.success('已禁用用户')
  await loadUsers()
}

async function enableUser(id: number) {
  await adminStore.enableUser(id)
  ElMessage.success('已启用用户')
  await loadUsers()
}

onMounted(loadUsers)
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

.filter-card {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 160px auto;
  gap: 12px;
  align-items: center;
}

@media (max-width: 900px) {
  .filter-card {
    grid-template-columns: 1fr;
  }
}
</style>
