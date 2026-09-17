import { ref, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * AI SSE 流式响应 Composable
 *
 * 用法：
 *   const { content, loading, error, startStream } = useAiStream()
 *
 *   // GET 方式 (EventSource)
 *   startStream('/api/v1/student/ai/diagnose/123')
 *
 *   // POST 方式 (fetch + ReadableStream)
 *   startStream('/api/v1/student/ai/diagnose-code', {
 *     method: 'POST',
 *     body: JSON.stringify({ questionId: 1, language: 'python', code: '...' }),
 *   })
 *
 * content 会自动逐字追加，实现打字机效果。
 */
export function useAiStream() {
  const content = ref('')
  const loading = ref(false)
  const error = ref('')
  let eventSource = null
  let abortController = null

  /**
   * @param {string} url — 请求路径
   * @param {object} [options] — 可选，POST 请求配置 { method, body }
   *   不传 options 使用 EventSource GET；传入则使用 fetch POST + ReadableStream
   */
  function startStream(url, options) {
    // 重置状态
    content.value = ''
    error.value = ''
    loading.value = true

    // 关闭之前的连接
    stopStream()

    if (options) {
      startPostStream(url, options)
    } else {
      startGetStream(url)
    }
  }

  /** GET 方式 — 使用 EventSource (兼容现有 SSE 端点) */
  function startGetStream(url) {
    const userStore = useUserStore()
    const token = userStore.token || ''
    const fullUrl = `${url}${url.includes('?') ? '&' : '?'}Authorization=${encodeURIComponent(token)}`
    const baseUrl = window.location.origin
    const finalUrl = url.startsWith('http') ? fullUrl : `${baseUrl}${fullUrl}`

    eventSource = new EventSource(finalUrl)

    eventSource.onmessage = (event) => {
      if (event.data) {
        content.value += event.data
      }
    }

    eventSource.onerror = () => {
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      loading.value = false
    }

    eventSource.onopen = () => {
      // 保持 loading 状态，等待 AI 流式内容到达
      // 内容通过 onmessage 到达后，模板 v-else-if="content" 会自动切换
    }
  }

  /** POST 方式 — 使用 fetch + ReadableStream 读取 SSE 响应 */
  async function startPostStream(url, { method, body, headers: extraHeaders }) {
    const userStore = useUserStore()
    const token = userStore.token || ''
    const baseUrl = window.location.origin
    const finalUrl = url.startsWith('http') ? url : `${baseUrl}${url}`

    abortController = new AbortController()

    try {
      const response = await fetch(finalUrl, {
        method: method || 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token,
          ...(extraHeaders || {}),
        },
        body: body,
        signal: abortController.signal,
      })

      if (!response.ok) {
        error.value = `请求失败: ${response.status}`
        loading.value = false
        return
      }

      // 逐块读取 SSE 流
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE data 行
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 保留未完成的行

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (data) {
              content.value += data
            }
          }
        }
      }

      // 处理剩余的 buffer
      if (buffer.startsWith('data:')) {
        const data = buffer.slice(5).trim()
        if (data) {
          content.value += data
        }
      }
    } catch (e) {
      if (e.name === 'AbortError') return // 用户主动取消
      error.value = '连接中断'
    } finally {
      loading.value = false
      abortController = null
    }
  }

  function stopStream() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    loading.value = false
  }

  onBeforeUnmount(() => {
    stopStream()
  })

  return { content, loading, error, startStream, stopStream }
}
