/**
 * 设置页头像上传预览 + 保存后顶栏同步证据。
 * 用法：vite:5173 + backend 就绪后：node frontend/scripts/avatar-preview-capture.mjs
 */
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/avatar-preview')
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
    headers: {
      'Content-Type': 'application/json',
      ...(opts.headers || {})
    }
  })
  const body = await res.json().catch(() => ({}))
  return { status: res.status, body }
}

/** 生成可辨识头像（蓝底黄圆+「头」），避免 1×1 放大后像纯色 fallback */
async function makeAvatarPng(browser) {
  const p = await browser.newPage()
  const dataUrl = await p.evaluate(() => {
    const c = document.createElement('canvas')
    c.width = 128
    c.height = 128
    const ctx = c.getContext('2d')
    ctx.fillStyle = '#2563eb'
    ctx.fillRect(0, 0, 128, 128)
    ctx.fillStyle = '#fbbf24'
    ctx.beginPath()
    ctx.arc(64, 64, 48, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#0f172a'
    ctx.font = 'bold 56px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('头', 64, 68)
    return c.toDataURL('image/png')
  })
  await p.close()
  const b64 = dataUrl.replace(/^data:image\/png;base64,/, '')
  return Buffer.from(b64, 'base64')
}

const marker = Date.now().toString(36)
const user = `avaprev_${marker}`
const password = 'password123'
{
  const reg = await apiJson('/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username: user, email: `${user}@test.local`, password })
  })
  if (reg.body.code !== 0 && reg.body.code !== 40001) {
    console.log('register', JSON.stringify(reg.body).slice(0, 200))
  }
}
const login = await apiJson('/v1/auth/login', {
  method: 'POST',
  body: JSON.stringify({ username: user, password })
})
if (login.body.code !== 0) {
  throw new Error(`login failed: ${JSON.stringify(login.body)}`)
}
const token = login.body.data.accessToken
const refresh = login.body.data.refreshToken
const demoMediaPath = `/media/avatar-preview/${marker}.png`

const browser = await chromium.launch()
const PNG_AVATAR = await makeAvatarPng(browser)
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })

await page.route('**/media/avatar-preview/**', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'image/png',
    body: PNG_AVATAR
  })
})

await page.route('**/api/v1/media/upload**', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      code: 0,
      message: 'ok',
      data: {
        url: `http://172.25.41.137:9080${demoMediaPath}`,
        key: `media/avatar-preview/${marker}.png`,
        contentType: 'image/png',
        size: PNG_AVATAR.length
      }
    })
  })
})

let savedAvatar = null
await page.route('**/api/v1/users/me**', async (route) => {
  const req = route.request()
  if (req.method() === 'PUT') {
    const post = JSON.parse(req.postData() || '{}')
    savedAvatar = post.avatarUrl ?? savedAvatar
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          id: 1,
          username: user,
          nickname: user,
          bio: post.bio ?? null,
          avatarUrl: savedAvatar,
          hasPassword: true,
          oauthBindings: { github: false, gitee: false }
        }
      })
    })
    return
  }
  if (req.method() === 'GET') {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        message: 'ok',
        data: {
          id: 1,
          username: user,
          nickname: user,
          bio: null,
          avatarUrl: savedAvatar,
          hasPassword: true,
          oauthBindings: { github: false, gitee: false }
        }
      })
    })
    return
  }
  await route.continue()
})

await page.goto(web, { waitUntil: 'domcontentloaded' })
await page.evaluate(
  ([a, r]) => {
    localStorage.setItem('community.token', a)
    localStorage.setItem('community.refreshToken', r)
  },
  [token, refresh]
)

await page.goto(`${web}/settings`, { waitUntil: 'networkidle', timeout: 30000 })
await page.waitForSelector('.avatar-row', { timeout: 15000 })
await page.waitForTimeout(600)

const beforeSrc = await page.locator('.avatar-row .n-avatar img').count()
await page.screenshot({ path: join(outDir, '01-settings-before-upload.png') })
note(
  'before-upload',
  beforeSrc === 0 ? 'pass' : 'info',
  `previewImgCount=${beforeSrc}（上传前应无图、字母 fallback）`
)

const input = page.locator('.avatar-row input[type="file"]')
await input.setInputFiles({
  name: 'avatar.png',
  mimeType: 'image/png',
  buffer: PNG_AVATAR
})

await page.waitForTimeout(1500)
const toastOk = await page.getByText('头像已上传，请点击保存资料').count()
const afterImg = await page.locator('.avatar-row .n-avatar img').count()
const afterSrc = afterImg
  ? await page.locator('.avatar-row .n-avatar img').first().getAttribute('src')
  : null
await page.screenshot({ path: join(outDir, '02-settings-after-upload.png') })
note(
  'after-upload-preview',
  toastOk >= 1 && afterImg >= 1 && afterSrc ? 'pass' : 'fail',
  `toast=${toastOk}; img=${afterImg}; src=${afterSrc}`
)

await page.getByRole('button', { name: '保存资料' }).click()
await page.waitForTimeout(1200)
const saveToast = await page.getByText('资料已保存').count()
const navImg = await page.locator('.gs-avatar__img').count()
const navSrc = navImg ? await page.locator('.gs-avatar__img').first().getAttribute('src') : null
await page.screenshot({ path: join(outDir, '03-topbar-after-save.png') })
note(
  'after-save-topbar',
  saveToast >= 1 && navImg >= 1 && navSrc ? 'pass' : 'fail',
  `saveToast=${saveToast}; navImg=${navImg}; navSrc=${navSrc}; savedAvatar=${savedAvatar}`
)

await page.reload({ waitUntil: 'networkidle' })
await page.waitForTimeout(800)
const navAfterReload = await page.locator('.gs-avatar__img').count()
await page.screenshot({ path: join(outDir, '04-topbar-after-reload.png') })
note('after-reload', navAfterReload >= 1 ? 'pass' : 'fail', `navImg=${navAfterReload}`)

await browser.close()

const failed = results.filter((r) => r.status === 'fail')
writeFileSync(join(outDir, 'result.json'), JSON.stringify({ user, demoMediaPath, results }, null, 2))
writeFileSync(
  join(outDir, 'README.md'),
  [
    '# 头像上传预览证据',
    '',
    `- 用户：\`${user}\``,
    `- 媒体演示路径：\`${demoMediaPath}\``,
    '',
    '| 步骤 | 截图 | 结果 |',
    '|---|---|---|',
    `| 上传前 | 01-settings-before-upload.png | ${results.find((r) => r.id === 'before-upload')?.status} |`,
    `| 上传后预览 | 02-settings-after-upload.png | ${results.find((r) => r.id === 'after-upload-preview')?.status} |`,
    `| 保存后顶栏 | 03-topbar-after-save.png | ${results.find((r) => r.id === 'after-save-topbar')?.status} |`,
    `| 刷新后顶栏 | 04-topbar-after-reload.png | ${results.find((r) => r.id === 'after-reload')?.status} |`,
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
