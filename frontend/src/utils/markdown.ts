import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

/**
 * 全站唯一的 Markdown 渲染出口（铁律：任何用户内容的 Markdown 渲染必须走本函数）。
 * 双重防线：markdown-it html:false 转义内联 HTML；DOMPurify 白名单消毒输出。
 * 页面组件禁止自行配置渲染或绕过本函数。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

export function renderMarkdown(source: string): string {
  return DOMPurify.sanitize(md.render(source))
}
