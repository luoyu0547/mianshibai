import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AdminJobCrawlReviewPage from '../AdminJobCrawlReviewPage.vue'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn() }),
  }
})

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
}))

vi.mock('@/api/jobCrawl', () => ({
  listJobCrawlReviewItems: vi.fn().mockResolvedValue({ data: [] }),
  approveJobCrawlItem: vi.fn().mockResolvedValue({}),
  rejectJobCrawlItem: vi.fn().mockResolvedValue({}),
  markJobCrawlItemDuplicate: vi.fn().mockResolvedValue({}),
}))

describe('AdminJobCrawlReviewPage', () => {
  it('renders pending review header', async () => {
    setActivePinia(createPinia())
    const wrapper = mount(AdminJobCrawlReviewPage, {
      global: {
        stubs: {
          AdminLayout: { template: '<div><slot /></div>' },
          NbPageHeader: { template: '<div><h2>{{ title }}</h2><slot name="actions" /><slot /></div>', props: ['title'] },
          NbCard: { template: '<div><slot /></div>' },
          NbButton: { template: '<button><slot /></button>' },
          NbStatusBadge: { template: '<span>{{ label }}</span>', props: ['label', 'variant'] },
          NbLoadingBlock: { template: '<div />' },
          NbEmptyState: { template: '<div />' },
          ElSelect: { template: '<select><slot /></select>', props: ['modelValue'] },
          ElOption: { template: '<option><slot /></option>', props: ['value', 'label'] },
          ElInput: { template: '<input />', props: ['modelValue'] },
          ElTag: { template: '<span><slot /></span>', props: ['size'] },
        },
      },
    })
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('职位采集审核池')
  })
})
