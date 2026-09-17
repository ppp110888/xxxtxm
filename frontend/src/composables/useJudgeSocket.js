import { ref, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 判题结果 WebSocket 客户端
 *
 * 用法:
 *   const { connected, lastResult, connect, disconnect } = useJudgeSocket()
 *   onMounted(() => connect())
 *   watch(lastResult, (r) => { if (r) handleResult(r) })
 */
export function useJudgeSocket() {
  const connected = ref(false)
  const lastResult = ref(null)
  let socket = null
  let pingTimer = null
  let reconnectTimer = null
  let reconnectAttempts = 0
  const MAX_RECONNECT = 5

  function connect() {
    const userStore = useUserStore()
    if (!userStore.token) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${location.host}/ws/judge?token=${encodeURIComponent(userStore.token)}`

    try {
      socket = new WebSocket(wsUrl)

      socket.onopen = () => {
        connected.value = true
        reconnectAttempts = 0
        // 心跳：每 30s 发送 ping
        pingTimer = setInterval(() => {
          if (socket?.readyState === WebSocket.OPEN) {
            socket.send('ping')
          }
        }, 30000)
      }

      socket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'judge_result') {
            lastResult.value = data
          }
        } catch { /* ignore non-JSON (e.g., pong) */ }
      }

      socket.onclose = (event) => {
        connected.value = false
        clearInterval(pingTimer)
        // 非正常关闭 + 未达最大重连次数 → 自动重连
        if (event.code !== 1000 && reconnectAttempts < MAX_RECONNECT) {
          reconnectAttempts++
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000)
          reconnectTimer = setTimeout(connect, delay)
        }
      }

      socket.onerror = () => {
        // onclose 会在 onerror 之后触发，重连逻辑在 onclose 中
      }
    } catch {
      connected.value = false
    }
  }

  function disconnect() {
    clearInterval(pingTimer)
    clearTimeout(reconnectTimer)
    if (socket) {
      socket.close(1000, 'Client disconnect')
      socket = null
    }
    connected.value = false
    lastResult.value = null
  }

  onBeforeUnmount(disconnect)

  return { connected, lastResult, connect, disconnect }
}
