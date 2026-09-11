/**
 * M6-6 红线巡检取证：触控目标 / Tab 焦点 / 200% 缩放
 * 依赖：http://127.0.0.1:5173
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-6-a11y')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()

  // —— 375 触控目标抽查 ——
  {
    const page = await browser.newPage({ viewport: { width: 375, height: 812 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/', { waitUntil: 'domcontentloaded', timeout: 45000 })
    await page.waitForTimeout(800)

    const measures = await page.evaluate(() => {
      const pick = (sel) => {
        const el = document.querySelector(sel)
        if (!el) return null
        const r = el.getBoundingClientRect()
        return {
          sel,
          w: Math.round(r.width),
          h: Math.round(r.height),
          ok: r.width >= 44 && r.height >= 44
        }
      }
      return [
        pick('.gs-theme-toggle'),
        pick('.gs-btn--primary'),
        pick('.gs-text-btn'),
        pick('a.gs-logo')
      ].filter(Boolean)
    })
    fs.writeFileSync(path.join(outDir, 'touch-375.json'), JSON.stringify(measures, null, 2))
    await page.screenshot({ path: path.join(outDir, 'touch-home-375.png'), fullPage: false })
    console.log('touch', measures)
    await page.close()
  }

  // —— Tab 焦点环分帧（首页）——
  {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/', { waitUntil: 'domcontentloaded', timeout: 45000 })
    await page.waitForTimeout(600)
    for (let i = 1; i <= 8; i++) {
      await page.keyboard.press('Tab')
      await page.waitForTimeout(120)
      await page.screenshot({
        path: path.join(outDir, `tab-home-${String(i).padStart(2, '0')}.png`),
        fullPage: false
      })
    }
    const active = await page.evaluate(() => {
      const el = document.activeElement
      return el
        ? {
            tag: el.tagName,
            className: el.className,
            aria: el.getAttribute('aria-label'),
            text: (el.textContent || '').trim().slice(0, 40)
          }
        : null
    })
    fs.writeFileSync(path.join(outDir, 'tab-home-active.json'), JSON.stringify(active, null, 2))
    console.log('tab-active', active)
    await page.close()
  }

  // —— 200% 缩放（CSS zoom 近似，viewport 不变）——
  const zoomRoutes = [
    { name: 'home', path: '/' },
    { name: 'forum', path: '/forum' },
    { name: 'shares', path: '/shares' }
  ]
  for (const route of zoomRoutes) {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto(`http://127.0.0.1:5173${route.path}`, {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    })
    await page.waitForTimeout(500)
    await page.evaluate(() => {
      document.documentElement.style.zoom = '200%'
    })
    await page.waitForTimeout(400)
    await page.screenshot({
      path: path.join(outDir, `zoom200-${route.name}.png`),
      fullPage: false
    })
    await page.close()
    console.log('zoom200', route.name)
  }

  // —— aria / role 快速 DOM 核对 ——
  {
    const page = await browser.newPage({ viewport: { width: 1200, height: 900 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/dev/components', {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    })
    await page.waitForTimeout(500)
    const aria = await page.evaluate(() => {
      const tabs = [...document.querySelectorAll('[role="tablist"]')].map((list) => ({
        label: list.getAttribute('aria-label'),
        tabs: [...list.querySelectorAll('[role="tab"]')].map((t) => ({
          text: (t.textContent || '').trim(),
          selected: t.getAttribute('aria-selected')
        }))
      }))
      const iconBtns = [...document.querySelectorAll('button')].filter((b) => {
        const hasText = (b.textContent || '').trim().length > 0
        return !hasText
      })
      return {
        tablists: tabs,
        iconButtonsWithoutLabel: iconBtns.map((b) => ({
          className: b.className,
          aria: b.getAttribute('aria-label')
        }))
      }
    })
    fs.writeFileSync(path.join(outDir, 'aria-lab.json'), JSON.stringify(aria, null, 2))
    console.log('aria', JSON.stringify(aria, null, 2))
    await page.close()
  }

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
