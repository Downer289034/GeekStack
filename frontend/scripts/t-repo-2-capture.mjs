/**
 * T-REPO-2 证据：主页仓库空态（未绑定 / 已绑定无仓 / repos 失败不 404）
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-repo-2')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
const api = process.env.SMOKE_API || 'http://127.0.0.1:8080/api'
mkdirSync(outDir, { recursive: true })

const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

async function apiJson(path, opts = {}) {
  const res = await fetch(`${api}${path}`, {
    ...opts,
    headers: { 'Content-Type': 'application/json', ...(opts.headers || {}) }
  })
  return { status: res.status, body: await res.json().catch(() => ({})) }
}

async function ensureUser(prefix) {
  const marker = Date.now().toString(36)
  const user = `${prefix}_${marker}`
  const password = 'password123'
  await apiJson('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username: user, email: `${user}@test.local`, password })
  }).catch(() => null)
  const login = await apiJson('/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: user, password })
  })
  if (login.body.code !== 0) throw new Error(`login failed ${JSON.stringify(login.body)}`)
  return {
    user,
    token: login.body.data.accessToken,
    refresh: login.body.data.refreshToken
  }
}

function profilePayload(user, nickname = user) {
  return {
    code: 0,
    message: 'ok',
    data: {
      profile: {
        id: 1,
        username: user,
        nickname,
        avatarUrl: null,
        bio: null,
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

const browser = await chromium.launch()

// ---- 1) owner 未绑定 → 去绑定 ----
{
  let cred
  try {
    cred = await ensureUser('repounb')
  } catch (e) {
    // backend down: synthetic session with mocked APIs only
    cred = { user: 'repounb_mock', token: 't', refresh: 'r' }
    console.log('register/login skipped:', String(e.message || e).slice(0, 80))
  }
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(`**/api/v1/users/${cred.user}/profile**`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(profilePayload(cred.user)) })
  )
  await page.route(`**/api/v1/users/${cred.user}/repos**`, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    })
  )
  await page.route('**/api/v1/users/me**', (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mePayload(cred.user, { github: false, gitee: false }))
    })
  })
  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(
    ([a, r]) => {
      localStorage.setItem('community.token', a)
      localStorage.setItem('community.refreshToken', r)
    },
    [cred.token, cred.refresh]
  )
  await page.goto(`${web}/users/${cred.user}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.profile-section', { timeout: 15000 })
  await page.waitForTimeout(600)
  const bindBtn = page.getByRole('link', { name: '去绑定' })
  const bindCount = await bindBtn.count()
  const desc = await page.getByText('尚未绑定 GitHub / Gitee').count()
  await page.screenshot({ path: join(outDir, '01-owner-unbound.png') })
  if (bindCount >= 1) {
    await bindBtn.first().click()
    await page.waitForURL(/\/settings/, { timeout: 10000 })
  }
  const onSettings = /\/settings/.test(page.url())
  note(
    'owner-unbound',
    bindCount >= 1 && desc >= 1 && onSettings ? 'pass' : 'fail',
    `bindBtn=${bindCount}; desc=${desc}; onSettings=${onSettings}`
  )
  await page.close()
}

// ---- 2) owner 已绑定无仓 ----
{
  const user = 'repobound_mock'
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(`**/api/v1/users/${user}/profile**`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(profilePayload(user)) })
  )
  await page.route(`**/api/v1/users/${user}/repos**`, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 0, message: 'ok', data: [] })
    })
  )
  await page.route('**/api/v1/users/me**', (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mePayload(user, { github: true, gitee: false }))
    })
  })
  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.setItem('community.token', 't')
    localStorage.setItem('community.refreshToken', 'r')
  })
  await page.goto(`${web}/users/${user}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForSelector('.profile-section', { timeout: 15000 })
  await page.waitForTimeout(600)
  const neutral = await page.getByText('暂无公开仓库').count()
  const noBindCta = await page.getByRole('link', { name: '去绑定' }).count()
  const not404 = await page.getByText('用户不存在').count()
  await page.screenshot({ path: join(outDir, '02-owner-bound-empty.png') })
  note(
    'owner-bound-empty',
    neutral >= 1 && noBindCta === 0 && not404 === 0 ? 'pass' : 'fail',
    `neutral=${neutral}; bindCta=${noBindCta}; not404=${not404 === 0}`
  )
  await page.close()
}

// ---- 3) repos 失败 ≠ 整页用户不存在 ----
{
  const user = 'repofail_mock'
  const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
  await page.route(`**/api/v1/users/${user}/profile**`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(profilePayload(user, 'RepoFail')) })
  )
  await page.route(`**/api/v1/users/${user}/repos**`, (route) =>
    route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ code: 50000, message: 'repos down' })
    })
  )
  await page.route('**/api/v1/users/me**', (route) => {
    if (route.request().method() !== 'GET') return route.continue()
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mePayload(user, { github: true, gitee: false }))
    })
  })
  await page.goto(web, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => {
    localStorage.setItem('community.token', 't')
    localStorage.setItem('community.refreshToken', 'r')
  })
  await page.goto(`${web}/users/${user}`, { waitUntil: 'networkidle', timeout: 30000 })
  await page.waitForTimeout(800)
  const profileOk = await page.getByText('RepoFail').count()
  const repoErr = await page.getByText('仓库列表暂时无法加载').count()
  const is404 = await page.getByText('用户不存在').count()
  await page.screenshot({ path: join(outDir, '03-repos-fail-not-404.png') })
  note(
    'repos-fail-not-404',
    profileOk >= 1 && repoErr >= 1 && is404 === 0 ? 'pass' : 'fail',
    `profileOk=${profileOk}; repoErr=${repoErr}; is404=${is404}`
  )
  await page.close()
}

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(join(outDir, 'result.json'), JSON.stringify({ results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-REPO-2 主页仓库空态证据',
    '',
    '| 场景 | 截图 | 结果 |',
    '|---|---|---|',
    `| owner 未绑定 → 去绑定 | 01-owner-unbound.png | ${results.find((r) => r.id === 'owner-unbound')?.status} |`,
    `| owner 已绑定无仓 | 02-owner-bound-empty.png | ${results.find((r) => r.id === 'owner-bound-empty')?.status} |`,
    `| repos 失败不整页 404 | 03-repos-fail-not-404.png | ${results.find((r) => r.id === 'repos-fail-not-404')?.status} |`,
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
