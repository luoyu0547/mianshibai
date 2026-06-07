<template>
  <div class="interview-room">
    <div
      v-if="remainingSeconds !== null"
      class="interview-room__timer"
      :class="{ 'interview-room__timer--warning': remainingSeconds < 120 }"
    >
      {{ remainingSeconds < 120 ? '⚠️ ' : '' }}{{ formatTime(remainingSeconds) }}
    </div>

    <div class="interview-room__top-bar">
      <div class="interview-room__progress">
        第 {{ currentQuestionNo }} / {{ totalQuestions }} 题
      </div>
      <el-button text type="danger" @click="handleCancel">结束面试</el-button>
    </div>

    <div class="interview-room__main">
      <div class="interview-room__left">
        <NbCard class="interview-room__question-card">
          <div v-if="state === 'idle'" class="interview-room__idle">
            <div class="interview-room__idle-icon">🎙️</div>
            <p>准备好开始面试了吗？</p>
            <NbButton type="primary" @click="startInterview">开始面试</NbButton>
          </div>

          <template v-else>
            <div class="interview-room__question-header">
              <span
                class="interview-room__status-dot"
                :class="`interview-room__status-dot--${statusColor}`"
              ></span>
              <span class="interview-room__status-text">{{ statusText }}</span>
            </div>

            <div class="interview-room__question-text">
              {{ currentQuestion?.questionText || '' }}
            </div>

            <div class="interview-room__transcript">
              <div class="interview-room__transcript-label">实时语音识别</div>
              <div class="interview-room__transcript-content">
                <span v-if="partialText" class="interview-room__transcript-partial">{{ partialText }}</span>
                <span v-if="finalText" class="interview-room__transcript-final">{{ finalText }}</span>
                <span v-if="!partialText && !finalText" class="interview-room__transcript-placeholder">
                  {{ state === 'recording' ? '正在聆听...' : '等待回答' }}
                </span>
              </div>
            </div>

            <div class="interview-room__controls">
              <NbButton
                v-if="state === 'readyToAnswer'"
                type="primary"
                :disabled="remainingSeconds !== null && remainingSeconds <= 0"
                @click="startRecording"
              >
                开始回答
              </NbButton>
              <NbButton
                v-if="state === 'recording'"
                type="accent"
                @click="stopRecording"
              >
                结束回答
              </NbButton>
              <NbButton
                v-if="state === 'error'"
                type="primary"
                @click="retryLastAction"
              >
                重试
              </NbButton>
              <div v-if="isSpinnerState" class="interview-room__spinner">
                <el-icon class="is-loading" :size="24"><LoadingIcon /></el-icon>
                <span>{{ spinnerText }}</span>
              </div>
            </div>
          </template>
        </NbCard>
      </div>

      <div class="interview-room__right">
        <NbCard class="interview-room__sidebar">
          <h3 class="interview-room__sidebar-title">简历摘要</h3>
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
import { Loading as LoadingIcon } from '@element-plus/icons-vue'
import NbCard from '@/components/NbCard.vue'
import NbButton from '@/components/NbButton.vue'
import { useInterviewStore } from '@/stores/interview'
import { useUserStore } from '@/stores/user'
import { AsrClient } from '@/utils/audio/asrClient'
import { downsampleBuffer, floatTo16BitPCM } from '@/utils/audio/pcm'
import type { InterviewQuestionVO } from '@/types/interview'

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
    if (res.data.code === 0 && res.data.data) {
      const question = res.data.data
      currentQuestion.value = question
      currentQuestionNo.value = question.questionNo
      await playQuestionAudio(question)
    } else {
      state.value = 'error'
      errorMessage.value = res.data.message || '加载题目失败'
    }
  } catch {
    state.value = 'error'
    errorMessage.value = '网络错误'
  }
}

function playQuestionAudio(question: InterviewQuestionVO): Promise<void> {
  return new Promise((resolve, reject) => {
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
        finalText.value += (finalText.value ? '' : '') + text
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
  if (asrClient) {
    asrClient.sendEnd()
  }

  cleanupAudio()

  setTimeout(() => {
    if (asrClient) {
      asrClient.close()
      asrClient = null
    }
    submitAnswer()
  }, 1500)
}

async function submitAnswer() {
  const answer = finalText.value.trim()
  if (!answer) {
    ElMessage.warning('未检测到有效回答，请重新回答')
    state.value = 'readyToAnswer'
    finalText.value = ''
    partialText.value = ''
    return
  }

  state.value = 'submittingAnswer'
  const durationSeconds = Math.round((Date.now() - recordingStartTime) / 1000)
  const turnId = currentQuestion.value?.turnId || 0

  try {
    const res = await interviewStore.submitAnswer(sessionId.value, turnId, {
      answerText: answer,
      answerDurationSeconds: durationSeconds,
    })

    if (res.data.code === 0 && res.data.data) {
      const result = res.data.data
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
        await playQuestionAudio(result.turn)
      } else {
        state.value = 'readyToAnswer'
      }
    } else {
      state.value = 'error'
      errorMessage.value = res.data.message || '提交失败'
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
  padding: 8px 16px;
  border-radius: 8px;
  background: #f0f9eb;
  border: 2px solid #67c23a;
  font-weight: bold;
  font-size: 18px;
  z-index: 9999;
}

.interview-room__timer--warning {
  background: #fef0f0;
  border-color: #f56c6c;
}

.interview-room__progress {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
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
  font-size: 64px;
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
}

.interview-room__status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: var(--nb-border);
}

.interview-room__status-dot--muted { background: var(--nb-muted); }
.interview-room__status-dot--primary { background: var(--nb-primary); }
.interview-room__status-dot--success { background: var(--nb-success); }
.interview-room__status-dot--warning { background: var(--nb-warning); }
.interview-room__status-dot--danger { background: var(--nb-accent); }

.interview-room__status-text {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--nb-muted);
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

.interview-room__transcript-partial {
  color: var(--nb-muted);
}

.interview-room__transcript-final {
  color: var(--nb-text);
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
</style>
