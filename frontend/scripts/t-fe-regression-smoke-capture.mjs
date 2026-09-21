/**
 * T-FE-REGRESSION-SMOKE：近期改动轻量浏览器回归
 * 1 搜索「找内容」+ 助手「社区问答」
 * 2 标签云含帖 hint + 详情三 Tab
 * 3 论坛列表置顶/精华徽章
 * 4 Owner 主页同步按钮（不强制点同步）
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-regression-smoke')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const TAG = 'Java'
const OWNER = 'regress_owner'
const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

function postCard(id, { pinned = 0, featured = 0, title }) {
  return {
    id,
    userId: 1,
    categoryId: 1,
    title,
    tags: [TAG],
    viewCount: 10,
    replyCount: 1,
    lastReplyAt: null,
    isPinned: pinned,
    isFeatured: featured,
    isHot: 0,
    createdAt: new Date().toISOString()
  }
}

const browser = await chromium.launch()

// ---------- 1) 搜索文案 + 助手 ----------
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(/\/api\/v1\//, async (route) => {
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
    if (url.includes('/users/me')) {
      await route.fulfill({
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
      return
    }
    await route.continue()
  })

  await page.goto(`${web}/search`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.search-page', { timeout: 15000 })
  const findTitle = await page.getByText('找内容', { exact: true }).count()
  const findHint = await page.getByText(/按关键字找帖子、项目与 Skill/).count()
  await page.screenshot({ path: join(outDir, '01-search-find-content.png') })

  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.setItem('community.token', 't')
    localStorage.setItem('community.refreshToken', 'r')
  })
  await page.goto(`${web}/`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.locator('.ai-fab').click()
  await page.waitForSelector('.ai-panel', { timeout: 10000 })
  const qaTitle = await page.getByText('社区问答', { exact: true }).count()
  const fabTitle = await page.locator('.ai-fab').getAttribute('title')
  await page.screenshot({ path: join(outDir, '02-assistant-community-qa.png') })
  note(
    'search-assistant-copy',
    findTitle >= 1 && findHint >= 1 && qaTitle >= 1 && fabTitle === '社区问答' ? 'pass' : 'fail',
    `findTitle=${findTitle}; findHint=${findHint}; qaTitle=${qaTitle}; fabTitle=${fabTitle}`
  )
  await page.close()
}

// ---------- 2) 标签云 hint + 详情三 Tab ----------
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  await page.route(/\/api\/v1\//, async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    if (path.includes('/tags/cloud') || path.endsWith('/tags')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: [{ name: TAG, count: 5 }]
        })
      })
      return
    }
    if (path.includes('/posts') && !path.match(/\/posts\/\d+/)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            total: 1,
            current: 1,
            size: 9,
            records: [postCard(11, { title: `${TAG} 帖子样例` })]
          }
        })
      })
      return
    }
    if (path.includes('/shares') && !path.match(/\/shares\/\d+/)) {
      await route.fulfill({
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
                title: `${TAG} 项目样例`,
                summary: 's',
                tags: [TAG],
                coverUrl: null,
                likeCount: 0,
                status: 'PUBLISHED',
                createdAt: new Date().toISOString()
              }
            ]
          }
        })
      })
      return
    }
    if (path.includes('/skills') && !path.match(/\/skills\/\d+/)) {
      await route.fulfill({
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
                id: 31,
                title: `${TAG} Skill 样例`,
                summary: 's',
                tags: [TAG],
                likeCount: 0,
                status: 'PUBLISHED',
                createdAt: new Date().toISOString()
              }
            ]
          }
        })
      })
      return
    }
    await route.continue()
  })

  await page.goto(`${web}/tags`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(400)
  const cloudHint = await page.getByText(/计数含可见帖子/).count()
  await page.screenshot({ path: join(outDir, '03-tags-cloud-hint.png') })

  await page.goto(`${web}/tags/${encodeURIComponent(TAG)}`, {
    waitUntil: 'networkidle',
    timeout: 30000
  })
  await page.waitForSelector('.gs-underline-tabs, [role="tablist"]', { timeout: 15000 }).catch(() => null)
  const tabPost = await page.getByRole('tab', { name: '帖子' }).count()
  const tabShare = await page.getByRole('tab', { name: '项目' }).count()
  const tabSkill = await page.getByRole('tab', { name: 'Skill' }).count()
  // fallback text if role not set
  const tabLabels =
    tabPost + tabShare + tabSkill >= 3
      ? 3
      : (await page.getByText('帖子', { exact: true }).count()) >= 1 &&
          (await page.getByText('项目', { exact: true }).count()) >= 1 &&
          (await page.getByText('Skill', { exact: true }).count()) >= 1
        ? 3
        : 0

  if (tabSkill >= 1 || (await page.getByText('Skill', { exact: true }).count()) >= 1) {
    const skillTab = page.getByRole('tab', { name: 'Skill' })
    if (await skillTab.count()) await skillTab.click()
    else await page.getByText('Skill', { exact: true }).first().click()
    await page.waitForTimeout(400)
  }
  await page.screenshot({ path: join(outDir, '04-tag-detail-tabs.png') })
  note(
    'tags-three-tabs',
    cloudHint >= 1 && tabLabels >= 3 ? 'pass' : 'fail',
    `cloudHint=${cloudHint}; tabs=${tabLabels}; roleTabs=${tabPost}/${tabShare}/${tabSkill}`
  )
  await page.close()
}

// ---------- 3) 论坛列表徽章 ----------
{
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
    if (url.pathname.match(/\/posts\/\d+/)) {
      await route.continue()
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
            postCard(9001, { pinned: 1, featured: 1, title: '置顶且精华样例' }),
            postCard(9002, { pinned: 0, featured: 1, title: '仅精华样例' })
          ]
        }
      })
    })
  })

  await page.goto(`${web}/forum`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.gs-feed-row', { timeout: 15000 })
  const pinned = await page.getByText('置顶', { exact: true }).count()
  const featured = await page.getByText('精华', { exact: true }).count()
  await page.screenshot({ path: join(outDir, '05-forum-pin-feature-badges.png') })
  note(
    'forum-badges',
    pinned >= 1 && featured >= 2 ? 'pass' : 'fail',
    `pinned=${pinned}; featured=${featured}`
  )
  await page.close()
}

// ---------- 4) Owner 同步按钮（不点同步） ----------
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(/\/api\/v1\//, async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    const method = route.request().method()
    if (method === 'GET' && path.includes('/users/me')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            id: 1,
            username: OWNER,
            nickname: OWNER,
            email: `${OWNER}@test.local`,
            avatarUrl: null,
            bio: null,
            role: 'USER',
            createdAt: new Date().toISOString(),
            hasPassword: true,
            oauthBindings: { github: true, gitee: true }
          }
        })
      })
      return
    }
    if (path.includes(`/users/${OWNER}/profile`)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: {
            profile: {
              id: 1,
              username: OWNER,
              nickname: OWNER,
              avatarUrl: null,
              bio: null,
              role: 'USER',
              createdAt: new Date().toISOString()
            },
            shares: [],
            posts: []
          }
        })
      })
      return
    }
    if (path.includes(`/users/${OWNER}/repos`)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: [] })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: null })
    })
  })

  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.setItem('community.token', 't')
    localStorage.setItem('community.refreshToken', 'r')
  })
  await page.goto(`${web}/users/${OWNER}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(500)
  const gh = await page.getByTestId('repo-sync-github').isVisible().catch(() => false)
  const gee = await page.getByTestId('repo-sync-gitee').isVisible().catch(() => false)
  await page.screenshot({ path: join(outDir, '06-profile-sync-btns.png') })
  note('profile-sync-btns', gh && gee ? 'pass' : 'fail', `github=${gh}; gitee=${gee}`)
  await page.close()
}

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(join(outDir, 'result.json'), JSON.stringify({ results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-REGRESSION-SMOKE 证据',
    '',
    '> 近期改动轻量回归：搜索文案 / 标签三 Tab / 精华置顶 / 同步按钮',
    '',
    '| # | 场景 | 截图 | 结果 |',
    '|---|---|---|---|',
    `| 1 | 搜索「找内容」+ 助手「社区问答」 | 01-search-find-content.png · 02-assistant-community-qa.png | ${results.find((r) => r.id === 'search-assistant-copy')?.status} |`,
    `| 2 | 标签云含帖 + 详情三 Tab | 03-tags-cloud-hint.png · 04-tag-detail-tabs.png | ${results.find((r) => r.id === 'tags-three-tabs')?.status} |`,
    `| 3 | 论坛置顶/精华徽章 | 05-forum-pin-feature-badges.png | ${results.find((r) => r.id === 'forum-badges')?.status} |`,
    `| 4 | Owner 同步按钮（未强制同步） | 06-profile-sync-btns.png | ${results.find((r) => r.id === 'profile-sync-btns')?.status} |`,
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
