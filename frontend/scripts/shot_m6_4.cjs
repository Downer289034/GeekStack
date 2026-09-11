/**
 * M6-4 页面迁移截图（三断点 · 暗色）
 * 依赖：前端 http://127.0.0.1:5173 ；后端可选（无数据时空态亦可验收）
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m6-4-pages')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const viewports = [
    { name: '375', width: 375, height: 812 },
    { name: '768', width: 768, height: 1024 },
    { name: '1200', width: 1200, height: 900 }
  ]

  const routes = [
    { name: 'shares', path: '/shares' },
    { name: 'forum', path: '/forum' },
    { name: 'search', path: '/search' },
    { name: 'tags', path: '/tags' },
    { name: '404', path: '/this-page-does-not-exist-m64' },
    { name: 'lab', path: '/dev/components' }
  ]

  // 尝试解析一条详情 id（后端可用时）
  let postId = null
  let shareId = null
  try {
    const posts = await fetch('http://127.0.0.1:8080/api/v1/posts?current=1&size=1').then((r) =>
      r.json()
    )
    postId = posts?.data?.records?.[0]?.id ?? null
  } catch {
    /* empty */
  }
  try {
    const shares = await fetch('http://127.0.0.1:8080/api/v1/shares?current=1&size=1').then((r) =>
      r.json()
    )
    shareId = shares?.data?.records?.[0]?.id ?? null
  } catch {
    /* empty */
  }
  if (postId) routes.push({ name: 'post-detail', path: `/forum/posts/${postId}` })
  if (shareId) routes.push({ name: 'share-detail', path: `/shares/${shareId}` })

  for (const route of routes) {
    for (const vp of viewports) {
      const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } })
      await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
      await page.goto(`http://127.0.0.1:5173${route.path}`, {
        waitUntil: 'domcontentloaded',
        timeout: 45000
      })
      await page.waitForTimeout(800)
      await page.screenshot({
        path: path.join(outDir, `${route.name}-${vp.name}.png`),
        fullPage: true
      })
      await page.close()
      console.log('wrote', route.name, vp.name)
    }
  }

  console.log('SHOT_OK', outDir)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
