/**
 * T-FE-TAG-DETAIL-TABS 证据：同标签下切帖子/项目 Tab
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-tag-detail-tabs')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const tag = 'Java'
const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })

await page.route('**/api/v1/posts**', (route) => {
  const url = new URL(route.request().url())
  if (url.pathname.match(/\/posts\/\d+/)) return route.continue()
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: {
        total: 2,
        current: 1,
        size: 9,
        records: [
          {
            id: 11,
            userId: 1,
            categoryId: 1,
            title: 'Java 异常排查笔记',
            tags: [tag],
            viewCount: 30,
            replyCount: 2,
            lastReplyAt: null,
            isPinned: 0,
            isHot: 0,
            createdAt: new Date().toISOString()
          },
          {
            id: 12,
            userId: 1,
            categoryId: 1,
            title: 'Spring Boot 3 升级清单',
            tags: [tag],
            viewCount: 18,
            replyCount: 0,
            lastReplyAt: null,
            isPinned: 0,
            isHot: 0,
            createdAt: new Date().toISOString()
          }
        ]
      }
    })
  })
})

await page.route('**/api/v1/shares**', (route) => {
  const url = new URL(route.request().url())
  if (url.pathname.match(/\/shares\/\d+/)) return route.continue()
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: {
        total: 1,
        current: 1,
        size: 9,
        records: [
          {
            id: 21,
            title: 'Java RAG Starter',
            summary: '极客栈示例项目',
            tags: [tag],
            repoUrl: null,
            viewCount: 9,
            createdAt: new Date().toISOString()
          }
        ]
      }
    })
  })
})

await page.route('**/api/v1/skills**', (route) => {
  const url = new URL(route.request().url())
  if (url.pathname.match(/\/skills\/\d+/)) return route.continue()
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: { total: 0, current: 1, size: 9, records: [] }
    })
  })
})

await page.goto(`${web}/tags/${encodeURIComponent(tag)}`, {
  waitUntil: 'networkidle',
  timeout: 30000
})
await page.waitForSelector('[role="tablist"]', { timeout: 15000 })
await page.waitForTimeout(500)

const shareCount = await page.getByText('共 1 个项目').count()
const shareCard = await page.getByText('Java RAG Starter').count()
await page.screenshot({ path: join(outDir, '01-tab-share.png') })
note('tab-share', shareCount >= 1 && shareCard >= 1 ? 'pass' : 'fail', `count=${shareCount}; card=${shareCard}`)

await page.getByRole('tab', { name: '帖子' }).click()
await page.waitForTimeout(600)
await page.waitForURL(/type=post/, { timeout: 10000 })
const postCount = await page.getByText('共 2 篇帖子').count()
const postRow = await page.getByText('Java 异常排查笔记').count()
await page.screenshot({ path: join(outDir, '02-tab-post.png') })
note('tab-post', postCount >= 1 && postRow >= 1 ? 'pass' : 'fail', `count=${postCount}; row=${postRow}; url=${page.url()}`)

await page.getByRole('tab', { name: 'Skill' }).click()
await page.waitForTimeout(600)
const skillEmpty = await page.getByText('该标签下暂无 Skill').count()
const skillCount = await page.getByText('共 0 个 Skill').count()
await page.screenshot({ path: join(outDir, '03-tab-skill-empty.png') })
note(
  'tab-skill-empty',
  skillEmpty >= 1 && skillCount >= 1 ? 'pass' : 'fail',
  `empty=${skillEmpty}; count=${skillCount}`
)

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(join(outDir, 'result.json'), JSON.stringify({ tag, results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-TAG-DETAIL-TABS 证据',
    '',
    `| 场景 | 截图 | 结果 |`,
    `|---|---|---|`,
    `| 项目 Tab | 01-tab-share.png | ${results.find((r) => r.id === 'tab-share')?.status} |`,
    `| 帖子 Tab | 02-tab-post.png | ${results.find((r) => r.id === 'tab-post')?.status} |`,
    `| Skill 空态 | 03-tab-skill-empty.png | ${results.find((r) => r.id === 'tab-skill-empty')?.status} |`,
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
