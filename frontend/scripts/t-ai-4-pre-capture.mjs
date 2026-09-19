import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-ai-4-pre')
mkdirSync(outDir, { recursive: true })

const browser = await chromium.launch()
const viewports = [
  { name: 'desktop-1920', width: 1920, height: 1080 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'mobile-375', width: 375, height: 812 }
]

async function openHomeLoggedIn(page) {
  await page.addInitScript(() => {
    localStorage.setItem('community.token', 't-ai-4-pre-shot-token')
    localStorage.setItem('community.refreshToken', 't-ai-4-pre-shot-refresh')
  })
  await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForSelector('.ai-fab', { timeout: 15000 })
}

for (const vp of viewports) {
  const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
  await openHomeLoggedIn(page)
  await page.click('.ai-fab')
  await page.waitForSelector('.ai-panel', { state: 'visible' })
  await page.fill('.ai-input', 'Skill 广场怎么用？')
  await page.locator('.ai-send').click({ force: true })
  await page.waitForTimeout(1200)
  await page.screenshot({ path: join(outDir, `01-open-${vp.name}.png`), fullPage: false })
  await page.close()
}

// 未登录提示
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
await page.waitForSelector('.ai-fab')
await page.click('.ai-fab')
await page.waitForSelector('.ai-login-hint')
await page.screenshot({ path: join(outDir, '02-login-hint.png'), fullPage: false })

writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-AI-4-PRE 验收证据',
    '',
    '## 三断点（已登录 + 面板展开 + mock 流）',
    '- `01-open-desktop-1920.png`',
    '- `01-open-tablet-768.png`',
    '- `01-open-mobile-375.png`',
    '',
    '## 未登录',
    '- `02-login-hint.png`（提示去登录）',
    '',
    '色值均走 token；琥珀仅发送按钮（GsButton primary）。',
    ''
  ].join('\n')
)

await browser.close()
console.log('t-ai-4-pre screenshots done')
