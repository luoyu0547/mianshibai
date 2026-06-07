// src/stores/user.ts
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, getCurrentUser, updateProfile as updateProfileApi } from '@/api/user'
import type { LoginUserVO, LoginRequest, UpdateProfileRequest } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(localStorage.getItem('mianshiba_token'))
  const userInfo = ref<LoginUserVO | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

  // Actions
  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    if (res.data.code === 0) {
      const { token: newToken, user } = res.data.data
      token.value = newToken
      userInfo.value = user
      localStorage.setItem('mianshiba_token', newToken)
      return true
    }
    return false
  }

  async function fetchCurrentUser() {
    const res = await getCurrentUser()
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      return true
    }
    return false
  }

  async function updateProfile(data: UpdateProfileRequest) {
    const res = await updateProfileApi(data)
    if (res.data.code === 0) {
      userInfo.value = res.data.data
      return true
    }
    return false
  }

  function logout() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('mianshiba_token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    fetchCurrentUser,
    updateProfile,
    logout,
  }
})
