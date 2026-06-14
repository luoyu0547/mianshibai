import { describe, expect, it, vi } from 'vitest'

const create = vi.fn(() => ({
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  },
}))

vi.mock('axios', () => ({
  default: { create },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}))

describe('request', () => {
  it('uses a three minute timeout for API calls', async () => {
    await import('../request')

    expect(create).toHaveBeenCalledWith(
      expect.objectContaining({
        timeout: 180000,
      }),
    )
  })
})
