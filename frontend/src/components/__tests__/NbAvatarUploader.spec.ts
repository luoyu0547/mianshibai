import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import NbAvatarUploader from '../NbAvatarUploader.vue'

describe('NbAvatarUploader', () => {
  it('renders fallback text when no avatar', () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: '',
        fallbackText: '张三',
      },
    })
    expect(wrapper.text()).toContain('张')
  })

  it('renders image when avatar is provided', () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: 'https://example.com/avatar.png',
      },
    })
    expect(wrapper.find('img').exists()).toBe(true)
    expect(wrapper.find('img').attributes('src')).toBe('https://example.com/avatar.png')
  })

  it('shows remove button when clearable and has avatar', () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: 'https://example.com/avatar.png',
        clearable: true,
      },
    })
    expect(wrapper.text()).toContain('移除头像')
  })

  it('emits remove event when remove button clicked', async () => {
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: 'https://example.com/avatar.png',
        clearable: true,
      },
    })
    await wrapper.find('.nb-avatar-uploader__remove').trigger('click')
    expect(wrapper.emitted('remove')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1)
  })

  it('calls uploadFn and emits update:modelValue on file change', async () => {
    const uploadFn = vi.fn().mockResolvedValue('https://example.com/new.png')
    const wrapper = mount(NbAvatarUploader, {
      props: {
        modelValue: '',
        uploadFn,
      },
    })

    const file = new File(['dummy'], 'avatar.png', { type: 'image/png' })
    const input = wrapper.find('input[type="file"]')
    await input.setValue('')
    Object.defineProperty(input.element, 'files', {
      value: [file],
      writable: false,
    })
    await input.trigger('change')

    // uploadFn is async, wait for it
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(uploadFn).toHaveBeenCalledWith(file)
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['https://example.com/new.png'])
  })
})
