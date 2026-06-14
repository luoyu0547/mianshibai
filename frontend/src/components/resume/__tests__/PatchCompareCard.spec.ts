import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import PatchCompareCard from '../PatchCompareCard.vue'

const mockProposal = {
  sectionType: 'basic' as const,
  operation: 'replace_section' as const,
  reason: '基本信息可以更规范',
  sectionData: {
    name: '张三',
    email: 'zhangsan@xx.com',
    phone: '13800138000',
    targetPosition: 'Java 后端工程师',
    city: '北京',
    github: '',
    blog: '',
  },
}

const mockCurrentData = {
  name: '张三',
  email: 'a@b.com',
  phone: '138001',
  targetPosition: '',
  city: '',
  github: '',
  blog: '',
}

describe('PatchCompareCard', () => {
  it('renders collapsed mini card by default', () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: true },
      },
    })
    expect(wrapper.find('.patch-compare-card__mini').exists()).toBe(true)
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(false)
  })

  it('expands to show comparison when clicking 查看对比', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(true)
    expect(wrapper.find('.patch-compare-card__field').exists()).toBe(true)
  })

  it('shows modified fields with markers', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    const modifiedRows = wrapper.findAll('.patch-compare-card__field--modified')
    expect(modifiedRows.length).toBeGreaterThanOrEqual(1)
  })

  it('emits accept on clicking 同意', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    await wrapper.find('.patch-compare-card__accept-btn').trigger('click')
    expect(wrapper.emitted('accept')).toHaveLength(1)
  })

  it('emits reject on clicking 反对', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    await wrapper.find('.patch-compare-card__reject-btn').trigger('click')
    expect(wrapper.emitted('reject')).toHaveLength(1)
  })

  it('collapses back when clicking 收起', async () => {
    const wrapper = mount(PatchCompareCard, {
      props: {
        proposal: mockProposal,
        currentData: mockCurrentData,
        sectionType: 'basic',
      },
      global: {
        stubs: { NbButton: { template: '<button @click="$emit(\'click\')"><slot/></button>' } },
      },
    })

    await wrapper.find('.patch-compare-card__expand-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(true)
    await wrapper.find('.patch-compare-card__collapse-btn').trigger('click')
    expect(wrapper.find('.patch-compare-card__compare').exists()).toBe(false)
  })
})
