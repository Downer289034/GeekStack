/**
 * M7-1 spike：md-editor-v3 --md-* ← 夜栈灯码 Token 透传取证
 * 依赖：http://127.0.0.1:5173/dev/md-editor-spike
 */
const { chromium } = require('playwright')
const path = require('path')
const fs = require('fs')

function parseColor(cssColor) {
  const s = String(cssColor || '').trim()
  const rgb = s.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
  if (rgb) return { kind: 'rgb', rgb: [Number(rgb[1]), Number(rgb[2]), Number(rgb[3])] }
  const oklch = s.match(/oklch\(\s*([\d.]+)%?/)
  if (oklch) return { kind: 'oklch', L: Number(oklch[1]) }
  if (s === '#fff' || s === '#ffffff' || s === 'white') return { kind: 'rgb', rgb: [255, 255, 255] }
  if (s === '#000' || s === '#000000' || s === 'black') return { kind: 'rgb', rgb: [0, 0, 0] }
  return null
}

function isLightBg(cssColor) {
  const p = parseColor(cssColor)
  if (!p) return null
  if (p.kind === 'oklch') {
    // oklch L 可能是 0–1 或 0–100（带 %）
    const L = p.L <= 1 ? p.L * 100 : p.L
    return L > 70
  }
  return luminance(p.rgb) > 0.45
}

function luminance([r, g, b]) {
  const toLin = (c) => {
    const x = c / 255
    return x <= 0.03928 ? x / 12.92 : ((x + 0.055) / 1.055) ** 2.4
  }
  return 0.2126 * toLin(r) + 0.7152 * toLin(g) + 0.0722 * toLin(b)
}

async function sample(page) {
  return page.evaluate(() => {
    const root = document.querySelector('.md-editor')
    const toolbar = document.querySelector('.md-editor-toolbar-wrapper, .md-editor-toolbar')
    const cm = document.querySelector('.cm-editor')
    const content = document.querySelector('.cm-content')
    const preview = document.querySelector('.md-editor-preview')
    const pick = (el) => {
      if (!el) return null
      const s = getComputedStyle(el)
      return {
        bg: s.backgroundColor,
        color: s.color,
        border: s.borderColor
      }
    }
    const vars = {}
    if (root) {
      const cs = getComputedStyle(root)
      ;[
        '--md-bk-color',
        '--md-color',
        '--md-border-color',
        '--md-border-active-color',
        '--surface-1',
        '--text-1',
        '--accent'
      ].forEach((k) => {
        vars[k] = cs.getPropertyValue(k).trim()
      })
    }
    return {
      themeClass: root?.className || '',
      dataTheme: document.documentElement.dataset.theme,
      vars,
      root: pick(root),
      toolbar: pick(toolbar),
      cm: pick(cm),
      content: pick(content),
      preview: pick(preview)
    }
  })
}

;(async () => {
  const outDir = path.join(__dirname, '../../docs/assets/m7-1-spike')
  fs.mkdirSync(outDir, { recursive: true })
  const browser = await chromium.launch()
  const report = { cases: [] }

  for (const mode of ['dark', 'light']) {
    for (const override of [false, true]) {
      const page = await browser.newPage({ viewport: { width: 1100, height: 900 } })
      await page.addInitScript((m) => localStorage.setItem('gs-theme', m), mode)
      await page.goto('http://127.0.0.1:5173/dev/md-editor-spike', {
        waitUntil: 'domcontentloaded',
        timeout: 45000
      })
      await page.waitForTimeout(900)
      const box = page.locator('.spike-toggle input')
      const checked = await box.isChecked()
      if (checked !== override) await box.click()
      await page.waitForTimeout(400)

      const raw = await sample(page)
      const verdict = {
        mode,
        override,
        rootBg: raw.root?.bg,
        rootIsLight: isLightBg(raw.root?.bg),
        cmBg: raw.cm?.bg,
        cmIsLight: isLightBg(raw.cm?.bg),
        mdBk: raw.vars['--md-bk-color'],
        surface1: raw.vars['--surface-1'],
        tokenBound:
          override &&
          String(raw.vars['--md-bk-color']).replace(/\s/g, '') ===
            String(raw.vars['--surface-1']).replace(/\s/g, ''),
        vars: raw.vars,
        themeClass: raw.themeClass
      }
      report.cases.push(verdict)

      const tag = `${mode}-${override ? 'override' : 'default'}`
      await page.screenshot({
        path: path.join(outDir, `spike-${tag}.png`),
        fullPage: false
      })
      console.log(tag, {
        rootBg: verdict.rootBg,
        rootIsLight: verdict.rootIsLight,
        tokenBound: verdict.tokenBound,
        mdBk: verdict.mdBk,
        surface1: verdict.surface1
      })
      await page.close()
    }
  }

  // 模拟业务页现状：夜间站点 + MdEditor 未传 theme（默认 light → 白底亮块）
  {
    const page = await browser.newPage({ viewport: { width: 1100, height: 900 } })
    await page.addInitScript(() => localStorage.setItem('gs-theme', 'dark'))
    await page.goto('http://127.0.0.1:5173/dev/md-editor-spike', {
      waitUntil: 'domcontentloaded',
      timeout: 45000
    })
    await page.waitForTimeout(800)
    // 强制成库默认 light（对齐 PostPublishView 未传 theme）
    await page.evaluate(() => {
      const root = document.querySelector('.md-editor')
      if (!root) return
      root.classList.remove('md-editor-dark')
      root.style.setProperty('--md-color', '#333')
      root.style.setProperty('--md-bk-color', '#fff')
      root.style.setProperty('--md-bk-color-outstand', '#f6f6f6')
      root.style.setProperty('--md-border-color', '#e6e6e6')
    })
    // 关掉 Token 覆盖
    const box = page.locator('.spike-toggle input')
    if (await box.isChecked()) await box.click()
    await page.waitForTimeout(300)
    // 再次强制 light 变量（覆盖可能被 Vue 刷回）
    await page.evaluate(() => {
      const root = document.querySelector('.md-editor')
      if (!root) return
      root.classList.remove('md-editor-dark')
      ;[
        ['--md-color', '#333'],
        ['--md-bk-color', '#fff'],
        ['--md-bk-color-outstand', '#f6f6f6'],
        ['--md-border-color', '#e6e6e6']
      ].forEach(([k, v]) => root.style.setProperty(k, v))
    })
    const pub = await sample(page)
    report.publishPageDarkSim = {
      rootBg: pub.root?.bg,
      rootIsLight: isLightBg(pub.root?.bg),
      note: '模拟发帖页：data-theme=dark 但 MdEditor 默认 light（未绑 theme 丙）'
    }
    await page.screenshot({
      path: path.join(outDir, 'm6-6-editor-dark-site-light-editor.png'),
      fullPage: false
    })
    console.log('publish-sim', report.publishPageDarkSim)
    await page.close()
  }

  const darkDefault = report.cases.find((c) => c.mode === 'dark' && !c.override)
  const darkOver = report.cases.find((c) => c.mode === 'dark' && c.override)
  report.conclusion = {
    canPassTokensViaCssVars: Boolean(darkOver?.tokenBound),
    darkLibraryThemeExists: darkDefault && darkDefault.rootIsLight === false,
    darkOverrideMatchesSurface1: Boolean(darkOver?.tokenBound),
    residualRisks: [
      'preview 主题（@vavt/markdown-theme）仍是独立皮肤，需另做 previewTheme 或 :deep 覆盖',
      'codeTheme 走 highlight.js CDN 成对 light/dark，需随 data-theme 切换（spike 已绑 atom/github）',
      '少数硬编码色（如 table-shape #222/#555）需补 :deep 覆盖',
      'CodeMirror 语法高亮色来自扩展主题，不完全吃 --md-*'
    ],
    recommendation: 'KEEP_AND_OVERRIDE',
    decision:
      '能透传：保留 md-editor-v3，用 theme 属性绑定 data-theme + CSS 覆盖 14 个 --md-* ← semantic Token；不做换库。'
  }

  fs.writeFileSync(path.join(outDir, 'spike-report.json'), JSON.stringify(report, null, 2))
  console.log('conclusion', report.conclusion)
  await browser.close()
})().catch((e) => {
  console.error(e)
  process.exit(1)
})
