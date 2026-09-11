/**
 * M6-3 核心组件截图（实验室 + 首页接入）
 * 依赖：前端 http://127.0.0.1:5173
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-3-components')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const viewports = [
    { name: '375', width: 375, height: 812 },
    { name: '768', width: 768, height: 1024 },
    { name: '1200', width: 1200, height: 900 }
  ]

  for (const vp of viewports) {
    for (const theme of ['dark', 'light']) {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await page.addInitScript((t) => localStorage.setItem('gs-theme', t), theme)
      await page.goto('http://127.0.0.1:5173/dev/components', { waitUntil: 'networkidle' })
      await page.waitForTimeout(400)
      await page.screenshot({
        path: path.join(outDir, `lab-${theme}-${vp.name}.png`),
        fullPage: true
      })
      await page.close()
    }

    const home = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
    await home.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await home.goto('http://127.0.0.1:5173/', { waitUntil: 'networkidle' })
    await home.waitForTimeout(500)
    await home.screenshot({
      path: path.join(outDir, `home-wired-${vp.name}.png`),
      fullPage: true
    })
    await home.close()
  }

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
