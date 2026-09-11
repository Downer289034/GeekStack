/**
 * M6-1 日/夜主题截图（对照原型 Token 观感）
 * 依赖：前端 dev http://127.0.0.1:5173
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-1-tokens')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()

  async function shot(theme, file) {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    await page.addInitScript((t) => {
      localStorage.setItem('gs-theme', t)
    }, theme)
    await page.goto('http://127.0.0.1:5173/', { waitUntil: 'networkidle' })
    await page.waitForFunction((t) => document.documentElement.dataset.theme === t, theme)
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(outDir, file), fullPage: true })
    await page.close()
  }

  await shot('dark', 'theme-dark-1200.png')
  await shot('light', 'theme-light-1200.png')

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
