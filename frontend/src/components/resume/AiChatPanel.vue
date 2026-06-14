<!-- src/components/resume/AiChatPanel.vue -->
<template>
  <div class="ai-chat-panel">
    <div v-if="messages.length === 0 && !isLoading" class="ai-chat-panel__empty">
      <NbEmptyState
        variant="ai"
        title="AI 简历助手"
        description="告诉 AI 你的需求，它会帮你优化简历内容"
      />
    </div>

    <div v-else class="ai-chat-panel__messages" ref="messagesContainer">
      <template v-for="(msg, index) in messages" :key="index">
        <div
          v-show="!isLoading || msg.content || (msg.role as string) === 'user'"
          :class="['ai-chat-panel__bubble', `ai-chat-panel__bubble--${msg.role}`]">
          <span v-if="msg.role === 'assistant'" class="ai-chat-panel__role">AI</span>
          <span class="ai-chat-panel__content" v-html="renderMarkdown(msg.content)"></span>
          <div v-if="msg.role === 'assistant' && visibleProposals(msg).length" class="ai-chat-panel__proposals">
            <div
              v-for="item in visibleProposals(msg)"
              :key="item.index"
              class="ai-chat-panel__proposal"
            >
              <span>AI 建议修改{{ sectionLabel(item.proposal.sectionType) }}</span>
              <small>{{ item.proposal.reason || '等待确认后应用' }}</small>
              <div class="ai-chat-panel__proposal-actions">
                <NbButton variant="primary" size="small" @click="emit('proposal', item.proposal)">查看对比</NbButton>
                <NbButton variant="ghost" size="small" @click="ignoreProposal(msg, item.index)">忽略</NbButton>
              </div>
            </div>
          </div>
        </div>
      </template>
      <div v-if="isLoading" class="ai-chat-panel__bubble ai-chat-panel__bubble--assistant ai-chat-panel__typing">
        <span class="ai-chat-panel__role">AI</span>
        <span class="ai-chat-panel__typing-dots"><span></span><span></span><span></span></span>
      </div>
    </div>

    <div class="ai-chat-panel__input">
      <el-input
        v-model="inputText"
        placeholder="输入消息，AI 帮你优化简历..."
        :disabled="isLoading"
        @keyup.enter="handleSend"
      />
      <NbButton variant="primary" :disabled="isLoading || !inputText.trim()" @click="handleSend">
        发送
      </NbButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import type { SectionType, ChatMessageVO, ResumePatchProposal } from '@/types/resume'
import { getChatHistory } from '@/api/resume'
import { createSseParser } from '@/utils/sse'
import { Marked } from 'marked'
import NbButton from '@/components/NbButton.vue'
import NbEmptyState from '@/components/NbEmptyState.vue'

const markedInstance = new Marked({ breaks: true, gfm: true })

function renderMarkdown(text: string): string {
  if (!text) return ''
  return markedInstance.parse(text) as string
}

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  proposals?: ResumePatchProposal[]
  ignoredProposalIndexes?: number[]
}

const props = defineProps<{
  resumeId: number
}>()

const emit = defineEmits<{
  extracted: [sectionType: SectionType, sectionData: Record<string, unknown>]
  proposal: [proposal: ResumePatchProposal]
}>()

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLDivElement>()

const sectionLabelMap: Record<SectionType, string> = {
  basic: '基本信息',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '技能标签',
  summary: '自我评价',
}

function sectionLabel(type: SectionType) {
  return sectionLabelMap[type] || type
}

function visibleProposals(msg: ChatMessage) {
  const ignored = new Set(msg.ignoredProposalIndexes || [])
  return (msg.proposals || [])
    .map((proposal, index) => ({ proposal, index }))
    .filter((item) => !ignored.has(item.index))
}

function ignoreProposal(msg: ChatMessage, index: number) {
  msg.ignoredProposalIndexes = [...(msg.ignoredProposalIndexes || []), index]
}

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
      if (res.code === 0 && res.data) {
        messages.value = res.data.map((m: ChatMessageVO) => ({
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

    const parser = createSseParser((event) => {
      if (event.event === 'resume_patch_proposal') {
        try {
          const proposal = JSON.parse(event.data) as ResumePatchProposal
          if (proposal.operation === 'replace_section' && proposal.sectionType && proposal.sectionData) {
            assistantMsg.proposals = [...(assistantMsg.proposals || []), proposal]
          }
        } catch {
          // ignore unparseable proposal events
        }
        return
      }

      if (event.data && event.data !== '[DONE]') {
        assistantMsg.content += event.data
      }
      scrollToBottom()
    })

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      parser.push(decoder.decode(value, { stream: true }))
    }
    parser.flush()
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

.ai-chat-panel__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.ai-chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-chat-panel__bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: var(--nb-radius);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.ai-chat-panel__bubble--user {
  align-self: flex-end;
  background: var(--nb-primary);
  color: #fff;
  border: var(--nb-border);
  box-shadow: var(--nb-shadow);
  flex-direction: row-reverse;
}

.ai-chat-panel__bubble--assistant {
  align-self: flex-start;
  background: var(--nb-surface);
  color: var(--nb-ink);
  border: var(--nb-border);
  box-shadow: var(--nb-shadow-xs);
}

.ai-chat-panel__role {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  background: var(--nb-accent);
  color: #fff;
  border: var(--nb-border);
  border-radius: var(--nb-radius-sm);
  font-family: var(--font-heading);
  font-size: 10px;
  font-weight: 700;
}

.ai-chat-panel__bubble--user .ai-chat-panel__role {
  display: none;
}

.ai-chat-panel__content {
  flex: 1;
  min-width: 0;
}

.ai-chat-panel__typing {
  align-items: center;
}

.ai-chat-panel__typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.ai-chat-panel__typing-dots span {
  width: 6px;
  height: 6px;
  background: var(--nb-muted);
  border-radius: 50%;
  animation: chat-typing 1.2s infinite;
}

.ai-chat-panel__typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-chat-panel__typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes chat-typing {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.ai-chat-panel__input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: var(--nb-border);
  background: var(--nb-surface);
}

.ai-chat-panel__proposals {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  width: 100%;
}

.ai-chat-panel__proposal {
  padding: 10px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: var(--nb-primary-light);
}

.ai-chat-panel__proposal small {
  display: block;
  margin-top: 4px;
  color: var(--nb-muted);
}

.ai-chat-panel__proposal-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
