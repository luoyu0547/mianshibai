<!-- src/components/resume/AiChatPanel.vue -->
<template>
  <div class="ai-chat-panel">
    <div class="ai-chat-panel__messages" ref="messagesContainer">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['ai-chat-panel__bubble', `ai-chat-panel__bubble--${msg.role}`]"
      >
        {{ msg.content }}
      </div>
      <div v-if="isLoading" class="ai-chat-panel__bubble ai-chat-panel__bubble--assistant ai-chat-panel__typing">
        <span></span><span></span><span></span>
      </div>
    </div>
    <div class="ai-chat-panel__input">
      <el-input
        v-model="inputText"
        placeholder="输入消息，AI 帮你优化简历..."
        :disabled="isLoading"
        @keyup.enter="handleSend"
      />
      <el-button type="primary" :disabled="isLoading || !inputText.trim()" @click="handleSend">
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import type { SectionType, ChatMessageVO } from '@/types/resume'
import { getChatHistory } from '@/api/resume'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const props = defineProps<{
  resumeId: number
}>()

const emit = defineEmits<{
  extracted: [sectionType: SectionType, sectionData: Record<string, unknown>]
}>()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLDivElement>()

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(async () => {
  if (props.resumeId) {
    try {
      const res = await getChatHistory(props.resumeId)
      if (res.data.code === 0 && res.data.data) {
        messages.value = res.data.data.map((m: ChatMessageVO) => ({
          role: m.role as 'user' | 'assistant',
          content: m.content,
        }))
        scrollToBottom()
      }
    } catch {
      // 加载失败不展示错误，保持空白面板
    }
  }
})

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isLoading.value = true
  scrollToBottom()

  const assistantMsg: ChatMessage = { role: 'assistant', content: '' }
  messages.value.push(assistantMsg)

  try {
    const token = localStorage.getItem('mianshiba_token')
    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const response = await fetch(`${baseUrl}/api/resume/${props.resumeId}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ message: text }),
    })

    if (!response.body) {
      assistantMsg.content = '无法连接到 AI 服务'
      return
    }

    const reader = (response.body as ReadableStream<Uint8Array>).getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed.startsWith('data:')) continue
        const data = trimmed.slice(5).trim()
        if (data === '[DONE]') continue

        try {
          const parsed = JSON.parse(data)
          if (parsed.content) {
            assistantMsg.content += parsed.content
          }

          const extractedMatch = assistantMsg.content.match(
            /\[EXTRACTED_DATA\]([\s\S]*?)\[\/EXTRACTED_DATA\]/,
          )
          if (extractedMatch && extractedMatch[1]) {
            const extracted = JSON.parse(extractedMatch[1])
            if (extracted.sectionType && extracted.sectionData) {
              emit('extracted', extracted.sectionType, extracted.sectionData)
            }
            assistantMsg.content = assistantMsg.content.replace(
              /\[EXTRACTED_DATA\][\s\S]*?\[\/EXTRACTED_DATA\]/,
              '',
            )
          }
        } catch {
          assistantMsg.content += data
        }
        scrollToBottom()
      }
    }
  } catch {
    assistantMsg.content = assistantMsg.content || '请求失败，请稍后重试'
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.ai-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.ai-chat-panel__messages {
  flex: 1;
  max-height: 300px;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-chat-panel__bubble {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: var(--nb-radius);
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.ai-chat-panel__bubble--user {
  align-self: flex-end;
  background: var(--nb-primary);
  color: #fff;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
}

.ai-chat-panel__bubble--assistant {
  align-self: flex-start;
  background: var(--nb-bg);
  color: var(--nb-text);
  border: var(--nb-border);
}

.ai-chat-panel__typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
}

.ai-chat-panel__typing span {
  width: 6px;
  height: 6px;
  background: var(--nb-muted);
  border-radius: 50%;
  animation: chat-typing 1.2s infinite;
}

.ai-chat-panel__typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-chat-panel__typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes chat-typing {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.ai-chat-panel__input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: var(--nb-border);
  background: var(--nb-card);
}
</style>
