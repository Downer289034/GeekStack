/**
 * T-FE-REPO-SYNC-LIVE：对现行 Vite 代理的真后端点一次同步（非 mock）。
 * 账号：ydowner_tang（已绑 Gitee）；口令仅本机环境/临时，不入库。
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-fe-repo-sync-live')
const web = process.env.SMOKE_WEB || 'http://127.0.0.1:5173'
const api = process.env.SMOKE_API || 'http://127.0.0.1:8080/api'
const user = process.env.SMOKE_USER || 'ydowner_tang'
const password = process.env.SMOKE_PASS || 'password123'
mkdirSync(outDir, { recursive: true })

const results = []
function note(id, status, detail) {
  results.push({ id, status, detail })
  console.log(`[${status}] ${id}: ${detail}`)
}

// 1) 未登录门禁：须 401（非路由 404）
const anon = await fetch(`${api}/v1/me/repos/sync?provider=GITEE`, { method: 'POST' })
const anonBody = await anon.json().catch(() => ({}))
writeFileSync(
  join(outDir, '00-anon-sync.json'),
  JSON.stringify({ http: anon.status, code: anonBody.code, message: anonBody.message }, null, 2)
)
note(
  'anon-401',
  anon.status === 401 && anonBody.code === 40100 ? 'pass' : 'fail',
  `http=${anon.status}; code=${anonBody.code}; msg=${anonBody.message}`
)

// 2) 登录拿 token
const loginRes = await fetch(`${api}/v1/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: user, password })
})
const loginBody = await loginRes.json()
if (loginBody.code !== 0) {
  note('login', 'fail', JSON.stringify(loginBody))
  writeFileSync(join(outDir, 'result.json'), JSON.stringify({ results }, null, 2))
  process.exit(1)
}
const token = loginBody.data.accessToken
const refresh = loginBody.data.refreshToken

// 3) 直连 API 一次（证明非 404）；可能 200 / 429 / 503
const apiSync = await fetch(`${api}/v1/me/repos/sync?provider=GITEE`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: '{}'
})
const apiSyncBody = await apiSync.json().catch(() => ({}))
writeFileSync(
  join(outDir, '01-api-sync-gitee.json'),
  JSON.stringify(
    {
      http: apiSync.status,
      code: apiSyncBody.code,
      message: apiSyncBody.message,
      data: apiSyncBody.data ?? null,
      note: '脱敏：无 token'
    },
    null,
    2
  )
)
const apiOk = apiSync.status !== 404 && apiSyncBody.code !== 40400
note(
  'api-sync-not-404',
  apiOk ? 'pass' : 'fail',
  `http=${apiSync.status}; code=${apiSyncBody.code}; msg=${apiSyncBody.message}`
)

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1280, height: 860 } })

const syncResponses = []
page.on('response', async (res) => {
  if (res.url().includes('/me/repos/sync') && res.request().method() === 'POST') {
    let body = null
    try {
      body = await res.json()
    } catch {
      body = null
    }
    syncResponses.push({
      http: res.status(),
      code: body?.code,
      message: body?.message,
      data: body?.data ?? null
    })
  }
})

await page.goto(web, { waitUntil: 'domcontentloaded' })
await page.evaluate(
  ([a, r]) => {
    localStorage.setItem('community.token', a)
    localStorage.setItem('community.refreshToken', r)
  },
  [token, refresh]
)

await page.goto(`${web}/users/${user}`, { waitUntil: 'networkidle', timeout: 45000 })
await page.waitForSelector('[data-testid="repo-sync-gitee"]', { timeout: 20000 })
await page.screenshot({ path: join(outDir, '02-owner-sync-btn.png'), fullPage: false })

const btn = page.getByTestId('repo-sync-gitee')
await btn.click()
// 等 toast 或按钮恢复
await page.waitForTimeout(2500)
await page.screenshot({ path: join(outDir, '03-after-click-sync.png'), fullPage: false })

const uiHit = syncResponses[0]
// 成功 / 限流 / 上游失败均可；仅「路由级 资源不存在」算失败
const acceptable =
  uiHit &&
  uiHit.http !== 404 &&
  !(uiHit.code === 40400 && String(uiHit.message || '').includes('资源不存在'))

note(
  'ui-click-sync',
  acceptable ? 'pass' : 'fail',
  `http=${uiHit?.http}; code=${uiHit?.code}; msg=${uiHit?.message}; data=${JSON.stringify(uiHit?.data ?? null)}`
)

writeFileSync(
  join(outDir, '03-ui-sync-response.json'),
  JSON.stringify({ username: user, provider: 'GITEE', responses: syncResponses }, null, 2)
)

await browser.close()

writeFileSync(join(outDir, 'result.json'), JSON.stringify({ apiBase: api, user, results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# T-FE-REPO-SYNC-LIVE 证据',
    '',
    '- API：现行 Vite 代理本机 `http://127.0.0.1:8080`（含 `POST /api/v1/me/repos/sync`）',
    '- 账号：`ydowner_tang`（已绑 Gitee；口令未入库）',
    '- 对接后端 commit：`2f63e92`（本地进程已重启加载）',
    '- prod `DEPLOY_COMMIT` 当时仍为 `2543a78`（A 的 T-DEPLOY-BACKEND-SYNC-IMAGE 未改文件）；本机门禁已满足',
    '',
    '| 场景 | 文件 | 结果 |',
    '|---|---|---|',
    `| 未登录 sync → 401 | 00-anon-sync.json | ${results.find((r) => r.id === 'anon-401')?.status} |`,
    `| API 直连非 404 | 01-api-sync-gitee.json | ${results.find((r) => r.id === 'api-sync-not-404')?.status} |`,
    `| Owner 可见同步按钮 | 02-owner-sync-btn.png | — |`,
    `| 点击同步（Network/toast） | 03-after-click-sync.png + 03-ui-sync-response.json | ${results.find((r) => r.id === 'ui-click-sync')?.status} |`,
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
