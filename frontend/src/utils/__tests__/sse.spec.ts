import { describe, expect, it } from 'vitest'
import { createSseParser } from '../sse'

describe('createSseParser', () => {
  it('parses plain message events incrementally', () => {
    const events: Array<{ event: string; data: string }> = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('data: hello')
    parser.push(' world\n\n')

    expect(events).toEqual([{ event: 'message', data: 'hello world' }])
  })

  it('parses named resume_patch_proposal events', () => {
    const events: Array<{ event: string; data: string }> = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('event: resume_patch_proposal\n')
    parser.push('data: {"sectionType":"summary"}\n\n')

    expect(events).toEqual([
      { event: 'resume_patch_proposal', data: '{"sectionType":"summary"}' },
    ])
  })

  it('ignores empty frames', () => {
    const events: Array<{ event: string; data: string }> = []
    const parser = createSseParser((event) => events.push(event))

    parser.push('\n\n')

    expect(events).toEqual([])
  })
})
