/**
 * T-FE-SEARCH-ASSISTANT-COPY 证据：搜索页 + 助手浮层文案对照
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-search-assistant-copy')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

const browser = await chromium.launch()

{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route('**/api/v1/**', async (route) => {
    const url = route.request().url()
    if (url.includes('/search')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: { total: 0, current: 1, size: 10, records: [] }
        })
      })
      return
    }
    if (url.includes('/forum/categories')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: [] })
      })
      return
    }
    await route.continue()
  })
  await page.goto(`${web}/search`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.search-page', { timeout: 15000 })
  await page.waitForTimeout(400)
  const title = await page.getByText('找内容', { exact: true }).count()
  const hint = await page.getByText('按关键字找帖子、项目与 Skill').count()
  const empty = await page.getByText('输入关键字开始找内容').count()
  const ph = await page.locator('.search-input input').getAttribute('placeholder')
  const topPh = await page.locator('.gs-search input[type="search"]').count()
  // on /search topbar is icon-only; also check home for topbar placeholder
  await page.screenshot({ path: join(outDir, '01-search-page.png') })
  note(
    'search-page',
    title >= 1 && hint >= 1 && empty >= 1 && /关键字/.test(ph || '') ? 'pass' : 'fail',
    `title=${title}; hint=${hint}; empty=${empty}; ph=${ph}; topbarInputOnSearch=${topPh}`
  )
  await page.close()
}

{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route('**/api/v1/users/me**', (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          id: 1,
          username: 'copyuser',
          nickname: 'copyuser',
          email: 'c@test.local',
          avatarUrl: null,
          bio: null,
          role: 'USER',
          createdAt: new Date().toISOString(),
          hasPassword: true,
          oauthBindings: { github: false, gitee: false }
        }
      })
    })
  })
  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.setItem('community.token', 't')
    localStorage.setItem('community.refreshToken', 'r')
  })
  await page.goto(`${web}/`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.gs-search input', { timeout: 15000 })
  const topPh = await page.locator('.gs-search input').getAttribute('placeholder')
  await page.locator('.ai-fab').click()
  await page.waitForSelector('.ai-panel', { timeout: 10000 })
  await page.waitForTimeout(500)
  const panelTitle = await page.getByText('社区问答', { exact: true }).count()
  const panelSub = await page.getByText('问社区内容').count()
  const welcome = await page.getByText('不是关键字列表检索').count()
  const inputPh = await page.locator('.ai-input').getAttribute('placeholder')
  await page.screenshot({ path: join(outDir, '02-assistant-panel.png') })
  note(
    'assistant-panel',
    panelTitle >= 1 &&
      panelSub >= 1 &&
      welcome >= 1 &&
      /问题/.test(inputPh || '') &&
      /找帖子/.test(topPh || '')
      ? 'pass'
      : 'fail',
    `title=${panelTitle}; sub=${panelSub}; welcome=${welcome}; inputPh=${inputPh}; topPh=${topPh}`
  )
  await page.close()
}

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(join(outDir, 'result.json'), JSON.stringify({ results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-SEARCH-ASSISTANT-COPY 证据',
    '',
    '口径：顶栏/`/search` = 找内容；浮层助手 = 问答。',
    '',
    '| 场景 | 截图 | 结果 |',
    '|---|---|---|',
    `| 搜索页 | 01-search-page.png | ${results.find((r) => r.id === 'search-page')?.status} |`,
    `| 助手浮层（含顶栏 placeholder） | 02-assistant-panel.png | ${results.find((r) => r.id === 'assistant-panel')?.status} |`,
    '',
    '```json',
    JSON.stringify(results, null, 2),
    '```',
    ''
  ].join('\n')
)

if (failed.length) {
  console.error('FAILED', failed)
  process.exit(1)
}
console.log('all pass →', outDir)
