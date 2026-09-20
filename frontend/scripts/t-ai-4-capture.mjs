import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-ai-4')
mkdirSync(outDir, { recursive: true })

const sseOk = [
  'event: token',
  'data: {"delta":"连续失败 **5 次**后，第 6 次返回 429。"}',
  '',
  'event: sources',
  'data: {"sources":[{"title":"登录频控说明","url":"/forum","content_id":"posts/1004","score":0.91}]}',
  '',
  'event: done',
  'data: {"branch":"ok","usage":{"prompt_tokens":12,"completion_tokens":18,"total_tokens":30}}',
  '',
  ''
].join('\n')

function json(status, message, code) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify({ code, message, data: null })
  }
}

async function arm(page, mode) {
  await page.route('**/api/v1/ai/ask', async (route) => {
    if (mode === 'ok') {
      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream; charset=utf-8' },
        body: sseOk
      })
      return
    }
    if (mode === 'down') {
      await route.fulfill(json(503, 'AI 助手暂时不可用，请稍后再试', 50300))
      return
    }
    await route.fulfill(json(429, '提问过于频繁，请稍后再试', 42900))
  })
  await page.route('**/api/v1/**', async (route) => {
    const url = route.request().url()
    if (url.includes('/api/v1/ai/ask')) {
      await route.fallback()
      return
    }
    if (url.includes('/api/v1/users/me') || url.includes('/api/v1/auth/')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            id: 1,
            username: 'shot',
            email: 's@t.local',
            role: 'USER',
            accessToken: 't-ai-4-shot',
            refreshToken: 't-ai-4-shot-refresh'
          }
        })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: { records: [], total: 0, current: 1, size: 10 } })
    })
  })
}

async function login(page) {
  await page.addInitScript(() => {
    localStorage.setItem('community.token', 't-ai-4-shot')
    localStorage.setItem('community.refreshToken', 't-ai-4-shot-refresh')
    localStorage.removeItem('geekstack.ai.chat.v1')
  })
}

const browser = await chromium.launch()
const viewports = [
  { name: 'desktop-1920', width: 1920, height: 1080 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'mobile-375', width: 375, height: 812 }
]

for (const vp of viewports) {
  const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
  await login(page)
  await arm(page, 'ok')
  await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ai-fab', { timeout: 20000 })
  await page.click('.ai-fab')
  await page.fill('.ai-input', '登录失败多少次会锁？')
  await page.locator('.ai-send').click({ force: true })
  await page.waitForSelector('.ai-source', { timeout: 8000 })
  await page.screenshot({ path: join(outDir, `01-ask-${vp.name}.png`) })
  if (vp.name === 'desktop-1920') {
    await page.click('.ai-source')
    await page.waitForURL('**/forum**', { timeout: 8000 })
    await page.screenshot({ path: join(outDir, '02-source-click.png') })
  }
  await page.close()
}

const down = await browser.newPage({ viewport: { width: 1280, height: 800 } })
await login(down)
await arm(down, 'down')
await down.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
await down.waitForSelector('.ai-fab')
await down.click('.ai-fab')
await down.fill('.ai-input', '版规有哪些？')
await down.locator('.ai-send').click({ force: true })
await down.waitForSelector('.ai-state', { timeout: 8000 })
await down.screenshot({ path: join(outDir, '03-503.png') })
await down.close()

const limit = await browser.newPage({ viewport: { width: 1280, height: 800 } })
await login(limit)
await arm(limit, 'limit')
await limit.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
await limit.waitForSelector('.ai-fab')
await limit.click('.ai-fab')
await limit.fill('.ai-input', '再问一次')
await limit.locator('.ai-send').click({ force: true })
await limit.waitForSelector('.ai-state', { timeout: 8000 })
await limit.screenshot({ path: join(outDir, '04-429.png') })
await limit.close()

const loginPage = await browser.newPage({ viewport: { width: 1280, height: 800 } })
await loginPage.goto('http://localhost:5173/login', { waitUntil: 'domcontentloaded' })
await loginPage.waitForTimeout(800)
const fab = await loginPage.locator('.ai-fab').count()
writeFileSync(join(outDir, 'login-widget.json'), JSON.stringify({ fabCount: fab }, null, 2))
await loginPage.screenshot({ path: join(outDir, '05-login-no-widget.png') })
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-AI-4 验收证据',
    '',
    '## 真实问答（路由 mock SSE，事件与网关一致）',
    '- `01-ask-desktop-1920.png` / `tablet-768` / `mobile-375`',
    '- `02-source-click.png`（来源卡片跳转）',
    '',
    '## 降级',
    '- `03-503.png` n-empty：AI 助手暂时不可用',
    '- `04-429.png` n-empty：提问过于频繁',
    '',
    '## 登录页',
    '- `05-login-no-widget.png` + `login-widget.json`（fabCount=0）',
    ''
  ].join('\n')
)
await browser.close()
if (fab !== 0) {
  console.error('login page still has widget', fab)
  process.exit(1)
}
console.log('t-ai-4 screenshots done')
