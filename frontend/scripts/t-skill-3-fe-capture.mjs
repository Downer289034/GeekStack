import { chromium } from 'playwright'
import { readFileSync, writeFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'

const root = join(dirname(fileURLToPath(import.meta.url)), '../..')
const outDir = join(root, 'docs/assets/t-skill-3-fe')
const sample = readFileSync(join(outDir, 'sample-SKILL.md'), 'utf8')
const browser = await chromium.launch()
const viewports = [
  { name: 'desktop-1920', width: 1920, height: 1080 },
  { name: 'tablet-768', width: 768, height: 1024 },
  { name: 'mobile-375', width: 375, height: 812 }
]

async function openPublish(page) {
  await page.addInitScript(() => {
    localStorage.setItem('community.token', 't-skill-3-fe-shot-token')
    localStorage.setItem('community.refreshToken', 't-skill-3-fe-shot-refresh')
  })
  await page.goto('http://localhost:5173/skills/new', { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForSelector('.publish-title', { timeout: 15000 })
}

for (const vp of viewports) {
  const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
  await openPublish(page)
  await page.locator('.n-form').locator('input').first().fill('java exception analyzer')
  await page.locator('textarea').first().fill('Analyze Java stack traces…')
  const versionInput = page.locator('.version-input input')
  if (await versionInput.count()) {
    await versionInput.fill('1.2.0')
  }
  const changelog = page.getByPlaceholder('本版改了什么（选填）')
  if (await changelog.count()) {
    await changelog.fill('对齐 SkillHub：新增 SemVer 与变更说明')
  }
  await page.screenshot({ path: join(outDir, `01-publish-${vp.name}.png`), fullPage: true })
  await page.close()
}

const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
await openPublish(page)
const summary = page.locator('textarea').first()
await summary.focus()
await page.evaluate(async (text) => {
  const dt = new DataTransfer()
  dt.setData('text/plain', text)
  document.activeElement?.dispatchEvent(
    new ClipboardEvent('paste', { clipboardData: dt, bubbles: true, cancelable: true })
  )
}, sample)
await page.waitForTimeout(500)
const titleVal = await page.locator('.n-form').locator('input').first().inputValue()
const summaryVal = await summary.inputValue()
writeFileSync(
  join(outDir, 'paste-extract.json'),
  JSON.stringify(
    {
      title: titleVal,
      summary: summaryVal,
      ok: /java exception/i.test(titleVal) && /stack traces/i.test(summaryVal)
    },
    null,
    2
  )
)
await page.screenshot({ path: join(outDir, '02-paste-extract.png'), fullPage: true })
await browser.close()
console.log('screenshots done', { titleVal, summaryOk: /stack traces/i.test(summaryVal) })
