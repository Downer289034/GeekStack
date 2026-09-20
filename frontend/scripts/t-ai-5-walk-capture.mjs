/**
 * T-AI-5 浮窗全链走查截图（真实网关 / 真实 ai-service，非路由 mock）。
 * 前置：backend :8081、vite :5173、qdrant+ai-service 已起；可 SSH 停启 community-ai-service。
 * 用法：node frontend/scripts/t-ai-5-walk-capture.mjs
 */
import { chromium } from 'playwright'
import { execSync } from 'child_process'
import { mkdirSync, writeFileSync, readFileSync, existsSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-ai-5')
const api = process.env.AI5_API || 'http://127.0.0.1:8081/api'
const web = process.env.AI5_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

function ssh(cmd) {
  return execSync(`ssh -o ConnectTimeout=12 -o BatchMode=yes root@172.25.41.137 ${JSON.stringify(cmd)}`, {
    encoding: 'utf8',
    timeout: 60000
  })
}

async function apiJson(path, opts = {}) {
  const resp = await fetch(`${api}${path}`, {
    ...opts,
    headers: {
      'Content-Type': 'application/json',
      ...(opts.headers || {})
    }
  })
  const body = await resp.json()
  return { status: resp.status, body }
}

async function ensureMeta() {
  const metaPath = join(outDir, '_meta.json')
  if (existsSync(metaPath)) {
    const meta = JSON.parse(readFileSync(metaPath, 'utf8'))
    if (meta.token && meta.marker && meta.postId) return meta
  }
  const marker = `M13WALKFE${Date.now()}`
  const user = `ai5_${Math.random().toString(36).slice(2, 10)}`
  const email = `${user}@test.local`
  const password = 'password123'
  const reg = await apiJson('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username: user, email, password })
  })
  if (reg.body.code !== 0) throw new Error(`register failed: ${JSON.stringify(reg.body)}`)
  const token = reg.body.data.accessToken
  const refresh = reg.body.data.refreshToken
  const cats = await apiJson('/v1/posts/categories')
  const catId = cats.body.data[0].id
  const post = await apiJson('/v1/posts', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      categoryId: catId,
      title: `${marker} 浮窗全链走查专用帖`,
      contentMd: `本文唯一标记词是 **${marker}**。极客栈 AI 助手走查：发帖后应被 RAG 索引。可检索句：${marker} 浮窗全链验证句。`,
      tags: ['ai-walk']
    })
  })
  if (post.body.code !== 0) throw new Error(`post failed: ${JSON.stringify(post.body)}`)
  const postId = post.body.data.id
  await new Promise((r) => setTimeout(r, 8000))
  const meta = { marker, user, email, password, token, refresh, postId }
  writeFileSync(metaPath, JSON.stringify(meta, null, 2))
  return meta
}

async function refreshMeta(meta) {
  const login = await apiJson('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: meta.user, password: meta.password })
  })
  if (login.body.code !== 0) throw new Error(`login failed: ${JSON.stringify(login.body)}`)
  meta.token = login.body.data.accessToken
  meta.refresh = login.body.data.refreshToken
  writeFileSync(join(outDir, '_meta.json'), JSON.stringify(meta, null, 2))
  return meta
}

async function injectAuth(page, meta) {
  await page.addInitScript(
    ({ token, refresh }) => {
      localStorage.setItem('community.token', token)
      localStorage.setItem('community.refreshToken', refresh)
      localStorage.removeItem('geekstack.ai.chat.v1')
    },
    { token: meta.token, refresh: meta.refresh }
  )
}

async function openFab(page) {
  await page.waitForSelector('.ai-fab', { timeout: 20000 })
  await page.click('.ai-fab')
  await page.waitForSelector('.ai-panel', { timeout: 8000 })
}

async function sendAsk(page, text) {
  await page.fill('.ai-input', text)
  await page.locator('.ai-send').click({ force: true })
}

async function burnQuota(token, n) {
  for (let i = 0; i < n; i++) {
    const resp = await fetch(`${api}/v1/ai/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream, application/json'
      },
      body: JSON.stringify({ question: `配额燃烧 ${i + 1}` })
    })
    // 读完或短路均可；429 表示已满
    if (resp.status === 429) {
      console.log(`burn hit 429 at attempt ${i + 1}`)
      return
    }
    if (resp.ok && resp.body) {
      await resp.text()
    } else {
      console.log(`burn ${i + 1} status=${resp.status}`)
      await resp.text().catch(() => '')
    }
  }
}

const evidence = {
  marker: '',
  postId: null,
  paths: {}
}

const meta = await ensureMeta()
await refreshMeta(meta)
evidence.marker = meta.marker
evidence.postId = meta.postId
console.log('meta', meta.marker, 'post', meta.postId)

const browser = await chromium.launch()
const desktop = { width: 1440, height: 900 }
const skipHit = process.env.AI5_SKIP_HIT === '1'

// ---- 1) 发帖标记已在 API 完成；浮窗提问命中来源 ----
if (!skipHit) {
  const page = await browser.newPage({ viewport: desktop })
  await injectAuth(page, meta)
  await page.goto(`${web}/`, { waitUntil: 'domcontentloaded' })
  await openFab(page)
  await sendAsk(page, `帖子 ${meta.marker} 讲了什么？请给来源`)
  await page.waitForSelector('.ai-source', { timeout: 90000 })
  const sourceText = await page.locator('.ai-source').first().innerText()
  const sourceCount = await page.locator('.ai-source').count()
  evidence.paths.hit = { sourceCount, sourceText }
  await page.screenshot({ path: join(outDir, '01-post-ask-hit.png'), fullPage: false })
  // 来源可点
  await page.locator('.ai-source').first().click()
  await page.waitForTimeout(1200)
  evidence.paths.hit.urlAfterClick = page.url()
  await page.screenshot({ path: join(outDir, '02-source-click.png'), fullPage: false })
  await page.close()
  console.log('01/02 hit ok', evidence.paths.hit)
} else {
  evidence.paths.hit = { skipped: true, note: 'reuse existing 01/02 png' }
  console.log('skip hit screenshots')
}

// ---- 2) 未登录引导 ----
if (!skipHit) {
  const page = await browser.newPage({ viewport: desktop })
  await page.addInitScript(() => {
    localStorage.removeItem('community.token')
    localStorage.removeItem('community.refreshToken')
    localStorage.removeItem('geekstack.ai.chat.v1')
  })
  await page.goto(`${web}/forum`, { waitUntil: 'domcontentloaded' })
  await openFab(page)
  await page.waitForSelector('.ai-login-hint', { timeout: 8000 })
  const hint = await page.locator('.ai-login-hint').innerText()
  evidence.paths.unlogged = { hint }
  await page.screenshot({ path: join(outDir, '03-unlogged-hint.png'), fullPage: false })
  await page.close()
  console.log('03 unlogged ok')
} else {
  evidence.paths.unlogged = { skipped: true }
}

// ---- 3) 停 ai-service → 503（必须配额未满；用新账号避免 429 抢先） ----
{
  const sUser = `ai5s_${Math.random().toString(36).slice(2, 10)}`
  const sPass = 'password123'
  const reg = await apiJson('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username: sUser, email: `${sUser}@test.local`, password: sPass })
  })
  if (reg.body.code !== 0) throw new Error(`503-user register failed: ${JSON.stringify(reg.body)}`)
  const sMeta = {
    token: reg.body.data.accessToken,
    refresh: reg.body.data.refreshToken
  }
  console.log('stopping ai-service (fresh user', sUser, ')...')
  try {
    ssh('docker stop community-ai-service')
  } catch (e) {
    console.warn('stop warn', String(e.message || e))
  }
  await new Promise((r) => setTimeout(r, 2500))
  const page = await browser.newPage({ viewport: desktop })
  await injectAuth(page, sMeta)
  await page.goto(`${web}/`, { waitUntil: 'domcontentloaded' })
  await openFab(page)
  await sendAsk(page, '版规有哪些？')
  await page.waitForSelector('.ai-state', { timeout: 45000 })
  const downText = await page.locator('.ai-state').innerText()
  evidence.paths.down503 = { downText, user: sUser }
  await page.screenshot({ path: join(outDir, '04-503.png'), fullPage: false })
  await page.close()
  console.log('04 503 ok', downText)
  console.log('starting ai-service...')
  try {
    ssh('docker start community-ai-service')
  } catch (e) {
    console.warn('start warn', String(e.message || e))
  }
  // 等健康
  for (let i = 0; i < 30; i++) {
    try {
      const h = await fetch('http://172.25.41.137:8090/health', { signal: AbortSignal.timeout(2000) })
      if (h.ok) break
    } catch {
      /* retry */
    }
    await new Promise((r) => setTimeout(r, 2000))
  }
  console.log('ai-service back')
}

// ---- 4) 超限 429 ----
{
  await refreshMeta(meta)
  // 烧满配额后 UI 再问一次
  console.log('burning quota...')
  await burnQuota(meta.token, 15)
  const page = await browser.newPage({ viewport: desktop })
  await injectAuth(page, meta)
  await page.goto(`${web}/`, { waitUntil: 'domcontentloaded' })
  await openFab(page)
  await sendAsk(page, '再问一次触发限流')
  await page.waitForSelector('.ai-state', { timeout: 20000 })
  const limitText = await page.locator('.ai-state').innerText()
  evidence.paths.limit429 = { limitText }
  await page.screenshot({ path: join(outDir, '05-429.png'), fullPage: false })
  await page.close()
  console.log('05 429 ok', limitText)
}

await browser.close()

writeFileSync(join(outDir, 'walkthrough.json'), JSON.stringify(evidence, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-AI-5 浮窗全链走查证据',
    '',
    `> 日期：2026-09-20 ｜ 标记词：\`${evidence.marker}\` ｜ 帖子 id：\`${evidence.postId}\``,
    '',
    '## 五条路径',
    '',
    '| # | 路径 | 证据 |',
    '|---|------|------|',
    '| 1 | 发帖（唯一标记）→ 浮窗提问命中来源 | `01-post-ask-hit.png` + `_meta.json` + `_ask_raw.txt` |',
    '| 2 | 来源卡片可点 | `02-source-click.png` |',
    '| 3 | 未登录引导 | `03-unlogged-hint.png` |',
    '| 4 | 停 ai-service → 503 | `04-503.png` |',
    '| 5 | 超限 → 429 | `05-429.png` |',
    '',
    '## 说明',
    '',
    '- 真实网关 `POST /api/v1/ai/ask`（本机 backend :8081 → 开发栈 ai-service :8090 / Qdrant :6333）。',
    '- **非** T-AI-4 路由 mock。',
    '- 重跑：`node frontend/scripts/t-ai-5-walk-capture.mjs`（需 vite + backend + SSH）。',
    ''
  ].join('\n')
)

const bad = []
if (!skipHit && !evidence.paths.hit?.sourceCount) bad.push('hit sources missing')
if (!skipHit && !String(evidence.paths.unlogged?.hint || '').includes('登录')) bad.push('unlogged hint')
if (!String(evidence.paths.down503?.downText || '').includes('不可用')) bad.push('503 copy')
if (!String(evidence.paths.limit429?.limitText || '').includes('频繁')) bad.push('429 copy')
if (bad.length) {
  console.error('evidence checks failed', bad)
  process.exit(1)
}
console.log('t-ai-5 walk capture done')
