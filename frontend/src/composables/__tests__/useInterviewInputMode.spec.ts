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

  it('toggles back to voice mode and clears textAnswer', () => {
    const onSwitchToVoice = vi.fn()
    const { toggleInputMode, isVoiceMode, textAnswer } = useInterviewInputMode({
      onSwitchToVoice,
    })

    toggleInputMode('已识别文本')
    toggleInputMode()

    expect(onSwitchToVoice).toHaveBeenCalledTimes(1)
    expect(isVoiceMode.value).toBe(true)
    expect(textAnswer.value).toBe('')
  })

  it('setTextAnswer updates value', () => {
    const { textAnswer, setTextAnswer } = useInterviewInputMode()
    setTextAnswer('手动输入')
    expect(textAnswer.value).toBe('手动输入')
  })

  it('callbacks are optional', () => {
    const { toggleInputMode } = useInterviewInputMode()
    // should not throw
    toggleInputMode('test')
    toggleInputMode()
  })
})
