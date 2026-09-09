/**
 * T-FE-3 三断点截图：搜索 / 标签云 / 标签详情
 * 依赖：前端 dev http://127.0.0.1:5173
 * 搜索/标签云接口可 mock（兼容未热更的后端）；标签详情走真实 shares?tag=
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

const mockTags = [
  { name: 'Vue', count: 12 },
  { name: 'Spring', count: 9 },
  { name: 'TypeScript', count: 7 },
  { name: 'MySQL', count: 5 },
  { name: 'Redis', count: 3 },
  { name: 'Docker', count: 2 }
]

const mockPosts = {
  total: 2,
  current: 1,
  size: 10,
  records: [
    {
      id: 101,
      categoryId: 1,
      title: 'Vue 3 组合式 API 实践笔记',
      replyCount: 4,
      viewCount: 128,
      isPinned: 0,
      createdAt: '2026-09-08T10:00:00'
    },
    {
      id: 102,
      categoryId: 2,
      title: 'Vite + Vue 工程化踩坑',
      replyCount: 1,
      viewCount: 56,
      isPinned: 0,
      createdAt: '2026-09-07T18:30:00'
    }
  ]
}

const mockShares = {
  total: 2,
  current: 1,
  size: 10,
  records: [
    {
      id: 201,
      title: 'GeekStack 前端脚手架',
      summary: 'Vue3 + Naive UI 社区前端模板',
      tags: ['Vue', 'TypeScript'],
      repoUrl: 'https://example.com/repo',
      viewCount: 88,
      createdAt: '2026-09-08T12:00:00'
    },
    {
      id: 202,
      title: '搜索体验 Demo',
      summary: '防抖与空态演示项目',
      tags: ['Vue'],
      repoUrl: null,
      viewCount: 21,
      createdAt: '2026-09-06T09:00:00'
    }
  ]
}

const mockCategories = [
  { id: 1, name: '经验分享', sortOrder: 1 },
  { id: 2, name: '踩坑记录', sortOrder: 2 }
]

function ok(data) {
  return JSON.stringify({ code: 0, message: 'ok', data })
}

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/tfe3-search')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const viewports = [
    { name: '375', width: 375, height: 812 },
    { name: '768', width: 768, height: 1024 },
    { name: '1200', width: 1200, height: 900 }
  ]

  let tagName = 'Vue'
  try {
    const tagsRes = await fetch('http://127.0.0.1:8080/api/v1/tags')
    if (tagsRes.ok) {
      const tagsJson = await tagsRes.json()
      const list = tagsJson?.data
      if (Array.isArray(list) && list.length) tagName = list[0].name
    }
  } catch {
    /* mock fallback */
  }

  async function withSearchMocks(page) {
    await page.route('**/api/v1/search**', async (route) => {
      const u = new URL(route.request().url())
      const q = u.searchParams.get('q') || ''
      const type = u.searchParams.get('type') || 'post'
      if (!q || q.includes('__no_such_keyword')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: ok({ total: 0, current: 1, size: 10, records: [] })
        })
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: ok(type === 'share' ? mockShares : mockPosts)
      })
    })
    await page.route('**/api/v1/posts/categories**', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: ok(mockCategories) })
    )
    await page.route('**/api/v1/tags', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: ok(mockTags) })
    )
  }

  for (const vp of viewports) {
    {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await withSearchMocks(page)
      await page.goto('http://127.0.0.1:5173/search?q=Vue&type=post', { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(outDir, `search-post-${vp.name}.png`), fullPage: true })
      await page.close()
    }

    {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await withSearchMocks(page)
      await page.goto(
        'http://127.0.0.1:5173/search?q=__no_such_keyword_tfe3__&type=share',
        { waitUntil: 'networkidle' }
      )
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(outDir, `search-empty-${vp.name}.png`), fullPage: true })
      await page.close()
    }

    {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await withSearchMocks(page)
      await page.goto('http://127.0.0.1:5173/tags', { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(outDir, `tags-${vp.name}.png`), fullPage: true })
      await page.close()
    }

    {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await page.goto(
        `http://127.0.0.1:5173/tags/${encodeURIComponent(tagName)}`,
        { waitUntil: 'networkidle' }
      )
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(outDir, `tag-detail-${vp.name}.png`), fullPage: true })
      await page.close()
    }
  }

  console.log('SHOT_OK', { tagName, outDir })
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
