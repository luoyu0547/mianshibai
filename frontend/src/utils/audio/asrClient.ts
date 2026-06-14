export interface AsrClientOptions {
  sessionId: number
  turnId: number
  token: string
  onPartial: (text: string) => void
  onFinal: (text: string) => void
  onError: (message: string) => void
}

export class AsrClient {
  private ws: WebSocket | null = null
  private options: AsrClientOptions

  constructor(options: AsrClientOptions) {
    this.options = options
  }

  connect(): void {
    const baseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
    const wsUrl = baseUrl
      ? baseUrl.replace(/^http/, 'ws').replace(/\/$/, '')
      : `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`
    const url = `${wsUrl}/ws/interview/asr?sessionId=${this.options.sessionId}&turnId=${this.options.turnId}&token=${encodeURIComponent(this.options.token)}`

    this.ws = new WebSocket(url)
    this.ws.binaryType = 'arraybuffer'

    this.ws.onopen = () => {
      this.sendStart()
    }

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'ASR_PARTIAL') {
          this.options.onPartial(data.text)
        } else if (data.type === 'ASR_FINAL') {
          this.options.onFinal(data.text)
        } else if (data.type === 'ERROR') {
          this.options.onError(data.message)
        }
      } catch {
        // ignore non-JSON messages
      }
    }

    this.ws.onerror = () => {
      this.options.onError('WebSocket 连接失败')
    }

    this.ws.onclose = () => {
      this.ws = null
    }
  }

  sendStart(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'START',
        sessionId: this.options.sessionId,
        turnId: this.options.turnId,
      }))
    }
  }

  sendAudio(buffer: ArrayBuffer): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(buffer)
    }
  }

  sendEnd(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type: 'END' }))
    }
  }

  close(): void {
    if (this.ws) {
      this.sendEnd()
      this.ws.close()
      this.ws = null
    }
  }

  get isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }
}
