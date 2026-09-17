<template>
  <div ref="editorContainer" class="monaco-editor-container" :style="{ height: height }" />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'python' },
  height: { type: String, default: '100%' },
  readOnly: { type: Boolean, default: false },
  theme: { type: String, default: 'vs-dark' },
})

const emit = defineEmits(['update:modelValue', 'mount'])

const editorContainer = ref(null)
const editor = shallowRef(null)
const completionProvider = shallowRef(null)

// ── 语言映射 ──
const langMap = {
  python: 'python',
  java: 'java',
  py: 'python',
  js: 'javascript',
}

function getLanguage(lang) {
  return langMap[lang] || lang || 'plaintext'
}

// ── 代码片段 ──
const PYTHON_SNIPPETS = [
  { label: 'solve', insertText: 'def solve():\n    ${1}\n\nif __name__ == "__main__":\n    solve()\n', detail: 'OJ 解题模板', category: '模板' },
  { label: 'input_int', insertText: 'int(input())', detail: '读取一个整数', category: '输入' },
  { label: 'input_list', insertText: 'list(map(int, input().split()))', detail: '读取一行整数列表', category: '输入' },
  { label: 'input_str', insertText: 'input().strip()', detail: '读取一行字符串', category: '输入' },
  { label: 'input_multi', insertText: '${1:a}, ${2:b} = map(int, input().split())', detail: '读取多个变量', category: '输入' },
  { label: 'for_range', insertText: 'for ${1:i} in range(${2:n}):\n    ${3}', detail: 'for 循环 (range)', category: '循环' },
  { label: 'for_list', insertText: 'for ${1:item} in ${2:items}:\n    ${3}', detail: 'for 遍历列表', category: '循环' },
  { label: 'if_else', insertText: 'if ${1:condition}:\n    ${2}\nelse:\n    ${3}', detail: 'if-else 分支', category: '条件' },
  { label: 'print_f', insertText: "print(f'${1:}={${1}}')", detail: 'f-string 打印', category: '输出' },
  { label: 'sort_list', insertText: 'sorted(${1:list}, key=lambda x: ${2:x})', detail: '排序列表', category: '工具' },
  { label: 'dict_get', insertText: '${1:dict}.get(${2:key}, ${3:default})', detail: '字典安全取值', category: '工具' },
]

const JAVA_SNIPPETS = [
  { label: 'main', insertText: 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        ${1}\n        sc.close();\n    }\n}\n', detail: 'OJ 解题模板 (含 import)', category: '模板' },
  { label: 'scanner', insertText: 'Scanner sc = new Scanner(System.in);\n${1}\nsc.close();', detail: 'Scanner 输入模板', category: '输入' },
  { label: 'readInt', insertText: 'sc.nextInt()', detail: '读取 int', category: '输入' },
  { label: 'readLine', insertText: 'sc.nextLine()', detail: '读取一行字符串', category: '输入' },
  { label: 'readLong', insertText: 'sc.nextLong()', detail: '读取 long', category: '输入' },
  { label: 'println', insertText: 'System.out.println(${1});', detail: '打印并换行', category: '输出' },
  { label: 'for_i', insertText: 'for (int ${1:i} = 0; ${1:i} < ${2:n}; ${1:i}++) {\n    ${3}\n}', detail: 'for 循环', category: '循环' },
  { label: 'for_each', insertText: 'for (${1:Type} ${2:item} : ${3:items}) {\n    ${4}\n}', detail: '增强 for 循环', category: '循环' },
  { label: 'if_else', insertText: 'if (${1:condition}) {\n    ${2}\n} else {\n    ${3}\n}', detail: 'if-else 分支', category: '条件' },
  { label: 'sort_arr', insertText: 'Arrays.sort(${1:arr});', detail: '数组排序', category: '工具' },
  { label: 'list', insertText: 'List<${1:Type}> ${2:list} = new ArrayList<>();', detail: '创建 ArrayList', category: '集合' },
]

function getSnippets(lang) {
  if (lang === 'python' || lang === 'py') return PYTHON_SNIPPETS
  if (lang === 'java') return JAVA_SNIPPETS
  return []
}

function createCompletionProvider(lang) {
  const snippets = getSnippets(lang)
  if (!snippets.length) return null

  return monaco.languages.registerCompletionItemProvider(lang, {
    provideCompletionItems: (model, position) => {
      const word = model.getWordUntilPosition(position)
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      }

      const suggestions = snippets.map((s, i) => ({
        label: s.label,
        kind: monaco.languages.CompletionItemKind.Snippet,
        insertText: s.insertText,
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        detail: `${s.category} · ${s.detail}`,
        range,
        sortText: String(i).padStart(4, '0'),
      }))

      return { suggestions }
    },
  })
}

function updateCompletionProvider(lang) {
  if (completionProvider.value) {
    completionProvider.value.dispose()
    completionProvider.value = null
  }
  const validLang = getLanguage(lang)
  if (validLang === 'python' || validLang === 'java') {
    completionProvider.value = createCompletionProvider(validLang)
  }
}

// ── 初始化编辑器 ──
onMounted(() => {
  if (!editorContainer.value) return

  editor.value = monaco.editor.create(editorContainer.value, {
    value: props.modelValue,
    language: getLanguage(props.language),
    theme: props.theme,
    automaticLayout: true,
    readOnly: props.readOnly,
    minimap: { enabled: false },
    fontSize: 14,
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'Consolas', monospace",
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    wordWrap: 'off',
    tabSize: 4,
    renderWhitespace: 'selection',
    bracketPairColorization: { enabled: true },
    autoIndent: 'full',
    suggest: {
      showWords: true,
      showSnippets: true,
      snippetsPreventQuickSuggestions: false,
    },
    padding: { top: 12 },
  })

  // 注册代码片段补全
  updateCompletionProvider(getLanguage(props.language))

  // 内容变化 → v-model
  editor.value.onDidChangeModelContent(() => {
    const val = editor.value.getValue()
    emit('update:modelValue', val)
  })

  // 通知父组件编辑器已挂载
  emit('mount', editor.value)
})

// ── 监听语言切换 ──
watch(() => props.language, (newLang) => {
  if (editor.value) {
    const model = editor.value.getModel()
    const validLang = getLanguage(newLang)
    monaco.editor.setModelLanguage(model, validLang)
    updateCompletionProvider(validLang)
  }
})

// ── 监听外部值变化 ──
watch(() => props.modelValue, (newVal) => {
  if (editor.value) {
    const current = editor.value.getValue()
    if (newVal !== current) {
      editor.value.setValue(newVal)
    }
  }
})

// ── 监听主题 ──
watch(() => props.theme, (newTheme) => {
  if (editor.value) {
    monaco.editor.setTheme(newTheme)
  }
})

// ── 销毁 ──
onBeforeUnmount(() => {
  if (completionProvider.value) {
    completionProvider.value.dispose()
  }
  if (editor.value) {
    editor.value.dispose()
  }
})

// ── 暴露方法给父组件 ──
defineExpose({
  getEditor: () => editor.value,
  focus: () => editor.value?.focus(),
  getValue: () => editor.value?.getValue(),
  setValue: (v) => editor.value?.setValue(v),
})
</script>

<style scoped>
.monaco-editor-container {
  width: 100%;
  min-height: 300px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}
</style>
