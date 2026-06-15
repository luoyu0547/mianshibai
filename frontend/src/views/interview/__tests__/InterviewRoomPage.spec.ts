import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import InterviewRoomPage from '../InterviewRoomPage.vue'

const mockSubmitAnswer = vi.fn().mockResolvedValue({
  code: 0,
  data: { nextAction: 'REPORT_READY' },
})

vi.mock('@/stores/interview', () => ({
  useInterviewStore: () => ({
    fetchSession: vi.fn().mockResolvedValue({}),
    currentSession: {
      startedAt: new Date(Date.now() - 60000).toISOString(),
      durationMinutes: 30,
      totalQuestions: 5,
      currentQuestionNo: 0,
      targetPosition: 'Java开发',
      techDirection: '后端',
    },
    startSession: vi.fn().mockResolvedValue({
      code: 0,
      data: {
        turnId: 1,
        questionNo: 1,
        questionText: 'Test question',
        ttsAudioBase64: 'dGVzdA==',
      },
    }),
    submitAnswer: mockSubmitAnswer,
  }),
}))

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    token: 'test-token',
  }),
}))

vi.mock('@/utils/audio/asrClient', () => ({
  AsrClient: vi.fn().mockImplementation(() => ({
    connect: vi.fn(),
    sendAudio: vi.fn(),
    sendEnd: vi.fn(),
    close: vi.fn(),
  })),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn().mockResolvedValue('confirm'),
  },
}))

describe('InterviewRoomPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockSubmitAnswer.mockClear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  async function createComponent() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/interview/:id/room', component: InterviewRoomPage }],
    })
    await router.push('/interview/1/room')
    await router.isReady()

    const wrapper = mount(InterviewRoomPage, {
      global: { plugins: [router] },
    })

    // Wait for onMounted to complete
    await new Promise((resolve) => setTimeout(resolve, 0))
    return wrapper
  }

  it('renders toggle button in readyToAnswer state', async () => {
    vi.stubGlobal('Audio', vi.fn().mockImplementation(() => ({
      play: vi.fn().mockRejectedValue(new Error('mock')),
      pause: vi.fn(),
    })))

    const wrapper = await createComponent()

    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('开始面试'))
    expect(startBtn).toBeDefined()
    await startBtn!.trigger('click')

    // Wait for async flow: loadingQuestion → startSession → playQuestionAudio → readyToAnswer
    await new Promise((resolve) => setTimeout(resolve, 100))

    const toggle = wrapper.findAll('button').find((b) => b.text().includes('切换为文字'))
    expect(toggle).toBeDefined()
  })

  it('switches to text mode and submits typed answer', async () => {
    vi.stubGlobal('Audio', vi.fn().mockImplementation(() => ({
      play: vi.fn().mockRejectedValue(new Error('mock')),
      pause: vi.fn(),
    })))

    const wrapper = await createComponent()

    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('开始面试'))
    expect(startBtn).toBeDefined()
    await startBtn!.trigger('click')
    await new Promise((resolve) => setTimeout(resolve, 100))

    const toggle = wrapper.findAll('button').find((b) => b.text().includes('切换为文字'))
    expect(toggle).toBeDefined()
    await toggle!.trigger('click')

    const textarea = wrapper.find('textarea')
    expect(textarea.exists()).toBe(true)

    await textarea.setValue('这是文字回答')

    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('提交回答'))
    expect(submitBtn).toBeDefined()
    await submitBtn!.trigger('click')

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(mockSubmitAnswer).toHaveBeenCalledTimes(1)
    expect(mockSubmitAnswer).toHaveBeenCalledWith(
      1,
      expect.any(Number),
      expect.objectContaining({
        answerText: '这是文字回答',
        answerDurationSeconds: 0,
      }),
    )
  })
})
