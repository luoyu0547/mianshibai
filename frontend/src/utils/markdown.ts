import { Marked } from 'marked'

const markedInstance = new Marked({ breaks: true, gfm: true })

export function renderMarkdown(text: string): string {
  if (!text) return ''
  return markedInstance.parse(text) as string
}
