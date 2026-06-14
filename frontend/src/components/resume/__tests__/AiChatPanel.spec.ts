import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiChatPanel from '../AiChatPanel.vue'

vi.mock('@/api/resume', () => ({
  getChatHistory: vi.fn().mockResolvedValue({ code: 0, data: [] }),
}))

const textEncoder = new TextEncoder()

function streamFromText(text: string) {
  return new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(textEncoder.encode(text))
      controller.close()
    },
  })
}

describe('AiChatPanel', () => {
  it('renders proposals as PatchCompareCard when receiving resume_patch_proposal', async () => {
    const proposal = JSON.stringify({
      sectionType: 'basic',
      operation: 'replace_section',
      reason: '基本信息可以更规范',
      sectionData: { name: '张三', email: 'zhangsan@xx.com' },
    })
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      body: streamFromText(
        `data: 好的，我会帮你优化简历。\n\nevent: resume_patch_proposal\ndata: ${proposal}\n\ndata: [DONE]\n\n`,
      ),
    }))

    const wrapper = mount(AiChatPanel, {
      props: {
        resumeId: 1,
        sectionDataMap: {
          basic: { name: '张三', email: 'a@b.com' },
          education: [],
          work: [],
          project: [],
          skills: {},
          summary: {},
        },
      },
      global: {
        stubs: {
          ElInput: {
            template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup.enter="$emit(\'keyup\', $event)" />',
            props: ['modelValue'],
          },
          NbButton: {
            template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot /></button>',
            props: ['disabled'],
          },
          NbEmptyState: true,
          PatchCompareCard: {
            template: '<div class="patch-compare-card-stub">查看对比</div>',
            props: ['proposal', 'currentData', 'sectionType'],
          },
        },
      },
    })

    await wrapper.find('input').setValue('帮我优化简历')
    await wrapper.find('button').trigger('click')
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('查看对比')
    })

    const messageBody = wrapper.find('.ai-chat-panel__bubble--assistant .ai-chat-panel__message-body')
    expect(messageBody.exists()).toBe(true)
    expect(messageBody.find('.ai-chat-panel__content').text()).toContain('好的，我会帮你优化简历。')
  })
})
