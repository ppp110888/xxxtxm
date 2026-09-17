import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '@/utils/markdown'

describe('renderMarkdown', () => {
  it('returns empty string for null/undefined input', () => {
    expect(renderMarkdown(null)).toBe('')
    expect(renderMarkdown(undefined)).toBe('')
    expect(renderMarkdown('')).toBe('')
  })

  it('wraps bold text in <strong> tags', () => {
    const result = renderMarkdown('**hello**')
    expect(result).toContain('<strong>hello</strong>')
  })

  it('renders code blocks with <pre><code>', () => {
    const result = renderMarkdown('```js\nconst x = 1;\n```')
    expect(result).toContain('<pre>')
    expect(result).toContain('<code')
    expect(result).toContain('class="language-js"')
  })

  it('renders inline code', () => {
    const result = renderMarkdown('use `println()`')
    expect(result).toContain('<code>println()</code>')
  })

  it('renders unordered lists', () => {
    const result = renderMarkdown('- item 1\n- item 2')
    expect(result).toContain('<ul>')
    expect(result).toContain('<li>item 1</li>')
    expect(result).toContain('<li>item 2</li>')
  })

  it('renders headings', () => {
    const result = renderMarkdown('# Title')
    expect(result).toContain('<h1')
    expect(result).toContain('Title')
  })

  it('escapes HTML in code blocks', () => {
    const result = renderMarkdown('```html\n<div>hello</div>\n```')
    expect(result).toContain('&lt;div&gt;')
    expect(result).toContain('&lt;/div&gt;')
  })

  it('renders links', () => {
    const result = renderMarkdown('[click here](https://example.com)')
    expect(result).toContain('<a href="https://example.com"')
    expect(result).toContain('click here')
  })
})
