/**
 * T-SKILL-3-FE：纯前端解析 SKILL.md YAML frontmatter（name / description）。
 * 零依赖；解析失败返回 null，由调用方静默降级为手填。
 */

/** SemVer 核心（与后端 SemVer 对齐：MAJOR.MINOR.PATCH，无预发布/构建元数据） */
export const SEMVER_CORE = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/

export interface SkillMdMeta {
  name?: string
  description?: string
}

function stripQuotes(raw: string): string {
  const s = raw.trim()
  if (
    (s.startsWith('"') && s.endsWith('"')) ||
    (s.startsWith("'") && s.endsWith("'"))
  ) {
    return s.slice(1, -1).trim()
  }
  return s
}

/**
 * 从粘贴文本提取 frontmatter 中的 name / description。
 * 仅支持单行 `key: value`（含引号）；复杂 YAML（|、>、嵌套）→ null。
 */
export function parseSkillMdFrontmatter(text: string): SkillMdMeta | null {
  if (typeof text !== 'string' || !text.trim()) return null
  const normalized = text.replace(/^\uFEFF/, '')
  const match = normalized.match(/^---[ \t]*\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)
  if (!match) return null

  const block = match[1]
  const meta: SkillMdMeta = {}
  for (const line of block.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const kv = trimmed.match(/^(name|description)\s*:\s*(.*)$/i)
    if (!kv) continue
    const key = kv[1].toLowerCase() as 'name' | 'description'
    const value = stripQuotes(kv[2] ?? '')
    if (!value || value === '|' || value === '>') continue
    meta[key] = value
  }

  if (!meta.name && !meta.description) return null
  return meta
}

/** kebab-case name → 显示名称（仅替换连字符，不改大小写） */
export function skillNameToTitle(name: string): string {
  return name.trim().replace(/-/g, ' ')
}

/**
 * 若粘贴内容可解析为 SKILL.md frontmatter，返回预填字段；否则 null。
 */
export function extractFromSkillMdPaste(text: string): { title?: string; summary?: string } | null {
  const meta = parseSkillMdFrontmatter(text)
  if (!meta) return null
  const out: { title?: string; summary?: string } = {}
  if (meta.name) out.title = skillNameToTitle(meta.name)
  if (meta.description) out.summary = meta.description
  if (!out.title && !out.summary) return null
  return out
}
