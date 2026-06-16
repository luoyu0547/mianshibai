<!-- src/views/application/ApplicationEditPage.vue -->
<template>
  <MainLayout>
    <div class="app-edit-page">
      <NbPageHeader
        eyebrow="求职管理"
        title="新建投递"
        description="记录一次新的求职投递"
      >
        <template #actions>
          <NbButton variant="ghost" @click="router.back()">&larr; 返回</NbButton>
        </template>
      </NbPageHeader>

      <NbCard>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          label-position="top"
          class="app-edit-page__form"
        >
          <div class="app-edit-page__row">
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入公司名称" />
            </el-form-item>
            <el-form-item label="职位名称" prop="jobTitle">
              <el-input v-model="form.jobTitle" placeholder="请输入职位名称" />
            </el-form-item>
          </div>

          <div class="app-edit-page__row">
            <el-form-item label="来源" prop="source">
              <el-select v-model="form.source" placeholder="选择来源" style="width: 100%;">
                <el-option
                  v-for="opt in APPLICATION_SOURCE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-input
                v-if="form.source === 'other'"
                v-model="form.sourceOther"
                placeholder="请输入具体渠道"
                style="margin-top: 8px;"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="选择状态" style="width: 100%;">
                <el-option
                  v-for="opt in APPLICATION_STATUS_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="app-edit-page__row">
            <el-form-item label="投递日期" prop="appliedAt">
              <el-date-picker
                v-model="form.appliedAt"
                type="date"
                placeholder="选择投递日期"
                value-format="YYYY-MM-DD"
                style="width: 100%;"
              />
            </el-form-item>
            <el-form-item label="薪资范围" prop="salaryRange">
              <el-input v-model="form.salaryRange" placeholder="如：20-30K" />
            </el-form-item>
          </div>

          <div class="app-edit-page__row">
            <el-form-item label="工作地点" prop="location">
              <el-input v-model="form.location" placeholder="如：北京" />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="联系人姓名" />
            </el-form-item>
          </div>

          <div class="app-edit-page__row">
            <el-form-item label="联系方式" prop="contactInfo">
              <el-input v-model="form.contactInfo" placeholder="手机 / 邮箱 / 微信" />
            </el-form-item>
          </div>

          <el-form-item label="备注" prop="notes">
            <el-input
              v-model="form.notes"
              type="textarea"
              :rows="4"
              placeholder="备注信息"
            />
          </el-form-item>

          <div class="app-edit-page__btns">
            <NbButton variant="ghost" @click="router.push('/applications')">取消</NbButton>
            <NbButton variant="primary" :loading="applicationStore.loading" @click="handleSubmit">
              创建投递
            </NbButton>
          </div>
        </el-form>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import { useApplicationStore } from '@/stores/application'
import { APPLICATION_STATUS_OPTIONS, APPLICATION_SOURCE_OPTIONS } from '@/types/application'
import type { ApplicationStatus, ApplicationSource } from '@/types/application'

const router = useRouter()
const applicationStore = useApplicationStore()
const formRef = ref<FormInstance>()

const form = reactive({
  companyName: '',
  jobTitle: '',
  source: '' as ApplicationSource | '',
  sourceOther: '',
  status: 'pending_submit' as ApplicationStatus,
  appliedAt: null as string | null,
  salaryRange: '',
  location: '',
  contactName: '',
  contactInfo: '',
  notes: '',
})

function dateToIso(dateStr: string | null): string | null {
  if (!dateStr) return null
  return dateStr.length === 10 ? dateStr + 'T00:00:00' : dateStr
}

function isoToDate(iso: string | null): string | null {
  if (!iso) return null
  return iso.substring(0, 10)
}

const rules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  jobTitle: [{ required: true, message: '请输入职位名称', trigger: 'blur' }],
}

function resolveSource(): string | undefined {
  if (!form.source) return undefined
  if (form.source === 'other') return form.sourceOther?.trim() || '其他'
  const opt = APPLICATION_SOURCE_OPTIONS.find(o => o.value === form.source)
  return opt?.label || form.source
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const result = await applicationStore.createApplication({
    companyName: form.companyName,
    jobTitle: form.jobTitle,
    source: resolveSource(),
    status: form.status,
    appliedAt: dateToIso(form.appliedAt),
    salaryRange: form.salaryRange || undefined,
    location: form.location || undefined,
    contactName: form.contactName || undefined,
    contactInfo: form.contactInfo || undefined,
    notes: form.notes || undefined,
  })

  if (result) {
    ElMessage.success('投递创建成功')
    router.push(`/applications/${result.id}`)
  } else {
    ElMessage.error('创建失败')
  }
}
</script>

<style scoped>
.app-edit-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.app-edit-page__form {
  max-width: 800px;
}

.app-edit-page__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.app-edit-page__btns {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 700px) {
  .app-edit-page__row {
    grid-template-columns: 1fr;
  }
}
</style>
