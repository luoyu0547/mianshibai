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
    textAnswer.value = ''
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
