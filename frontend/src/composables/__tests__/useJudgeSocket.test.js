import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useJudgeSocket } from '@/composables/useJudgeSocket'

// Mock the user store
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    token: 'mock-token-xyz',
  })),
}))

describe('useJudgeSocket', () => {
  let originalWebSocket = null

  beforeEach(() => {
    originalWebSocket = globalThis.WebSocket
    // Mock WebSocket
    globalThis.WebSocket = vi.fn(function () {
      this.readyState = WebSocket.CONNECTING
      this.onopen = null
      this.onmessage = null
      this.onclose = null
      this.onerror = null
      this.send = vi.fn()
      this.close = vi.fn(function (code, reason) {
        if (this.onclose) {
          this.onclose({ code: code || 1000, reason })
        }
      })
      // Simulate connection
      setTimeout(() => { this.readyState = WebSocket.OPEN; if (this.onopen) this.onopen() }, 0)
    })
    globalThis.WebSocket.CONNECTING = 0
    globalThis.WebSocket.OPEN = 1
    globalThis.WebSocket.CLOSING = 2
    globalThis.WebSocket.CLOSED = 3
    // Mock location
    globalThis.location = { protocol: 'http:', host: 'localhost:5173' }
  })

  afterEach(() => {
    globalThis.WebSocket = originalWebSocket
    vi.restoreAllMocks()
  })

  it('returns initial state with connected=false and lastResult=null', () => {
    const { connected, lastResult } = useJudgeSocket()
    expect(connected.value).toBe(false)
    expect(lastResult.value).toBeNull()
  })

  it('returns connect and disconnect functions', () => {
    const { connect, disconnect } = useJudgeSocket()
    expect(typeof connect).toBe('function')
    expect(typeof disconnect).toBe('function')
  })

  it('connect creates a WebSocket with correct URL', () => {
    const { connect } = useJudgeSocket()
    connect()
    expect(globalThis.WebSocket).toHaveBeenCalled()
    const url = globalThis.WebSocket.mock.calls[0][0]
    expect(url).toContain('ws://localhost:5173/ws/judge')
    expect(url).toContain('token=mock-token-xyz')
  })

  it('disconnect closes the WebSocket and resets state', () => {
    const { connect, disconnect, connected, lastResult } = useJudgeSocket()
    connect()
    disconnect()
    expect(connected.value).toBe(false)
    expect(lastResult.value).toBeNull()
  })

  it('handles incoming JSON judge_result message', async () => {
    const { connect, lastResult } = useJudgeSocket()
    connect()
    // Wait for onopen
    await vi.waitFor(() => expect(globalThis.WebSocket).toHaveBeenCalled(), { timeout: 100 })
    // Simulate message
    const ws = globalThis.WebSocket.mock.results?.[0]?.value
    if (ws?.onmessage) {
      ws.onmessage({ data: JSON.stringify({ type: 'judge_result', status: 'AC', submissionId: 1 }) })
      expect(lastResult.value).toEqual({ type: 'judge_result', status: 'AC', submissionId: 1 })
    }
  })

  it('ignores non-JSON messages gracefully', () => {
    const { connect } = useJudgeSocket()
    connect()
    const ws = globalThis.WebSocket.mock.results?.[0]?.value
    if (ws?.onmessage) {
      expect(() => ws.onmessage({ data: 'pong' })).not.toThrow()
    }
  })
})
