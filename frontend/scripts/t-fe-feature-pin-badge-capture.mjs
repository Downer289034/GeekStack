/**
 * T-FE-FEATURE-PIN-BADGE 证据：列表/详情展示置顶与精华
 * 使用 mock postId=9001（置顶+精华）与 9002（仅精华）
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-feature-pin-badge')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const POST_PIN_FEATURED = 9001
const POST_FEATURED = 9002

function postCard(id, { pinned = 0, featured = 0, title }) {
  return {
    id,
    userId: 1,
    categoryId: 1,
    title,
    tags: ['Java'],
    viewCount: 42,
    replyCount: 3,
    lastReplyAt: null,
    isPinned: pinned,
    isFeatured: featured,
    isHot: 0,
    createdAt: new Date().toISOString()
  }
}

function postDetail(id, opts) {
  return {
    ...postCard(id, opts),
    contentMd: `证据帖 #${id}：isPinned=${opts.pinned} isFeatured=${opts.featured}`
  }
}

const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })

await page.route(/\/api\/v1\/posts/, async (route) => {
  const url = new URL(route.request().url())
  if (url.pathname.includes('/categories')) {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: [{ id: 1, name: '讨论', slug: 'discuss', sortOrder: 1 }]
      })
    })
    return
  }
  if (url.pathname.includes('/comments')) {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    })
    return
  }
  const m = url.pathname.match(/\/posts\/(\d+)$/)
  if (m) {
    const id = Number(m[1])
    const body =
      id === POST_PIN_FEATURED
        ? postDetail(id, { pinned: 1, featured: 1, title: '置顶且精华示例帖' })
        : id === POST_FEATURED
          ? postDetail(id, { pinned: 0, featured: 1, title: '仅精华示例帖' })
          : postDetail(id, { pinned: 0, featured: 0, title: '普通帖' })
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: body })
    })
    return
  }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: {
        total: 2,
        current: 1,
        size: 20,
        records: [
          postCard(POST_PIN_FEATURED, {
            pinned: 1,
            featured: 1,
            title: '置顶且精华示例帖'
          }),
          postCard(POST_FEATURED, { pinned: 0, featured: 1, title: '仅精华示例帖' })
        ]
      }
    })
  })
})

await page.goto(`${web}/forum`, { waitUntil: 'networkidle', timeout: 30000 })
await page.waitForSelector('.gs-feed-row', { timeout: 15000 })
await page.waitForTimeout(400)
const listPinned = await page.getByText('置顶', { exact: true }).count()
const listFeatured = await page.getByText('精华', { exact: true }).count()
await page.screenshot({ path: join(outDir, '01-forum-list-badges.png') })
note(
  'forum-list',
  listPinned >= 1 && listFeatured >= 2 ? 'pass' : 'fail',
  `pinned=${listPinned}; featured=${listFeatured}; postIds=${POST_PIN_FEATURED},${POST_FEATURED}`
)
await page.close()

const detailPage = await browser.newPage({ viewport: { width: 1280, height: 900 } })
await detailPage.route(/\/api\/v1\/posts/, async (route) => {
  const url = new URL(route.request().url())
  if (url.pathname.includes('/categories')) {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    })
    return
  }
  if (url.pathname.includes('/comments')) {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    })
    return
  }
  const m = url.pathname.match(/\/posts\/(\d+)$/)
  if (m) {
    const id = Number(m[1])
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: postDetail(id, { pinned: 1, featured: 1, title: '置顶且精华示例帖' })
      })
    })
    return
  }
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: 0, message: 'ok', data: { total: 0, current: 1, size: 20, records: [] } })
  })
})

await detailPage.goto(`${web}/forum/posts/${POST_PIN_FEATURED}`, {
  waitUntil: 'networkidle',
  timeout: 30000
})
await detailPage.waitForTimeout(800)
const bodyText = await detailPage.locator('main').innerText().catch(() => '')
await detailPage.screenshot({ path: join(outDir, '02-post-detail-both.png') })
const detailPinned = await detailPage.getByText('置顶', { exact: true }).count()
const detailFeatured = await detailPage.getByText('精华', { exact: true }).count()
const hasTitle = bodyText.includes('置顶且精华示例帖')
note(
  'post-detail',
  hasTitle && detailPinned >= 1 && detailFeatured >= 1 ? 'pass' : 'fail',
  `pinned=${detailPinned}; featured=${detailFeatured}; hasTitle=${hasTitle}; snippet=${bodyText.slice(0, 180).replace(/\n/g, ' ')}`
)
await detailPage.close()

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(
  join(outDir, 'result.json'),
  JSON.stringify({ postIds: [POST_PIN_FEATURED, POST_FEATURED], results }, null, 2)
)
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-FEATURE-PIN-BADGE 证据',
    '',
    `- mock postId：\`${POST_PIN_FEATURED}\`（置顶+精华）、\`${POST_FEATURED}\`（仅精华）`,
    '',
    '| 场景 | 截图 | 结果 |',
    '|---|---|---|',
    `| 论坛列表 | 01-forum-list-badges.png | ${results.find((r) => r.id === 'forum-list')?.status} |`,
    `| 帖子详情 | 02-post-detail-both.png | ${results.find((r) => r.id === 'post-detail')?.status} |`,
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
