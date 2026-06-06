import { describe, it, expect } from 'vitest'
import { downsampleBuffer, floatTo16BitPCM } from '../pcm'

describe('downsampleBuffer', () => {
  it('returns input when rates match', () => {
    const input = new Float32Array([0.5, -0.5, 0.25])
    const result = downsampleBuffer(input, 16000, 16000)
    expect(result).toBe(input)
  })

  it('downsamples 48kHz to 16kHz correctly', () => {
    const input = new Float32Array(48000).fill(0.5)
    const result = downsampleBuffer(input, 48000, 16000)
    expect(result.length).toBe(16000)
  })
})

describe('floatTo16BitPCM', () => {
  it('converts Float32 to Int16 ArrayBuffer', () => {
    const input = new Float32Array([0, 1, -1])
    const result = floatTo16BitPCM(input)
    expect(result.byteLength).toBe(6)
    const view = new DataView(result)
    expect(view.getInt16(0, true)).toBe(0)
    expect(view.getInt16(2, true)).toBe(32767)
    expect(view.getInt16(4, true)).toBe(-32768)
  })

  it('clamps values beyond [-1, 1]', () => {
    const input = new Float32Array([2, -2])
    const result = floatTo16BitPCM(input)
    const view = new DataView(result)
    expect(view.getInt16(0, true)).toBe(32767)
    expect(view.getInt16(2, true)).toBe(-32768)
  })
})
