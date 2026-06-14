<!-- src/views/auth/LoginPage.vue -->
<template>
  <AuthLayout>
    <NbCard class="login-card">
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录 Tab -->
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            class="login-form"
          >
            <el-form-item label="账号" prop="userAccount">
              <el-input
                v-model="loginForm.userAccount"
                placeholder="请输入账号（4-32位字母/数字/下划线）"
                size="large"
              />
            </el-form-item>

            <el-form-item label="密码" prop="userPassword">
              <el-input
                v-model="loginForm.userPassword"
                type="password"
                placeholder="请输入密码（8-64位）"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <NbButton
                type="primary"
                block
                :loading="isLoading"
                @click="handleLogin"
              >
                登录
              </NbButton>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 Tab -->
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            class="login-form"
          >
            <el-form-item label="账号" prop="userAccount">
              <el-input
                v-model="registerForm.userAccount"
                placeholder="请输入账号（4-32位字母/数字/下划线）"
                size="large"
              />
            </el-form-item>

            <el-form-item label="密码" prop="userPassword">
              <el-input
                v-model="registerForm.userPassword"
                type="password"
                placeholder="请输入密码（8-64位）"
                size="large"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="checkPassword">
              <el-input
                v-model="registerForm.checkPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>

            <el-form-item>
              <NbButton
                type="primary"
                block
                :loading="isLoading"
                @click="handleRegister"
              >
                注册
              </NbButton>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </NbCard>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/user'
import type { LoginRequest, RegisterRequest } from '@/types/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const isLoading = ref(false)

// 登录表单
const loginFormRef = ref<FormInstance>()
const loginForm = reactive<LoginRequest>({
  userAccount: '',
  userPassword: '',
})

const loginRules: FormRules<LoginRequest> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4-32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字、下划线', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
}

// 注册表单
const registerFormRef = ref<FormInstance>()
const registerForm = reactive<RegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const validateCheckPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== registerForm.userPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules<RegisterRequest> = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 32, message: '账号长度为 4-32 位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '账号只能包含字母、数字、下划线', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8-64 位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateCheckPassword, trigger: 'blur' },
  ],
}

// 登录处理
async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  try {
    const success = await userStore.login({
      userAccount: loginForm.userAccount,
      userPassword: loginForm.userPassword,
    })
    if (success) {
      ElMessage.success('登录成功')
      router.push('/')
    }
  } finally {
    isLoading.value = false
  }
}

// 注册处理
async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  isLoading.value = true
  try {
    const res = await registerApi({
      userAccount: registerForm.userAccount,
      userPassword: registerForm.userPassword,
      checkPassword: registerForm.checkPassword,
    })
    if (res.code === 0) {
      ElMessage.success('注册成功，正在自动登录...')
      const success = await userStore.login({
        userAccount: registerForm.userAccount,
        userPassword: registerForm.userPassword,
      })
      if (success) {
        ElMessage.success('登录成功，请完善个人资料')
        router.push('/profile')
      }
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-card {
  padding: 32px;
}

.login-form :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
  color: var(--nb-text);
  padding-bottom: 4px;
}
</style>
