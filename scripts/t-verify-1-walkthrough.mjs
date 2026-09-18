import { chromium } from 'playwright'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const OUT = path.join(root, 'docs', 'assets', 't-verify-1')
fs.mkdirSync(OUT, { recursive: true })

const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } })
const log = []

await page.goto('http://127.0.0.1:5173/', { waitUntil: 'networkidle' })
await page.evaluate(() => {
  document.documentElement.dataset.theme = 'dark'
  localStorage.setItem('gs-theme', 'dark')
})
await page.reload({ waitUntil: 'networkidle' })
await page.waitForTimeout(800)
let hasCanvas = await page.locator('canvas.gs-particles').count()
const darkRgb = await page.evaluate(() =>
  getComputedStyle(document.documentElement).getPropertyValue('--particles-rgb').trim()
)
await page.screenshot({ path: path.join(OUT, '01-home-dark.png') })
log.push({ step: 'dark-home', hasCanvas, darkRgb })

const toggle = page.locator('.gs-theme-toggle')
if ((await toggle.count()) > 0) await toggle.click()
await page.waitForTimeout(700)
const lightRgb = await page.evaluate(() =>
  getComputedStyle(document.documentElement).getPropertyValue('--particles-rgb').trim()
)
hasCanvas = await page.locator('canvas.gs-particles').count()
await page.screenshot({ path: path.join(OUT, '02-home-light.png') })
log.push({ step: 'light-home', hasCanvas, lightRgb })

await page.goto('http://127.0.0.1:5173/login', { waitUntil: 'networkidle' })
await page.waitForTimeout(500)
const loginCanvas = await page.locator('canvas.gs-particles').count()
await page.screenshot({ path: path.join(OUT, '03-login-no-particles.png') })
log.push({ step: 'login-exclude', loginCanvas, pass: loginCanvas === 0 })

await page.goto('http://127.0.0.1:5173/', { waitUntil: 'networkidle' })
await page.waitForTimeout(400)
const vis = await page.evaluate(() => {
  Object.defineProperty(document, 'hidden', { configurable: true, get: () => true })
  document.dispatchEvent(new Event('visibilitychange'))
  return { hiddenSimulated: document.hidden }
})
log.push({
  step: 'visibilitychange-hidden',
  ...vis,
  note: 'stop() on hidden; Task Manager CPU zero needs real tab switch'
})

await page.goto('http://127.0.0.1:5173/oauth/callback/gitee', { waitUntil: 'domcontentloaded' })
await page.evaluate(() => {
  sessionStorage.setItem('oauth_csrf_state_gitee', 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
  localStorage.setItem(
    'oauth_pending_gitee',
    JSON.stringify({
      provider: 'gitee',
      code: 'fake-code',
      state: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
    })
  )
})
await page.waitForTimeout(1200)
const errText = await page
  .locator('.n-empty__description')
  .innerText()
  .catch(() => '')
const pendingGone = await page.evaluate(() => localStorage.getItem('oauth_pending_gitee'))
const sessGone = await page.evaluate(() => sessionStorage.getItem('oauth_csrf_state_gitee'))
await page.screenshot({ path: path.join(OUT, '04-oauth-state-mismatch.png') })
log.push({
  step: 'sec1-tamper-state',
  errText,
  pendingGone,
  sessGone,
  pass: /state/.test(errText) && pendingGone === null
})

fs.writeFileSync(path.join(OUT, 'walkthrough.json'), JSON.stringify(log, null, 2), 'utf8')
console.log(JSON.stringify(log, null, 2))
await browser.close()

if (!log.find((x) => x.step === 'login-exclude')?.pass) process.exit(1)
if (!log.find((x) => x.step === 'sec1-tamper-state')?.pass) process.exit(1)
