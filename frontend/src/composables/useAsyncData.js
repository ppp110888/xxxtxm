import { ref, shallowRef } from 'vue'

/**
 * 统一的异步数据加载 composable
 *
 * @param {Function} fetcher — 异步数据获取函数，返回 Promise
 * @returns {{ data, loading, error, execute }}
 *
 * 用法:
 *   const { data, loading, error, execute } = useAsyncData(() => api.getXxx())
 *   onMounted(() => execute())
 */
export function useAsyncData(fetcher) {
  const data = shallowRef(null)
  const loading = ref(false)
  const error = ref('')

  async function execute() {
    loading.value = true
    error.value = ''
    try {
      data.value = await fetcher()
    } catch (e) {
      error.value = e?.message || '加载失败'
      data.value = null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}

/**
 * 带参数的异步数据加载
 *
 * @param {Function} fetcher — (params) => Promise，接收 execute 时传入的参数
 * @returns {{ data, loading, error, execute }}
 */
export function useAsyncDataWithParams(fetcher) {
  const data = shallowRef(null)
  const loading = ref(false)
  const error = ref('')

  async function execute(...args) {
    loading.value = true
    error.value = ''
    try {
      data.value = await fetcher(...args)
    } catch (e) {
      error.value = e?.message || '加载失败'
      data.value = null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}
