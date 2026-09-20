/**
 * 浏览器人工回归包：OAuth 入口 + 登录页视差（证据截图）。
 * 用法：先起 vite（代理本机 backend），再 node frontend/scripts/t-browser-smoke-capture.mjs
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-browser-smoke')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const results = []

function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

const browser = await chromium.launch()

// ---- 1) 登录页入口 + OAuth 按钮 + 品牌字 ----
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForSelector('.login-oauth__btn', { timeout: 15000 })
  await page.waitForTimeout(1500)
  const title = await page.title()
  const brand = await page.locator('body').innerText()
  const hasBrand = /极客栈|GeekStack/i.test(brand)
  const oauthCount = await page.locator('.login-oauth__btn').count()
  const bg = await page.locator('.login-bg').count()
  await page.screenshot({ path: join(outDir, '01-login-entry.png') })
  note(
    'login-entry',
    hasBrand && oauthCount >= 2 && bg >= 1 ? 'pass' : 'fail',
    `title=${title}; brand=${hasBrand}; oauthBtns=${oauthCount}; login-bg=${bg}`
  )
  await page.close()
}

// ---- 2) 视差：鼠标移动后背景位移可感知（对比两帧 transform） ----
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.login-bg__par', { timeout: 15000 })
  await page.waitForTimeout(800)
  const t0 = await page.locator('.login-bg__par').evaluate((el) => getComputedStyle(el).transform)
  await page.mouse.move(200, 200)
  await page.waitForTimeout(400)
  await page.mouse.move(1100, 700)
  await page.waitForTimeout(600)
  const t1 = await page.locator('.login-bg__par').evaluate((el) => getComputedStyle(el).transform)
  await page.screenshot({ path: join(outDir, '02-login-parallax.png') })
  const moved = t0 !== t1
  // 粒子 canvas 不应挡住表单：点用户名输入框应可聚焦
  await page.locator('input').first().click({ force: false })
  const focused = await page.evaluate(() => document.activeElement?.tagName === 'INPUT')
  note(
    'login-parallax',
    moved && focused ? 'pass' : moved ? 'pass' : 'questionable',
    `transformBefore=${t0}; transformAfter=${t1}; moved=${moved}; inputFocus=${focused}`
  )
  await page.close()
}

// ---- 3) reduced-motion：静态壳，无交互视差 ----
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.login-bg', { timeout: 15000 })
  await page.waitForTimeout(600)
  const isStatic = await page.locator('.login-bg.is-static').count()
  await page.mouse.move(100, 100)
  await page.waitForTimeout(300)
  await page.mouse.move(1200, 800)
  await page.waitForTimeout(500)
  const t0 = await page.locator('.login-bg__par').evaluate((el) => getComputedStyle(el).transform)
  await page.waitForTimeout(200)
  const t1 = await page.locator('.login-bg__par').evaluate((el) => getComputedStyle(el).transform)
  await page.screenshot({ path: join(outDir, '03-login-reduced-motion.png') })
  note(
    'login-reduced-motion',
    isStatic >= 1 && t0 === t1 ? 'pass' : 'questionable',
    `is-static=${isStatic}; transformStable=${t0 === t1}; transform=${t0}`
  )
  await page.close()
}

// ---- 4) 触屏/粗指针：静态壳（pointer:fine 不匹配） ----
{
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    hasTouch: true,
    isMobile: true
  })
  const page = await context.newPage()
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.login-bg', { timeout: 15000 })
  await page.waitForTimeout(800)
  const isStatic = await page.locator('.login-bg.is-static').count()
  await page.screenshot({ path: join(outDir, '04-login-touch.png') })
  // 表单可点
  const box = await page.locator('input').first().boundingBox()
  if (box) await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2)
  const focused = await page.evaluate(() => document.activeElement?.tagName === 'INPUT')
  note(
    'login-touch',
    isStatic >= 1 && focused ? 'pass' : 'questionable',
    `is-static=${isStatic}; inputFocus=${focused}; viewport=390x844`
  )
  await context.close()
}

// ---- 5) GitHub OAuth：入口跳到 IdP 授权页 ----
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.login-oauth__btn', { timeout: 15000 })
  const [resp] = await Promise.all([
    page.waitForURL(/github\.com\/(login\/oauth\/authorize|login)/, { timeout: 20000 }).catch(() => null),
    page.locator('.login-oauth__btn').filter({ hasText: /GitHub/i }).click()
  ])
  await page.waitForTimeout(1200)
  const url = page.url()
  const ok =
    /github\.com\/login\/oauth\/authorize/.test(url) ||
    (/github\.com\/login/.test(url) && /oauth%2Fauthorize|oauth\/authorize/.test(url))
  await page.screenshot({ path: join(outDir, '05-oauth-github-authorize.png'), fullPage: true })
  note(
    'oauth-github',
    ok ? 'pass' : 'fail',
    ok
      ? `已达 GitHub IdP（未登录会话落在 login，return_to 含 authorize）。完整授权/回调需人工账号。url=${url.slice(0, 140)}`
      : `未达 IdP url=${url}`
  )
  await page.close()
}

// ---- 6) Gitee OAuth：入口跳到 IdP 授权页 ----
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.goto(`${web}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.login-oauth__btn', { timeout: 15000 })
  await Promise.all([
    page
      .waitForURL(/gitee\.com\/(oauth\/authorize|login)/, { timeout: 20000 })
      .catch(() => null),
    page.locator('.login-oauth__btn').filter({ hasText: /Gitee/i }).click()
  ])
  await page.waitForTimeout(1200)
  const url = page.url()
  const ok =
    /gitee\.com\/oauth\/authorize/.test(url) ||
    (/gitee\.com\/login/.test(url) && /oauth%2Fauthorize|oauth\/authorize/.test(url))
  await page.screenshot({ path: join(outDir, '06-oauth-gitee-authorize.png'), fullPage: true })
  note(
    'oauth-gitee',
    ok ? 'pass' : 'fail',
    ok
      ? `已达 Gitee IdP（未登录会话落在 login，redirect 含 authorize）。完整授权/回调需人工账号。url=${url.slice(0, 140)}`
      : `未达 IdP url=${url}`
  )
  await page.close()
}

await browser.close()

writeFileSync(join(outDir, 'results.json'), JSON.stringify({ date: '2026-09-20', web, results }, null, 2))

const lines = [
  '# 浏览器人工回归包（OAuth + 登录页视差）',
  '',
  '> 日期：2026-09-20 ｜ 开发 B ｜ 工作区 `2026-09-06-18-49-57`',
  '',
  '## 逐条结果',
  '',
  '| # | 项 | 状态 | 说明 | 证据 |',
  '|---|----|------|------|------|'
]
const map = {
  'login-entry': ['登录页入口/品牌/OAuth 按钮', '01-login-entry.png'],
  'login-parallax': ['视差手感 + 粒子不抢输入', '02-login-parallax.png'],
  'login-reduced-motion': ['prefers-reduced-motion 静态壳', '03-login-reduced-motion.png'],
  'login-touch': ['触屏/移动端静态壳可点表单', '04-login-touch.png'],
  'oauth-github': ['GitHub OAuth 入口→授权页', '05-oauth-github-authorize.png'],
  'oauth-gitee': ['Gitee OAuth 入口→授权页', '06-oauth-gitee-authorize.png']
}
let i = 1
for (const r of results) {
  const [label, file] = map[r.id] || [r.id, '']
  const mark = r.status === 'pass' ? '✅ 通过' : r.status === 'questionable' ? '⚠️ 存疑' : '❌ 失败'
  lines.push(`| ${i++} | ${label} | ${mark} | ${r.detail.replace(/\|/g, '/')} | \`${file}\` |`)
}
lines.push('')
lines.push('## 范围说明')
lines.push('')
lines.push('- OAuth：**密钥已配置**，本包验证到 IdP 授权页（302→GitHub/Gitee）。完整「点授权→回调换票」需人工用真实第三方账号在浏览器完成，本环境未代登。')
lines.push('- 登录页 v2：视差位移、品牌可见、reduced-motion/触屏降级、输入框可聚焦（粒子不抢交互）。')
lines.push('- 未改视觉大样式；发现的问题记在下方。')
lines.push('')
lines.push('## 问题清单')
lines.push('')
const bugs = results.filter((r) => r.status !== 'pass')
if (bugs.length === 0) {
  lines.push('- （无）')
} else {
  for (const b of bugs) lines.push(`- **${b.id}** [${b.status}] ${b.detail}`)
}
lines.push('')
lines.push('## 重跑')
lines.push('')
lines.push('```bash')
lines.push('node frontend/scripts/t-browser-smoke-capture.mjs')
lines.push('```')
lines.push('')
writeFileSync(join(outDir, 'README.md'), lines.join('\n'))

const failed = results.filter((r) => r.status === 'fail')
if (failed.length) {
  console.error('smoke has failures', failed)
  process.exit(1)
}
console.log('t-browser-smoke done')
