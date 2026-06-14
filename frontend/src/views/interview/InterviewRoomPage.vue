<template>
  <div class="interview-room">
    <div
      v-if="remainingSeconds !== null"
      class="interview-room__timer"
      :class="{ 'interview-room__timer--warning': remainingSeconds < 120 }"
    >
      <span class="interview-room__timer-icon"></span>
      <span class="interview-room__timer-text">{{ formatTime(remainingSeconds) }}</span>
    </div>

    <div class="interview-room__top-bar">
      <div class="interview-room__progress">
        <span class="interview-room__progress-num">{{ currentQuestionNo }}</span>
        <span class="interview-room__progress-sep">/</span>
        <span class="interview-room__progress-total">{{ totalQuestions }}</span>
      </div>
      <NbButton variant="danger" @click="handleCancel">结束面试</NbButton>
    </div>

    <div class="interview-room__main">
      <div class="interview-room__left">
        <NbCard
          v-if="state === 'idle'"
          class="interview-room__question-card"
        >
          <div class="interview-room__idle">
            <div class="interview-room__idle-icon"></div>
            <p>准备好开始面试了吗？</p>
            <NbButton variant="primary" @click="startInterview">开始面试</NbButton>
          </div>
        </NbCard>

        <NbCard
          v-else-if="state === 'error'"
          variant="danger"
          class="interview-room__question-card"
        >
          <div class="interview-room__error-state">
            <div class="interview-room__error-icon"></div>
            <p class="interview-room__error-message">{{ errorMessage || '发生错误' }}</p>
            <NbButton variant="primary" @click="retryLastAction">重试</NbButton>
          </div>
        </NbCard>

        <NbCard
          v-else-if="state === 'recording'"
          variant="warning"
          class="interview-room__question-card"
        >
          <div class="interview-room__recording-banner">
            <span class="interview-room__rec-dot"></span>
            <span class="interview-room__rec-label">正在录音</span>
          </div>

          <div class="interview-room__question-header">
            <NbStatusBadge
              :label="`第 ${currentQuestionNo} 题`"
              variant="primary"
            />
            <NbStatusBadge
              v-if="currentQuestion?.turnType === 'follow_up'"
              label="追问"
              variant="ai"
            />
          </div>

          <div class="interview-room__question-text">
            {{ currentQuestion?.questionText || '' }}
          </div>

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
                正在聆听...
              </span>
            </div>
          </div>

          <div class="interview-room__controls">
            <NbButton variant="accent" @click="stopRecording">结束回答</NbButton>
            <NbButton variant="secondary" @click="toggleInputMode(finalText)">切换为文字</NbButton>
          </div>
        </NbCard>

        <NbCard
          v-else
          variant="ai"
          class="interview-room__question-card"
        >
          <div class="interview-room__question-header">
            <NbStatusBadge
              :label="`第 ${currentQuestionNo} 题`"
              variant="primary"
            />
            <NbStatusBadge
              v-if="currentQuestion?.turnType === 'follow_up'"
              label="追问"
              variant="ai"
            />
            <div class="interview-room__status-indicator">
              <span
                class="interview-room__status-pill"
                :class="`interview-room__status-pill--${statusColor}`"
              >
                <span
                  v-if="isSpinnerState"
                  class="interview-room__status-spinner"
                ></span>
                {{ statusText }}
              </span>
            </div>
          </div>

          <div class="interview-room__question-text">
            <template v-if="currentQuestion?.questionText">
              {{ currentQuestion.questionText }}
            </template>
            <template v-else-if="state === 'playingQuestion'">
              AI 正在提问，请稍候...
            </template>
            <template v-else>
              正在准备题目...
            </template>
          </div>

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
                {{ '等待回答' }}
              </span>
            </div>
          </div>

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
                @click="submitAnswer"
              >
                提交回答
              </NbButton>
            </template>
            <NbButton variant="secondary" @click="toggleInputMode(finalText)">
              {{ isVoiceMode ? '切换为文字' : '切换为语音' }}
            </NbButton>
          </div>
          <div v-else-if="isSpinnerState" class="interview-room__spinner">
            <span class="interview-room__status-spinner"></span>
            <span>{{ spinnerText }}</span>
          </div>
        </NbCard>

        <NbCard
          v-if="state === 'completed'"
          variant="success"
          class="interview-room__completed-card"
        >
          <div class="interview-room__completed">
            <div class="interview-room__completed-icon"></div>
            <p>面试结束！正在生成报告...</p>
          </div>
        </NbCard>
      </div>

      <div class="interview-room__right">
        <NbCard class="interview-room__sidebar">
          <h3 class="interview-room__sidebar-title">面试信息</h3>
          <div v-if="sessionInfo" class="interview-room__sidebar-content">
            <div class="interview-room__sidebar-item">
              <span class="interview-room__sidebar-label">目标岗位</span>
              <span>{{ sessionInfo.targetPosition }}</span>
            </div>
            <div v-if="sessionInfo.techDirection" class="interview-room__sidebar-item">
              <span class="interview-room__sidebar-label">技术方向</span>
              <span>{{ sessionInfo.techDirection }}</span>
            </div>
          </div>
        </NbCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import NbStatusBadge from '@/components/NbStatusBadge.vue'
import { useInterviewStore } from '@/stores/interview'
import { useUserStore } from '@/stores/user'
import { AsrClient } from '@/utils/audio/asrClient'
import { downsampleBuffer, floatTo16BitPCM } from '@/utils/audio/pcm'
import type { InterviewQuestionVO } from '@/types/interview'
import { useInterviewInputMode } from '@/composables/useInterviewInputMode'

type RoomState =
  | 'idle'
  | 'loadingQuestion'
  | 'playingQuestion'
  | 'readyToAnswer'
  | 'recording'
  | 'recognizing'
  | 'submittingAnswer'
  | 'generatingNext'
  | 'completed'
  | 'error'

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()
const userStore = useUserStore()

const sessionId = computed(() => Number(route.params.id) || 0)

const state = ref<RoomState>('idle')
const currentQuestion = ref<InterviewQuestionVO | null>(null)
const currentQuestionNo = ref(0)
const totalQuestions = ref(5)
const partialText = ref('')
const finalText = ref('')
const errorMessage = ref('')
const sessionInfo = ref<{ targetPosition: string; techDirection: string } | null>(null)

const remainingSeconds = ref<number | null>(null)
let timerInterval: ReturnType<typeof setInterval> | null = null

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
    finalText.value = ''
    partialText.value = ''
    if (state.value === 'recording') {
      stopRecordingWithoutSubmit()
    }
  },
})

function startTimer() {
  if (!interviewStore.currentSession?.startedAt || !interviewStore.currentSession?.durationMinutes) return
  const started = new Date(interviewStore.currentSession.startedAt).getTime()
  const end = started + interviewStore.currentSession.durationMinutes * 60 * 1000

  const update = () => {
    const now = Date.now()
    remainingSeconds.value = Math.max(0, Math.floor((end - now) / 1000))
    if (remainingSeconds.value <= 0 && timerInterval) {
      clearInterval(timerInterval)
      timerInterval = null
    }
  }
  update()
  timerInterval = setInterval(update, 1000)
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

let asrClient: AsrClient | null = null
let audioContext: AudioContext | null = null
let mediaStream: MediaStream | null = null
let scriptProcessor: ScriptProcessorNode | null = null
let recordingStartTime = 0
let currentAudio: HTMLAudioElement | null = null

const statusColor = computed(() => {
  const map: Record<RoomState, string> = {
    idle: 'muted',
    loadingQuestion: 'warning',
    playingQuestion: 'primary',
    readyToAnswer: 'success',
    recording: 'danger',
    recognizing: 'warning',
    submittingAnswer: 'warning',
    generatingNext: 'warning',
    completed: 'success',
    error: 'danger',
  }
  return map[state.value] || 'muted'
})

const statusText = computed(() => {
  const map: Record<RoomState, string> = {
    idle: '准备中',
    loadingQuestion: '正在加载题目...',
    playingQuestion: 'AI 正在提问',
    readyToAnswer: '请开始回答',
    recording: '正在录音',
    recognizing: '正在识别...',
    submittingAnswer: '正在提交回答...',
    generatingNext: '正在生成下一题...',
    completed: '面试结束',
    error: errorMessage.value || '发生错误',
  }
  return map[state.value] || ''
})

const isSpinnerState = computed(() =>
  ['loadingQuestion', 'playingQuestion', 'recognizing', 'submittingAnswer', 'generatingNext'].includes(state.value),
)

const spinnerText = computed(() => {
  const map: Record<string, string> = {
    loadingQuestion: '加载题目中...',
    playingQuestion: '播放问题中...',
    recognizing: '语音识别中...',
    submittingAnswer: '提交回答中...',
    generatingNext: '生成下一题...',
  }
  return map[state.value] || ''
})

onMounted(async () => {
  await interviewStore.fetchSession(sessionId.value)
  const session = interviewStore.currentSession
  if (session) {
    sessionInfo.value = {
      targetPosition: session.targetPosition,
      techDirection: session.techDirection,
    }
    totalQuestions.value = session.totalQuestions || 5
    currentQuestionNo.value = session.currentQuestionNo || 0
    startTimer()
  }
})

onBeforeUnmount(() => {
  cleanupAudio()
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
  }
})

async function startInterview() {
  state.value = 'loadingQuestion'
  try {
    const res = await interviewStore.startSession(sessionId.value)
    if (res.code === 0 && res.data) {
      const question = res.data
      currentQuestion.value = question
      currentQuestionNo.value = question.questionNo
      await playQuestionAudio(question)
    } else {
      state.value = 'error'
      errorMessage.value = res.message || '加载题目失败'
    }
  } catch {
    state.value = 'error'
    errorMessage.value = '网络错误'
  }
}

function playQuestionAudio(question: InterviewQuestionVO): Promise<void> {
  return new Promise((resolve) => {
    state.value = 'playingQuestion'
    try {
      if (currentAudio) {
        currentAudio.pause()
        currentAudio = null
      }

      const binaryStr = atob(question.ttsAudioBase64)
      const bytes = new Uint8Array(binaryStr.length)
      for (let i = 0; i < binaryStr.length; i++) {
        bytes[i] = binaryStr.charCodeAt(i)
      }
      const blob = new Blob([bytes], { type: 'audio/mp3' })
      const url = URL.createObjectURL(blob)

      const audio = new Audio(url)
      currentAudio = audio

      audio.onended = () => {
        URL.revokeObjectURL(url)
        currentAudio = null
        state.value = 'readyToAnswer'
        resolve()
      }

      audio.onerror = () => {
        URL.revokeObjectURL(url)
        currentAudio = null
        state.value = 'readyToAnswer'
        resolve()
      }

      audio.play().catch(() => {
        state.value = 'readyToAnswer'
        resolve()
      })
    } catch {
      state.value = 'readyToAnswer'
      resolve()
    }
  })
}

async function startRecording() {
  partialText.value = ''
  finalText.value = ''
  state.value = 'recording'
  recordingStartTime = Date.now()

  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        sampleRate: 16000,
        echoCancellation: true,
        noiseSuppression: true,
      },
    })

    audioContext = new AudioContext()
    const source = audioContext.createMediaStreamSource(mediaStream)

    scriptProcessor = audioContext.createScriptProcessor(4096, 1, 1)
    source.connect(scriptProcessor)
    scriptProcessor.connect(audioContext.destination)

    const token = userStore.token || ''
    const turnId = currentQuestion.value?.turnId || 0

    asrClient = new AsrClient({
      sessionId: sessionId.value,
      turnId,
      token,
      onPartial: (text) => {
        partialText.value = text
      },
      onFinal: (text) => {
        finalText.value += text
        partialText.value = ''
      },
      onError: (msg) => {
        ElMessage.warning(`语音识别: ${msg}`)
      },
    })
    asrClient.connect()

    scriptProcessor.onaudioprocess = (event) => {
      if (state.value !== 'recording' || !asrClient) return
      const inputData = event.inputBuffer.getChannelData(0)
      const downsampled = downsampleBuffer(inputData, audioContext!.sampleRate, 16000)
      const pcmBuffer = floatTo16BitPCM(downsampled)
      asrClient.sendAudio(pcmBuffer)
    }
  } catch {
    state.value = 'error'
    errorMessage.value = '无法访问麦克风，请检查浏览器权限'
  }
}

function stopRecording() {
  state.value = 'recognizing'
  stopRecordingWithoutSubmit()
  setTimeout(() => {
    submitAnswer()
  }, 1500)
}

function stopRecordingWithoutSubmit() {
  if (asrClient) {
    asrClient.sendEnd()
  }
  cleanupAudio()
}

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

function retryLastAction() {
  if (currentQuestion.value) {
    state.value = 'readyToAnswer'
    finalText.value = ''
    partialText.value = ''
  } else {
    state.value = 'idle'
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要结束本次面试吗？已回答的题目不会被保存。', '结束面试', {
      confirmButtonText: '确认结束',
      cancelButtonText: '继续面试',
      type: 'warning',
    })
    cleanupAudio()
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
    router.push('/interview')
  } catch {
    // cancelled
  }
}

function cleanupAudio() {
  if (scriptProcessor) {
    scriptProcessor.disconnect()
    scriptProcessor = null
  }
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((track) => track.stop())
    mediaStream = null
  }
  if (asrClient) {
    asrClient.close()
    asrClient = null
  }
}
</script>

<style scoped>
.interview-room {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 64px);
  background: var(--nb-bg);
}

.interview-room__top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  box-shadow: var(--nb-shadow);
}

.interview-room__timer {
  position: fixed;
  top: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: var(--nb-radius);
  background: var(--nb-surface);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 18px;
  z-index: 9999;
  color: var(--nb-ink);
}

.interview-room__timer--warning {
  border-color: var(--nb-danger);
  background: rgba(232, 67, 147, 0.08);
}

.interview-room__timer--warning .interview-room__timer-icon {
  background: var(--nb-danger);
  animation: nb-pulse 1s ease-in-out infinite;
}

.interview-room__timer-icon {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--nb-success);
  flex-shrink: 0;
}

.interview-room__timer-text {
  font-variant-numeric: tabular-nums;
}

.interview-room__progress {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-family: var(--font-heading);
}

.interview-room__progress-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--nb-primary);
}

.interview-room__progress-sep {
  font-size: 18px;
  color: var(--nb-muted);
}

.interview-room__progress-total {
  font-size: 18px;
  font-weight: 600;
  color: var(--nb-muted);
}

.interview-room__main {
  display: flex;
  flex: 1;
  gap: 24px;
  padding: 24px 32px;
}

.interview-room__left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.interview-room__right {
  width: 280px;
  flex-shrink: 0;
}

.interview-room__idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 48px 0;
}

.interview-room__idle-icon {
  width: 64px;
  height: 64px;
  border: 3px solid var(--nb-primary);
  border-radius: 50%;
  position: relative;
}

.interview-room__idle-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--nb-primary);
}

.interview-room__idle p {
  font-size: 18px;
  color: var(--nb-muted);
  margin: 0;
}

.interview-room__question-card {
  min-height: 400px;
}

.interview-room__question-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.interview-room__status-indicator {
  margin-left: auto;
}

.interview-room__status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: var(--nb-radius-sm);
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  border: 1px solid;
}

.interview-room__status-pill--muted {
  background: var(--nb-muted-surface);
  color: var(--nb-muted);
  border-color: var(--nb-muted);
}

.interview-room__status-pill--primary {
  background: rgba(108, 92, 231, 0.12);
  color: var(--nb-primary);
  border-color: var(--nb-primary);
}

.interview-room__status-pill--success {
  background: rgba(0, 184, 148, 0.12);
  color: var(--nb-success);
  border-color: var(--nb-success);
}

.interview-room__status-pill--warning {
  background: rgba(253, 203, 110, 0.22);
  color: var(--nb-ink);
  border-color: var(--nb-warning);
}

.interview-room__status-pill--danger {
  background: rgba(232, 67, 147, 0.12);
  color: var(--nb-danger);
  border-color: var(--nb-danger);
}

.interview-room__status-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: nb-spin 0.8s linear infinite;
}

@keyframes nb-spin {
  to { transform: rotate(360deg); }
}

.interview-room__question-text {
  font-size: 20px;
  line-height: 1.6;
  margin-bottom: 24px;
  padding: 16px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  min-height: 80px;
}

.interview-room__recording-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  margin-bottom: 20px;
  background: rgba(253, 203, 110, 0.15);
  border: 2px solid var(--nb-warning);
  border-radius: var(--nb-radius);
}

.interview-room__rec-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--nb-danger);
  animation: nb-pulse 1.2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes nb-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.3);
  }
}

.interview-room__rec-label {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--nb-danger);
}

.interview-room__transcript {
  margin-bottom: 24px;
}

.interview-room__transcript-label {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: var(--nb-muted);
  margin-bottom: 8px;
}

.interview-room__transcript-content {
  min-height: 100px;
  max-height: 200px;
  overflow-y: auto;
  padding: 12px;
  background: var(--nb-bg);
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  font-size: 15px;
  line-height: 1.6;
}

.interview-room__transcript-final {
  color: var(--nb-text);
}

.interview-room__transcript-partial {
  color: var(--nb-muted);
}

.interview-room__transcript-placeholder {
  color: var(--nb-muted);
  font-style: italic;
}

.interview-room__controls {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.interview-room__spinner {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--nb-muted);
  font-size: 14px;
}

.interview-room__error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 48px 0;
}

.interview-room__error-icon {
  width: 56px;
  height: 56px;
  border: 3px solid var(--nb-danger);
  border-radius: 50%;
  position: relative;
}

.interview-room__error-icon::after {
  content: '!';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-family: var(--font-heading);
  font-size: 32px;
  font-weight: 700;
  color: var(--nb-danger);
}

.interview-room__error-message {
  font-size: 16px;
  color: var(--nb-danger);
  margin: 0;
  text-align: center;
}

.interview-room__completed-card {
  text-align: center;
}

.interview-room__completed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 0;
}

.interview-room__completed-icon {
  width: 56px;
  height: 56px;
  border: 3px solid var(--nb-success);
  border-radius: 50%;
  position: relative;
}

.interview-room__completed-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 22px;
  height: 12px;
  border: solid var(--nb-success);
  border-width: 0 0 4px 4px;
  transform: translate(-50%, -70%) rotate(-45deg);
}

.interview-room__completed p {
  font-size: 18px;
  color: var(--nb-muted);
  margin: 0;
}

.interview-room__sidebar-title {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
}

.interview-room__sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.interview-room__sidebar-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.interview-room__sidebar-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--nb-muted);
}

@media (max-width: 768px) {
  .interview-room__main {
    flex-direction: column;
    padding: 16px;
  }

  .interview-room__right {
    width: 100%;
  }
}

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
</style>
