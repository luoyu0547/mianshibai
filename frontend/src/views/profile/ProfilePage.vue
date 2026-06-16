<!-- src/views/profile/ProfilePage.vue -->
<template>
  <MainLayout>
    <div class="profile-page">
      <h1 class="profile-page__title">个人资料</h1>

      <NbCard class="profile-card">
        <div class="profile-card__layout">
          <!-- 左侧头像区 -->
          <div class="profile-card__left">
            <div class="profile-avatar" @click="avatarInputRef?.click()">
              <img v-if="form.userAvatar" :src="form.userAvatar" alt="用户头像" />
              <span v-else>{{ userStore.userInfo?.userName?.[0] || userStore.userInfo?.userAccount?.[0] || 'U' }}</span>
              <div class="profile-avatar__overlay">
                <svg viewBox="0 0 24 24" width="28" height="28" fill="currentColor"><path d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0 0 13 3.06V1h-2v2.06A8.994 8.994 0 0 0 3.06 11H1v2h2.06A8.994 8.994 0 0 0 11 20.94V23h2v-2.06A8.994 8.994 0 0 0 20.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/></svg>
              </div>
            </div>
            <input
              ref="avatarInputRef"
              class="profile-avatar__input"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              @change="handleAvatarChange"
            />
            <div class="profile-avatar__name">
              {{ userStore.userInfo?.userName || userStore.userInfo?.userAccount }}
            </div>
            <div class="profile-avatar__role">
              {{ userStore.userInfo?.userRole === 'admin' ? '管理员' : '普通用户' }}
            </div>
          </div>

          <!-- 右侧表单区 -->
          <div class="profile-card__right">
            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-position="top"
              class="profile-form"
            >
              <el-form-item label="昵称" prop="userName">
                <el-input v-model="form.userName" placeholder="请输入昵称" />
              </el-form-item>

              <el-form-item label="头像地址" prop="userAvatar">
                <el-input v-model="form.userAvatar" placeholder="上传后自动填充，也可粘贴图片 URL" />
              </el-form-item>

              <el-form-item label="目标岗位" prop="targetPosition">
                <el-select
                  v-model="form.targetPosition"
                  placeholder="请选择或输入目标岗位"
                  style="width: 100%;"
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                >
                  <el-option
                    v-for="item in targetPositionOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="技术方向" prop="techDirection">
                <el-select
                  v-model="form.techDirection"
                  placeholder="请选择技术方向"
                  style="width: 100%;"
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                >
                  <el-option
                    v-for="item in techDirectionOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="工作年限" prop="workYears">
                <el-input-number
                  v-model="form.workYears"
                  :min="0"
                  :max="60"
                  :step="1"
                  style="width: 100%;"
                  controls-position="right"
                />
              </el-form-item>

              <el-form-item label="城市" prop="city">
                <el-select
                  v-model="form.city"
                  placeholder="请选择或输入所在城市"
                  style="width: 100%;"
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                >
                  <el-option
                    v-for="item in cityOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="求职状态" prop="jobStatus">
                <el-select v-model="form.jobStatus" placeholder="请选择" style="width: 100%;">
                  <el-option label="不限" value="" />
                  <el-option label="正在看机会" value="looking" />
                  <el-option label="开放机会" value="open" />
                  <el-option label="暂不考虑" value="not_looking" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <NbButton
                  type="success"
                  block
                  :loading="isSaving"
                  @click="handleSave"
                >
                  保存资料
                </NbButton>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useUserStore } from '@/stores/user'
import type { UpdateProfileRequest } from '@/types/user'
import { uploadAvatar } from '@/api/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const avatarInputRef = ref<HTMLInputElement>()
const isSaving = ref(false)
const isUploadingAvatar = ref(false)

const targetPositionOptions = [
  'Java 开发工程师',
  '前端开发工程师',
  '后端开发工程师',
  '全栈开发工程师',
  'Python 开发工程师',
  'Go 开发工程师',
  'C++ 开发工程师',
  '算法工程师',
  'AI 工程师',
  '大数据工程师',
  '数据分析师',
  '测试工程师',
  '运维工程师',
  '安全工程师',
  'iOS 开发工程师',
  'Android 开发工程师',
  '产品经理',
  '项目经理',
  '架构师',
  '技术总监',
]

const techDirectionOptions = [
  '后端',
  '前端',
  '全栈',
  '移动端',
  'AI / 机器学习',
  '大数据',
  '云计算 / 云原生',
  '测试开发',
  '运维 / DevOps',
  '安全',
  '嵌入式 / IoT',
  '游戏开发',
  '区块链',
  '其他',
]

const cityOptions = [
  '北京', '上海', '广州', '深圳',
  '杭州', '南京', '苏州', '成都',
  '武汉', '西安', '重庆', '长沙',
  '天津', '郑州', '济南', '青岛',
  '合肥', '福州', '厦门', '东莞',
  '佛山', '珠海', '大连', '沈阳',
  '哈尔滨', '长春', '昆明', '贵阳',
  '南宁', '海口', '石家庄', '太原',
  '南昌', '兰州', '银川', '西宁',
  '拉萨', '乌鲁木齐', '呼和浩特',
  '海外',
]

const form = reactive<UpdateProfileRequest>({
  userName: '',
  userAvatar: '',
  targetPosition: '',
  techDirection: '',
  workYears: 0,
  city: '',
  jobStatus: '',
})

const rules: FormRules<UpdateProfileRequest> = {
  userName: [
    { max: 64, message: '昵称最长 64 个字符', trigger: 'blur' },
  ],
  userAvatar: [
    { max: 512, message: '头像地址最长 512 个字符', trigger: 'blur' },
  ],
  targetPosition: [
    { max: 128, message: '目标岗位最长 128 个字符', trigger: 'blur' },
  ],
  techDirection: [
    { max: 128, message: '技术方向最长 128 个字符', trigger: 'blur' },
  ],
  workYears: [
    { type: 'number', min: 0, max: 60, message: '工作年限需在 0-60 之间', trigger: 'change' },
  ],
  city: [
    { max: 64, message: '城市最长 64 个字符', trigger: 'blur' },
  ],
}

// 页面加载时填充当前用户信息
onMounted(() => {
  if (userStore.userInfo) {
    form.userName = userStore.userInfo.userName || ''
    form.userAvatar = userStore.userInfo.userAvatar || ''
    form.targetPosition = userStore.userInfo.targetPosition || ''
    form.techDirection = userStore.userInfo.techDirection || ''
    form.workYears = userStore.userInfo.workYears || 0
    form.city = userStore.userInfo.city || ''
    form.jobStatus = userStore.userInfo.jobStatus || ''
  }
})

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WebP 图片')
    input.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像不能超过 2MB')
    input.value = ''
    return
  }

  isUploadingAvatar.value = true
  try {
    const res = await uploadAvatar(file)
    form.userAvatar = res.data.url
    ElMessage.success('头像上传成功，请保存资料')
  } finally {
    isUploadingAvatar.value = false
    input.value = ''
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  isSaving.value = true
  try {
    const success = await userStore.updateProfile({
      userName: form.userName || undefined,
      userAvatar: form.userAvatar || undefined,
      targetPosition: form.targetPosition || undefined,
      techDirection: form.techDirection || undefined,
      workYears: form.workYears,
      city: form.city || undefined,
      jobStatus: form.jobStatus || undefined,
    })
    if (success) {
      ElMessage.success('资料保存成功')
    }
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
.profile-page__title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 24px;
}

.profile-card__layout {
  display: flex;
  gap: 48px;
}

.profile-card__left {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
  border-right: 2px dashed var(--nb-border);
}

.profile-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--nb-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 700;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  margin-bottom: 16px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
}

.profile-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.profile-avatar__overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
}

.profile-avatar:hover .profile-avatar__overlay {
  opacity: 1;
}

.profile-avatar__input {
  display: none;
}

.profile-avatar__name {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.profile-avatar__role {
  font-size: 14px;
  color: var(--nb-muted);
}

.profile-card__right {
  flex: 1;
}

.profile-form :deep(.el-form-item__label) {
  font-family: var(--font-heading);
  font-weight: 500;
}

@media (max-width: 768px) {
  .profile-card__layout {
    flex-direction: column;
    gap: 24px;
  }

  .profile-card__left {
    flex: none;
    border-right: none;
    border-bottom: 2px dashed var(--nb-border);
    padding-top: 0;
    padding-bottom: 24px;
  }
}
</style>
