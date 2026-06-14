export interface SseEvent {
  event: string
  data: string
}

export function createSseParser(onEvent: (event: SseEvent) => void) {
  let buffer = ''

  function dispatch(frame: string) {
    const lines = frame.split('\n')
    let eventType = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim() || 'message'
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart())
      }
    }

    if (dataLines.length > 0) {
      onEvent({ event: eventType, data: dataLines.join('\n') })
    }
  }

  return {
    push(chunk: string) {
      buffer += chunk
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''
      for (const frame of frames) {
        if (frame.trim()) dispatch(frame)
      }
    },
    flush() {
      if (buffer.trim()) {
        dispatch(buffer)
        buffer = ''
      }
    },
  }
}
