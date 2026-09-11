/**
 * M6-2 布局骨架三断点截图（对照蓝图）
 * 依赖：前端 dev http://127.0.0.1:5173
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-2-layout')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const viewports = [
    { name: '375', width: 375, height: 812 },
    { name: '768', width: 768, height: 1024 },
    { name: '1200', width: 1200, height: 900 }
  ]

  const pages = [
    { key: 'home', url: '/' },
    { key: 'shares', url: '/shares' },
    { key: 'forum', url: '/forum' },
    { key: 'search', url: '/search' },
    { key: 'login', url: '/login' }
  ]

  for (const vp of viewports) {
    for (const p of pages) {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
      await page.goto(`http://127.0.0.1:5173${p.url}`, { waitUntil: 'networkidle' })
      await page.waitForTimeout(500)
      await page.screenshot({
        path: path.join(outDir, `${p.key}-${vp.name}.png`),
        fullPage: true
      })
      await page.close()
    }
  }

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
