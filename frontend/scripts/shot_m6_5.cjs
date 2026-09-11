/**
 * M6-5 骨架屏 / 实验室截图
 * 依赖：前端 http://127.0.0.1:5173
 * 骨架态：拦截 /api/v1 延迟，固定住 Loading
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-5-motion')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const viewports = [
    { name: '375', width: 375, height: 812 },
    { name: '768', width: 768, height: 1024 },
    { name: '1200', width: 1200, height: 900 }
  ]

  // —— 骨架屏（API 挂起）——
  const skeletonRoutes = [
    { name: 'skel-home', path: '/' },
    { name: 'skel-shares', path: '/shares' },
    { name: 'skel-forum', path: '/forum' }
  ]

  for (const route of skeletonRoutes) {
    for (const vp of viewports) {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
      await page.route('**/api/v1/**', async (r) => {
        await new Promise((resolve) => setTimeout(resolve, 60_000))
        await r.abort()
      })
      await page.goto(`http://127.0.0.1:5173${route.path}`, {
        waitUntil: 'domcontentloaded',
        timeout: 45000
      })
      await page.waitForTimeout(700)
      await page.screenshot({
        path: path.join(outDir, `${route.name}-${vp.name}.png`),
        fullPage: true
      })
      await page.close()
      console.log('wrote', route.name, vp.name)
    }
  }

  // —— 实验室（含骨架样例 + reveal 演示）——
  for (const vp of viewports) {
    const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/dev/components', {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    })
    await page.waitForTimeout(600)
    await page.screenshot({
      path: path.join(outDir, `lab-${vp.name}.png`),
      fullPage: true
    })
    await page.close()
    console.log('wrote lab', vp.name)
  }

  // —— 焦点环：键盘 Tab 到「写文章」后截一帧 ——
  {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/', {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    })
    await page.waitForTimeout(800)
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab')
    }
    await page.waitForTimeout(200)
    await page.screenshot({
      path: path.join(outDir, 'focus-ring-1200.png'),
      fullPage: false
    })
    await page.close()
    console.log('wrote focus-ring-1200')
  }

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
