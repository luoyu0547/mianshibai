<template>
  <AdminLayout>
    <section class="admin-page">
      <NbPageHeader
        eyebrow="Job Crawl"
        :title="isEdit ? '编辑采集任务' : '新建采集任务'"
        :description="isEdit ? '修改采集任务配置' : '创建新的职位批量采集任务'"
      />

      <NbCard>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          class="crawl-form"
        >
          <el-form-item label="任务名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="100" />
          </el-form-item>

          <el-form-item label="采集来源" prop="sourceType">
            <el-select v-model="form.sourceType" placeholder="请选择采集来源" style="width: 100%">
              <el-option
                v-for="opt in JOB_CRAWL_SOURCE_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="form.sourceType !== 'manual_url_list'" label="源URL" prop="sourceUrl">
            <el-input v-model="form.sourceUrl" placeholder="请输入源URL" />
          </el-form-item>

          <el-form-item v-if="form.sourceType === 'manual_url_list'" label="链接列表" prop="sourceUrl">
            <el-input
              v-model="form.sourceUrl"
              type="textarea"
              :rows="5"
              placeholder="请输入链接，每行一个URL"
            />
          </el-form-item>

          <el-form-item label="关键词" prop="keywords">
            <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
          </el-form-item>

          <el-form-item label="城市" prop="cities">
            <el-input v-model="form.cities" placeholder="多个城市用逗号分隔" />
          </el-form-item>

          <el-form-item label="经验级别" prop="experienceLevels">
            <el-input v-model="form.experienceLevels" placeholder="如：应届生,1-3年,3-5年" />
          </el-form-item>

          <el-form-item label="调度方式" prop="scheduleType">
            <el-select v-model="form.scheduleType" placeholder="请选择调度方式" style="width: 100%">
              <el-option
                v-for="opt in JOB_CRAWL_SCHEDULE_TYPE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="form.scheduleType === 'cron'" label="Cron表达式" prop="cronExpression">
            <el-input v-model="form.cronExpression" placeholder="如：0 0 8 * * ?" />
          </el-form-item>

          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              placeholder="可选备注信息"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <NbButton variant="primary" :loading="submitting" @click="handleSubmit">
              {{ isEdit ? '保存修改' : '创建任务' }}
            </NbButton>
            <NbButton variant="ghost" @click="router.back()">取消</NbButton>
          </el-form-item>
        </el-form>
      </NbCard>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbPageHeader from '@/components/NbPageHeader.vue'
import { useJobCrawlStore } from '@/stores/jobCrawl'
import { getJobCrawlTask } from '@/api/jobCrawl'
import { JOB_CRAWL_SOURCE_TYPE_OPTIONS, JOB_CRAWL_SCHEDULE_TYPE_OPTIONS } from '@/types/jobCrawl'
import type { AdminJobCrawlTaskCreateRequest } from '@/types/jobCrawl'

const router = useRouter()
const route = useRoute()
const crawlStore = useJobCrawlStore()

const isEdit = computed(() => !!route.params.id)
const taskId = computed(() => (isEdit.value ? Number(route.params.id) : undefined))

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<AdminJobCrawlTaskCreateRequest>({
  name: '',
  sourceType: 'company_career_page',
  sourceUrl: '',
  keywords: '',
  cities: '',
  experienceLevels: '',
  scheduleType: 'manual',
  cronExpression: '',
  remark: '',
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { max: 100, message: '长度不超过100个字符', trigger: 'blur' },
  ],
  sourceType: [
    { required: true, message: '请选择采集来源', trigger: 'change' },
  ],
  sourceUrl: [
    { required: true, message: '请输入源URL或链接列表', trigger: 'blur' },
  ],
}

async function loadTask() {
  if (!taskId.value) return
  const res = await getJobCrawlTask(taskId.value)
  if (res.data) {
    form.name = res.data.name
    form.sourceType = res.data.sourceType
    form.sourceUrl = res.data.sourceUrl || ''
    form.keywords = res.data.keywords || ''
    form.cities = res.data.cities || ''
    form.experienceLevels = res.data.experienceLevels || ''
    form.scheduleType = res.data.scheduleType
    form.cronExpression = res.data.cronExpression || ''
    form.remark = res.data.remark || ''
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && taskId.value) {
      await crawlStore.update(taskId.value, { ...form })
      ElMessage.success('任务已更新')
    } else {
      await crawlStore.create({ ...form })
    }
    router.push('/admin/job-crawl')
  } finally {
    submitting.value = false
  }
}

onMounted(loadTask)
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.crawl-form {
  max-width: 640px;
}
</style>
