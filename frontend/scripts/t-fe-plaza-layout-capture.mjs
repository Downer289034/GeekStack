/**
 * T-FE-PLAZA-LAYOUT 证据：宽屏 / 中屏 / 窄屏项目广场。
 * 用法：vite:5173 就绪后 node frontend/scripts/t-fe-plaza-layout-capture.mjs
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-plaza-layout')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const records = Array.from({ length: 6 }, (_, i) => ({
  id: i + 1,
  title: ['LangChain Agents', 'Qdrant RAG Kit', 'vLLM Serving', 'CrewAI Demo', 'LlamaIndex Flow', 'OpenWebUI Mod'][i],
  summary:
    i % 2 === 0
      ? '面向开源 AI 的可复用脚手架，含向量检索、工具调用与部署示例。'
      : '轻量 Agent 编排与评测脚本，适合本地联调。',
  tags: i % 2 === 0 ? ['AI', 'RAG'] : ['Agent'],
  repoUrl: null,
  viewCount: 120 + i * 17,
  createdAt: new Date(Date.now() - i * 86400000).toISOString()
}))

const browser = await chromium.launch()

async function shot(name, width, height) {
  const page = await browser.newPage({ viewport: { width, height } })
  await page.route('**/api/v1/shares**', async (route) => {
    const url = new URL(route.request().url())
    if (url.pathname.match(/\/shares\/\d+/)) {
      await route.continue()
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: { total: records.length, current: 1, size: 9, records }
      })
    })
  })
  await page.goto(`${web}/shares`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.card-grid .gs-project-card', { timeout: 15000 })
  await page.waitForTimeout(500)

  const main = page.locator('.gs-shell-main')
  const box = await main.boundingBox()
  const hasWide = await main.evaluate((el) => el.classList.contains('gs-shell-main--wide'))
  const cardCount = await page.locator('.card-grid .gs-project-card').count()
  const summaryCount = await page.locator('.gs-project-card__summary').count()
  const navWrap = await page.locator('.gs-nav__links').evaluate((el) => getComputedStyle(el).flexWrap)
  const sideGap = box ? Math.max(0, (width - box.width) / 2) : null

  await page.screenshot({ path: join(outDir, name), fullPage: false })
  await page.close()
  return { name, width, hasWide, cardCount, summaryCount, navWrap, mainWidth: box?.width, sideGap }
}

const results = []
results.push(await shot('01-wide-1440.png', 1440, 900))
results.push(await shot('02-mid-1180.png', 1180, 900))
results.push(await shot('03-tablet-768.png', 768, 900))
results.push(await shot('04-mobile-390.png', 390, 844))

await browser.close()

const wide = results[0]
const ok =
  wide.hasWide &&
  wide.cardCount >= 3 &&
  wide.summaryCount >= 1 &&
  wide.navWrap === 'nowrap' &&
  (wide.sideGap == null || wide.sideGap < 80)

writeFileSync(join(outDir, 'result.json'), JSON.stringify({ ok, results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-PLAZA-LAYOUT 证据',
    '',
    '| 视口 | 截图 |',
    '|---|---|',
    '| 1440 | 01-wide-1440.png |',
    '| 1180 | 02-mid-1180.png |',
    '| 768 | 03-tablet-768.png |',
    '| 390 | 04-mobile-390.png |',
    '',
    '```json',
    JSON.stringify({ ok, results }, null, 2),
    '```',
    ''
  ].join('\n')
)

console.log(JSON.stringify({ ok, results }, null, 2))
if (!ok) process.exit(1)
console.log('all pass →', outDir)
