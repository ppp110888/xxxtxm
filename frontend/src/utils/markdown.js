import { marked } from 'marked'

// 配置 marked 渲染器
const renderer = new marked.Renderer()

// 代码块 — 添加语言类名和 pre/code 包装
renderer.code = function ({ text, lang }) {
  const langClass = lang ? ` class="language-${lang}"` : ''
  const escaped = escapeHtml(text)
  return `<pre><code${langClass}>${escaped}</code></pre>`
}

// 行内代码
renderer.codespan = function ({ text }) {
  return `<code>${escapeHtml(text)}</code>`
}

// 表格 — 用 div 包裹方便横向滚动
renderer.table = function ({ header, rows }) {
  const thead = `<thead><tr>${header.map(h => `<th>${h.text}</th>`).join('')}</tr></thead>`
  const tbody = `<tbody>${rows.map(row =>
    `<tr>${row.map(c => `<td>${c.text}</td>`).join('')}</tr>`
  ).join('')}</tbody>`
  return `<div class="table-wrapper"><table>${thead}${tbody}</table></div>`
}

// 图片 — 添加 loading lazy
renderer.image = function ({ href, title, text }) {
  const titleAttr = title ? ` title="${title}"` : ''
  return `<img src="${href}" alt="${text}"${titleAttr} loading="lazy" />`
}

marked.setOptions({
  renderer,
  breaks: true,        // 换行转 <br>
  gfm: true,           // GitHub Flavored Markdown (表格、任务列表等)
})

/**
 * 渲染 Markdown 为 HTML（安全转义后输出）
 */
export function renderMarkdown(md) {
  if (!md) return ''
  return marked.parse(md)
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
