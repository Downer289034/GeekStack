/**
 * T-FE-TAG-CLOUD-COPY 证据：/tags 页头 hint 含帖口径
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-tag-cloud-copy')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })

await page.route('**/api/v1/tags**', (route) =>
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: [
        { name: 'Java', count: 12 },
        { name: 'RAG', count: 8 },
        { name: 'Vue', count: 5 }
      ]
    })
  })
)

await page.goto(`${web}/tags`, { waitUntil: 'networkidle', timeout: 30000 })
await page.waitForSelector('.tags-hint', { timeout: 15000 })
await page.waitForTimeout(400)

const body = await page.locator('.tags-page').innerText()
const hasHint = /可见帖子.*项目.*Skill|帖子、已发布项目与 Skill/.test(body)
const noStale = !/share-only|帖子标签待后端/.test(body)
const titleOk = (await page.getByText('标签云', { exact: true }).count()) >= 1

await page.screenshot({ path: join(outDir, '01-tags-hint.png') })
await browser.close()

const ok = hasHint && noStale && titleOk
const result = { ok, hasHint, noStale, titleOk, snippet: body.slice(0, 200) }
writeFileSync(join(outDir, 'result.json'), JSON.stringify(result, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-TAG-CLOUD-COPY 证据',
    '',
    '- `01-tags-hint.png`：页头 hint「计数含可见帖子、已发布项目与 Skill」',
    '',
    '```json',
    JSON.stringify(result, null, 2),
    '```',
    ''
  ].join('\n')
)

console.log(JSON.stringify(result, null, 2))
if (!ok) process.exit(1)
console.log('pass →', outDir)
