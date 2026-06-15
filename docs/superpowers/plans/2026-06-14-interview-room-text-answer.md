# 模拟面试房间语音/文字切换输入实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在模拟面试房间页面增加语音/文字输入切换开关，允许用户在 ASR 识别不准时切换到文字输入并提交回答。

**Architecture:** 将输入模式状态管理抽取到独立 composable `useInterviewInputMode`，在 `InterviewRoomPage.vue` 中集成；复用现有后端提交接口，不改动后端。

**Tech Stack:** Vue 3, TypeScript, Pinia, Vitest, @vue/test-utils

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `frontend/src/composables/useInterviewInputMode.ts` | 创建 | 管理 inputMode / textAnswer / toggle 逻辑 |
| `frontend/src/composables/__tests__/useInterviewInputMode.spec.ts` | 创建 | composable 单元测试 |
| `frontend/src/views/interview/InterviewRoomPage.vue` | 修改 | 集成切换按钮、文字输入框、提交逻辑 |
| `frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts` | 创建 | 页面级关键交互测试 |

---

## Task 1: 创建 useInterviewInputMode composable

**Files:**
- Create: `frontend/src/composables/useInterviewInputMode.ts`

**目标：** 封装输入模式状态，提供清晰的切换接口。

- [ ] **Step 1: 编写 composable 代码**

```ts
// frontend/src/composables/useInterviewInputMode.ts
import { ref, computed } from 'vue'

export type InputMode = 'voice' | 'text'

export interface UseInterviewInputModeOptions {
  onSwitchToText?: () => void
  onSwitchToVoice?: () => void
}

export function useInterviewInputMode(options: UseInterviewInputModeOptions = {}) {
  const inputMode = ref<InputMode>('voice')
  const textAnswer = ref('')

  const isVoiceMode = computed(() => inputMode.value === 'voice')
  const isTextMode = computed(() => inputMode.value === 'text')

  function setTextAnswer(value: string) {
    textAnswer.value = value
  }

  function switchToText(currentTranscript: string = '') {
    options.onSwitchToText?.()
    textAnswer.value = currentTranscript
    inputMode.value = 'text'
  }

  function switchToVoice() {
    options.onSwitchToVoice?.()
    inputMode.value = 'voice'
  }

  function toggleInputMode(currentTranscript: string = '') {
    if (isVoiceMode.value) {
      switchToText(currentTranscript)
    } else {
      switchToVoice()
    }
  }

  return {
    inputMode,
    textAnswer,
    isVoiceMode,
    isTextMode,
    setTextAnswer,
    switchToText,
    switchToVoice,
    toggleInputMode,
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/composables/useInterviewInputMode.ts
git commit -m "feat(interview): add useInterviewInputMode composable"
```

---

## Task 2: 为 useInterviewInputMode 编写单元测试

**Files:**
- Create: `frontend/src/composables/__tests__/useInterviewInputMode.spec.ts`

**目标：** 验证模式切换、文本回填、回调触发。

- [ ] **Step 1: 编写测试代码**

```ts
// frontend/src/composables/__tests__/useInterviewInputMode.spec.ts
import { describe, it, expect, vi } from 'vitest'
import { useInterviewInputMode } from '../useInterviewInputMode'

describe('useInterviewInputMode', () => {
  it('defaults to voice mode', () => {
    const { inputMode, isVoiceMode, isTextMode } = useInterviewInputMode()
    expect(inputMode.value).toBe('voice')
    expect(isVoiceMode.value).toBe(true)
    expect(isTextMode.value).toBe(false)
  })

  it('toggles to text mode and fills transcript', () => {
    const onSwitchToText = vi.fn()
    const { toggleInputMode, isTextMode, textAnswer } = useInterviewInputMode({
      onSwitchToText,
    })

    toggleInputMode('已识别文本')

    expect(onSwitchToText).toHaveBeenCalledTimes(1)
    expect(isTextMode.value).toBe(true)
    expect(textAnswer.value).toBe('已识别文本')
  })

  it('toggles back to voice mode and clears transcript callback', () => {
    const onSwitchToVoice = vi.fn()
    const { toggleInputMode, isVoiceMode } = useInterviewInputMode({
      onSwitchToVoice,
    })

    toggleInputMode('已识别文本')
    toggleInputMode('已识别文本')

    expect(onSwitchToVoice).toHaveBeenCalledTimes(1)
    expect(isVoiceMode.value).toBe(true)
  })

  it('setTextAnswer updates value', () => {
    const { textAnswer, setTextAnswer } = useInterviewInputMode()
    setTextAnswer('手动输入')
    expect(textAnswer.value).toBe('手动输入')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败/通过**

```bash
cd frontend
npx vitest run src/composables/__tests__/useInterviewInputMode.spec.ts
```

Expected: 4 tests PASS

- [ ] **Step 3: Commit**

```bash
git add frontend/src/composables/__tests__/useInterviewInputMode.spec.ts
git commit -m "test(interview): add useInterviewInputMode tests"
```

---

## Task 3: 修改 InterviewRoomPage.vue 集成输入模式

**Files:**
- Modify: `frontend/src/views/interview/InterviewRoomPage.vue`

**目标：** 在页面中集成 composable，实现语音/文字切换与文字提交。

- [ ] **Step 1: 导入 composable**

在 `<script setup>` 顶部添加导入：

```ts
import { useInterviewInputMode } from '@/composables/useInterviewInputMode'
```

- [ ] **Step 2: 初始化 composable**

在 `script setup` 中，在 `const remainingSeconds = ref<number | null>(null)` 附近添加：

```ts
const {
  inputMode,
  textAnswer,
  isVoiceMode,
  isTextMode,
  toggleInputMode,
} = useInterviewInputMode({
  onSwitchToText: () => {
    stopRecordingWithoutSubmit()
  },
  onSwitchToVoice: () => {
    textAnswer.value = ''
    finalText.value = ''
    partialText.value = ''
    if (state.value === 'recording') {
      stopRecordingWithoutSubmit()
    }
  },
})
```

- [ ] **Step 3: 新增 stopRecordingWithoutSubmit 函数**

在 `stopRecording` 函数附近添加：

```ts
function stopRecordingWithoutSubmit() {
  if (asrClient) {
    asrClient.sendEnd()
  }
  cleanupAudio()
  setTimeout(() => {
    if (asrClient) {
      asrClient.close()
      asrClient = null
    }
  }, 1500)
}
```

- [ ] **Step 4: 修改 stopRecording 复用新函数**

将原 `stopRecording` 改为：

```ts
function stopRecording() {
  state.value = 'recognizing'
  stopRecordingWithoutSubmit()
  setTimeout(() => {
    submitAnswer()
  }, 1500)
}
```

- [ ] **Step 5: 修改 submitAnswer 支持文字模式**

将 `submitAnswer` 开头改为：

```ts
async function submitAnswer() {
  const answer = isTextMode.value
    ? textAnswer.value.trim()
    : finalText.value.trim()

  if (!answer) {
    ElMessage.warning(
      isTextMode.value ? '回答内容不能为空，请输入后提交' : '未检测到有效回答，请重新回答',
    )
    state.value = 'readyToAnswer'
    if (isTextMode.value) {
      textAnswer.value = ''
    } else {
      finalText.value = ''
      partialText.value = ''
    }
    return
  }

  state.value = 'submittingAnswer'
  const durationSeconds = isTextMode.value
    ? 0
    : Math.round((Date.now() - recordingStartTime) / 1000)
  const turnId = currentQuestion.value?.turnId || 0

  try {
    const res = await interviewStore.submitAnswer(sessionId.value, turnId, {
      answerText: answer,
      answerDurationSeconds: durationSeconds,
    })

    if (res.code === 0 && res.data) {
      const result = res.data
      if (result.nextAction === 'REPORT_READY') {
        state.value = 'completed'
        ElMessage.success('面试结束！正在生成报告...')
        setTimeout(() => {
          router.push(`/interview/${sessionId.value}/report`)
        }, 1500)
      } else if (result.turn) {
        state.value = 'generatingNext'
        currentQuestion.value = result.turn
        currentQuestionNo.value = result.turn.questionNo
        finalText.value = ''
        partialText.value = ''
        textAnswer.value = ''
        inputMode.value = 'voice'
        await playQuestionAudio(result.turn)
      } else {
        state.value = 'readyToAnswer'
      }
    } else {
      state.value = 'error'
      errorMessage.value = res.message || '提交失败'
    }
  } catch {
    state.value = 'error'
    errorMessage.value = '网络错误'
  }
}
```

- [ ] **Step 6: 修改模板底部控制区**

找到 `<div class="interview-room__controls">` 区域，统一改为始终显示切换按钮，并根据模式显示不同主操作。

**recording 状态卡片内：**

```vue
<div class="interview-room__controls">
  <NbButton
    variant="accent"
    @click="stopRecording"
  >
    结束回答
  </NbButton>
  <NbButton
    variant="secondary"
    @click="toggleInputMode(finalText)"
  >
    切换为文字
  </NbButton>
</div>
```

**readyToAnswer 状态：**

```vue
<div v-if="state === 'readyToAnswer'" class="interview-room__controls">
  <template v-if="isVoiceMode">
    <NbButton
      variant="primary"
      :disabled="remainingSeconds !== null && remainingSeconds <= 0"
      @click="startRecording"
    >
      开始回答
    </NbButton>
  </template>
  <template v-else>
    <NbButton
      variant="primary"
      :disabled="remainingSeconds !== null && remainingSeconds <= 0"
      @click="submitAnswer"
    >
      提交回答
    </NbButton>
  </template>
  <NbButton
    variant="secondary"
    @click="toggleInputMode(finalText)"
  >
    {{ isVoiceMode ? '切换为文字' : '切换为语音' }}
  </NbButton>
</div>
```

- [ ] **Step 7: 修改模板中的实时语音识别区**

将实时语音识别区在文字模式下变为可编辑文本框。找到两个 `<div class="interview-room__transcript">` 区域，统一替换为：

```vue
<div class="interview-room__transcript">
  <div class="interview-room__transcript-label">
    {{ isTextMode ? '你的回答（可编辑）' : '实时语音识别' }}
  </div>
  <div
    v-if="isTextMode"
    class="interview-room__transcript-content interview-room__transcript-content--editable"
  >
    <textarea
      v-model="textAnswer"
      class="interview-room__text-answer"
      rows="5"
      placeholder="请输入你的回答..."
    />
  </div>
  <div v-else class="interview-room__transcript-content">
    <span v-if="finalText" class="interview-room__transcript-final">{{ finalText }}</span>
    <span v-if="partialText" class="interview-room__transcript-partial">{{ partialText }}</span>
    <span v-if="!partialText && !finalText" class="interview-room__transcript-placeholder">
      {{ state === 'readyToAnswer' ? '等待回答' : '正在聆听...' }}
    </span>
  </div>
</div>
```

- [ ] **Step 8: 添加文字输入框样式**

在 `<style scoped>` 末尾添加：

```css
.interview-room__transcript-content--editable {
  padding: 0;
}

.interview-room__text-answer {
  width: 100%;
  min-height: 120px;
  padding: 12px;
  border: none;
  border-radius: var(--nb-radius);
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.6;
  color: var(--nb-text);
  resize: vertical;
  outline: none;
}

.interview-room__text-answer::placeholder {
  color: var(--nb-muted);
  font-style: italic;
}
```

- [ ] **Step 9: Commit**

```bash
git add frontend/src/views/interview/InterviewRoomPage.vue
git commit -m "feat(interview): add voice/text input toggle in interview room"
```

---

## Task 4: 添加 InterviewRoomPage 页面级测试

**Files:**
- Create: `frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts`

**目标：** 验证切换按钮出现、文字提交调用 store。

- [ ] **Step 1: 编写测试代码**

```ts
// frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import InterviewRoomPage from '../InterviewRoomPage.vue'

const mockSubmitAnswer = vi.fn().mockResolvedValue({
  code: 0,
  data: { nextAction: 'REPORT_READY' },
})

vi.mock('@/stores/interview', () => ({
  useInterviewStore: () => ({
    fetchSession: vi.fn().mockResolvedValue({}),
    currentSession: {
      startedAt: new Date().toISOString(),
      durationMinutes: 30,
      totalQuestions: 5,
      currentQuestionNo: 1,
      targetPosition: 'Java开发',
      techDirection: '后端',
    },
    startSession: vi.fn(),
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

describe('InterviewRoomPage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockSubmitAnswer.mockClear()
  })

  it('renders toggle button in readyToAnswer state', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/interview/:id/room', component: InterviewRoomPage }],
    })
    await router.push('/interview/1/room')

    const wrapper = mount(InterviewRoomPage, {
      global: {
        plugins: [router],
      },
    })

    // 等待 onMounted 完成
    await new Promise((resolve) => setTimeout(resolve, 0))

    const toggleButton = wrapper.findAll('button').find((b) => b.text().includes('切换为文字'))
    expect(toggleButton).toBeDefined()
  })

  it('switches to text mode and submits typed answer', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/interview/:id/room', component: InterviewRoomPage }],
    })
    await router.push('/interview/1/room')

    const wrapper = mount(InterviewRoomPage, {
      global: {
        plugins: [router],
      },
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    const toggleButton = wrapper.findAll('button').find((b) => b.text().includes('切换为文字'))
    await toggleButton?.trigger('click')

    const textarea = wrapper.find('textarea')
    expect(textarea.exists()).toBe(true)

    await textarea.setValue('这是文字回答')

    const submitButton = wrapper.findAll('button').find((b) => b.text().includes('提交回答'))
    await submitButton?.trigger('click')

    expect(mockSubmitAnswer).toHaveBeenCalledTimes(1)
    expect(mockSubmitAnswer).toHaveBeenCalledWith(
      1,
      0,
      expect.objectContaining({
        answerText: '这是文字回答',
        answerDurationSeconds: 0,
      }),
    )
  })
})
```

- [ ] **Step 2: 运行测试**

```bash
cd frontend
npx vitest run src/views/interview/__tests__/InterviewRoomPage.spec.ts
```

Expected: 2 tests PASS（或根据实际实现调整）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/interview/__tests__/InterviewRoomPage.spec.ts
git commit -m "test(interview): add InterviewRoomPage text input tests"
```

---

## Task 5: 运行前端质量检查

**Files:**
- 涉及全部修改文件

- [ ] **Step 1: 类型检查**

```bash
cd frontend
npm run type-check
```

Expected: 无类型错误

- [ ] **Step 2: ESLint 检查**

```bash
cd frontend
npm run lint
```

Expected: 无 lint 错误

- [ ] **Step 3: 单元测试全量**

```bash
cd frontend
npm run test:unit
```

Expected: 全部通过

- [ ] **Step 4: Commit（如 lint/type 有自动修复）**

```bash
git add -A
git commit -m "chore(interview): fix lint and type issues" || echo "No changes to commit"
```

---

## Task 6: 手动验证

**Files:**
- 无需文件修改

- [ ] **Step 1: 启动前后端**

```bash
# 后端
cd backend
.\mvnw.cmd spring-boot:run

# 前端（新终端）
cd frontend
npm run dev
```

- [ ] **Step 2: 验证场景**

1. 创建并进入面试房间，默认显示「开始回答」按钮。
2. 点击「切换为文字」，出现 textarea 和「提交回答」按钮。
3. 输入文字并提交，正常进入下一题或报告页。
4. 切回语音模式，点击「开始回答」录音，正常提交。
5. 录音中点击「切换为文字」，已识别文本自动填入 textarea，修改后提交。
6. 文字模式下空内容提交，弹出「回答内容不能为空」警告。

- [ ] **Step 3: 提交最终代码**

```bash
git add -A
git commit -m "feat(interview): support text input toggle in interview room"
```

---

## 自我审查

- **Spec coverage:** 全部覆盖。
  - 默认语音：Task 3 Step 2 初始化 `inputMode('voice')`。
  - 切换时自动带入文本：Task 3 Step 2 `onSwitchToText` 设置 `textAnswer.value = currentTranscript`。
  - 底部控制区切换按钮：Task 3 Step 6。
  - 文字提交按钮：Task 3 Step 6。
  - 后端无需改动：未安排后端任务。
- **Placeholder scan:** 无 TBD/TODO。
- **Type consistency:** `inputMode` 类型 `'voice' | 'text'`、`textAnswer` string、composable 接口一致。
