export function downsampleBuffer(input: Float32Array, inputSampleRate: number, outputSampleRate = 16000): Float32Array {
  if (inputSampleRate === outputSampleRate) {
    return input
  }
  const ratio = inputSampleRate / outputSampleRate
  const outputLength = Math.round(input.length / ratio)
  const output = new Float32Array(outputLength)
  for (let i = 0; i < outputLength; i++) {
    const start = Math.round(i * ratio)
    const end = Math.min(Math.round((i + 1) * ratio), input.length)
    let sum = 0
    for (let j = start; j < end; j++) {
      sum += input[j]!
    }
    output[i] = sum / (end - start)
  }
  return output
}

export function floatTo16BitPCM(input: Float32Array): ArrayBuffer {
  const buffer = new ArrayBuffer(input.length * 2)
  const view = new DataView(buffer)
  for (let i = 0; i < input.length; i++) {
    const s = Math.max(-1, Math.min(1, input[i]!))
    view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true)
  }
  return buffer
}
