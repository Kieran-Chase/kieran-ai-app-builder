import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'

/**
 * Markdown 渲染器（带代码语法高亮）
 * 用于渲染 AI 流式返回的内容，让 html / css / js 等代码块以独立的高亮代码框展示
 */
const md = new MarkdownIt({
  // 允许识别 URL 自动转链接
  linkify: true,
  // 保留换行
  breaks: true,
  // 代码块高亮
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        const highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
        // 外层包裹带语言标签的容器，样式在 AppChatPage 中定义
        return `<pre class="hljs-pre"><div class="hljs-lang">${lang}</div><code class="hljs">${highlighted}</code></pre>`
      } catch {
        // 高亮失败则降级为纯文本
      }
    }
    // 未指定语言或不支持时，转义后原样输出
    const escaped = md.utils.escapeHtml(code)
    return `<pre class="hljs-pre"><code class="hljs">${escaped}</code></pre>`
  },
})

/**
 * 将 Markdown 文本渲染为 HTML
 *
 * @param content Markdown 文本
 */
export const renderMarkdown = (content: string): string => {
  if (!content) {
    return ''
  }
  // 先渲染成 HTML，再用 DOMPurify 消毒，防止 XSS（如 <script>、onerror 等）
  const rawHtml = md.render(content)
  return DOMPurify.sanitize(rawHtml)
}
