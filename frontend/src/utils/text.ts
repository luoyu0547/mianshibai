export function displayText(value: string | null | undefined, fallback = '暂无') {
  const text = value?.trim()
  return text ? text : fallback
}

export function splitTags(value: string | null | undefined) {
  return (value ?? '')
    .split(/[,，、;；\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}
