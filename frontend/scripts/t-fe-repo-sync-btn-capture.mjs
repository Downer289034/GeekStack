/**
 * T-FE-REPO-SYNC-BTN 证据：Owner 绑定态可见同步按钮；访客无按钮；点击后列表刷新。
 * 全程 Playwright route mock（不依赖 A 的 sync API 是否已 push）。
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-repo-sync-btn')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
mkdirSync(outDir, { recursive: true })

const OWNER = 'syncowner_mock'
const VISITOR = 'visitor_mock'

const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

function profilePayload(user) {
  return {
    code: 0,
    message: 'ok',
    data: {
      profile: {
        id: 1,
        username: user,
        nickname: user,
        avatarUrl: null,
        bio: '同步按钮证据',
        role: 'USER',
        createdAt: new Date().toISOString()
      },
      shares: [],
      posts: []
    }
  }
}

function mePayload(user, bindings) {
  return {
    code: 0,
    message: 'ok',
    data: {
      id: 1,
      username: user,
      nickname: user,
      email: `${user}@test.local`,
      avatarUrl: null,
      bio: null,
      role: 'USER',
      createdAt: new Date().toISOString(),
      hasPassword: true,
      oauthBindings: bindings
    }
  }
}

function repoCard(id, name) {
  return {
    id,
    userId: 1,
    provider: 'GITHUB',
    repoFullName: name,
    repoUrl: `https://github.com/${name}`,
    description: 'mock repo',
    stars: 3,
    pushedAt: null,
    syncedAt: new Date().toISOString(),
    hidden: 0
  }
}

async function seedAuth(page, user, token = 'mock-token') {
  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(
    ({ token }) => {
      localStorage.setItem('community.token', token)
      localStorage.setItem('community.refreshToken', 'mock-refresh')
    },
    { token }
  )
  void user
}

const browser = await chromium.launch()

// ---- 1) Owner 已绑定：可见同步按钮（空列表）----
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  let syncHits = 0
  let repoPhase = 'empty'

  await page.route(/\/api\/v1\//, async (route) => {
    const url = new URL(route.request().url())
    const method = route.request().method()
    const path = url.pathname

    if (method === 'GET' && path.includes('/users/me')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mePayload(OWNER, { github: true, gitee: true }))
      })
      return
    }
    if (method === 'GET' && path.includes(`/users/${OWNER}/profile`)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(profilePayload(OWNER))
      })
      return
    }
    if (method === 'GET' && path.includes(`/users/${OWNER}/repos`)) {
      const data = repoPhase === 'empty' ? [] : [repoCard(101, 'alice/after-sync')]
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data })
      })
      return
    }
    if (method === 'POST' && path.includes('/me/repos/sync')) {
      syncHits += 1
      const provider = (url.searchParams.get('provider') || 'GITHUB').toUpperCase()
      repoPhase = 'filled'
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 0,
          message: 'ok',
          data: { provider, syncedCount: 1 }
        })
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: null })
    })
  })

  await seedAuth(page, OWNER)
  await page.goto(`${web}/users/${OWNER}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(500)

  const githubBtn = page.getByTestId('repo-sync-github')
  const giteeBtn = page.getByTestId('repo-sync-gitee')
  const githubVisible = await githubBtn.isVisible().catch(() => false)
  const giteeVisible = await giteeBtn.isVisible().catch(() => false)
  await page.screenshot({ path: join(outDir, '01-owner-bound-sync-btns.png') })
  note(
    'owner-bound-btns',
    githubVisible && giteeVisible ? 'pass' : 'fail',
    `github=${githubVisible}; gitee=${giteeVisible}`
  )

  await githubBtn.click()
  await page.waitForTimeout(600)
  const afterSyncRepo = await page.getByText('alice/after-sync').count()
  await page.screenshot({ path: join(outDir, '02-owner-after-sync.png') })
  note(
    'owner-sync-refresh',
    syncHits >= 1 && afterSyncRepo >= 1 ? 'pass' : 'fail',
    `syncHits=${syncHits}; repoVisible=${afterSyncRepo}`
  )
  await page.close()
}

// ---- 2) 访客：无同步按钮 ----
{
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(/\/api\/v1\//, async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    if (path.includes(`/users/${OWNER}/profile`)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(profilePayload(OWNER))
      })
      return
    }
    if (path.includes(`/users/${OWNER}/repos`)) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 0, message: 'ok', data: [repoCard(1, 'alice/public')] })
      })
      return
    }
    if (path.includes('/users/me')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mePayload(VISITOR, { github: true, gitee: false }))
      })
      return
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: null })
    })
  })

  await seedAuth(page, VISITOR)
  await page.goto(`${web}/users/${OWNER}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(400)
  const githubCount = await page.getByTestId('repo-sync-github').count()
  const giteeCount = await page.getByTestId('repo-sync-gitee').count()
  await page.screenshot({ path: join(outDir, '03-visitor-no-sync-btn.png') })
  note(
    'visitor-no-btn',
    githubCount === 0 && giteeCount === 0 ? 'pass' : 'fail',
    `github=${githubCount}; gitee=${giteeCount}`
  )
  await page.close()
}

await browser.close()

writeFileSync(
  join(outDir, 'result.json'),
  JSON.stringify({ api: 'POST /api/v1/me/repos/sync?provider=', results }, null, 2)
)
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-REPO-SYNC-BTN 证据',
    '',
    '- 契约：`POST /api/v1/me/repos/sync?provider=GITHUB|GITEE`（依赖 A `T-REPO-MANUAL-SYNC-API`）',
    '- 本证据为 Playwright mock（A 未合入 origin 时仍可关 UI 验收）',
    '',
    '| 场景 | 截图 | 结果 |',
    '|---|---|---|',
    `| Owner 绑定态可见同步按钮 | 01-owner-bound-sync-btns.png | ${results.find((r) => r.id === 'owner-bound-btns')?.status} |`,
    `| 点击同步后刷新列表 | 02-owner-after-sync.png | ${results.find((r) => r.id === 'owner-sync-refresh')?.status} |`,
    `| 访客无同步按钮 | 03-visitor-no-sync-btn.png | ${results.find((r) => r.id === 'visitor-no-btn')?.status} |`,
    '',
    '```json',
    JSON.stringify(results, null, 2),
    '```',
    ''
  ].join('\n')
)

const failed = results.filter((r) => r.status === 'fail')
if (failed.length) {
  console.error('FAILED', failed)
  process.exit(1)
}
console.log('all pass →', outDir)
