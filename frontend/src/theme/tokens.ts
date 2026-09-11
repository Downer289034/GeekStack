import type { GlobalThemeOverrides } from 'naive-ui'
import { darkTheme, type GlobalTheme } from 'naive-ui'

/** 夜栈灯码主题名（与 html data-theme 一致） */
export type GsTheme = 'dark' | 'light'

/**
 * Naive UI / seemly 不支持 oklch() 字符串，themeOverrides 必须用 sRGB hex。
 * 下列 hex 由 tokens.css 中同名 OKLCH 转换，CSS 仍以 oklch 为单一视觉事实源。
 */
const hex = {
  ink950: '#05070d',
  ink900: '#090d15',
  ink800: '#131822',
  ink700: '#232933',
  ink500: '#646972',
  ink300: '#a1a5ab',
  ink100: '#e5e8ed',
  amber300: '#f2cb83',
  amber500: '#e8a127',
  amber600: '#d28500',
  /** Light semantic accent L64（≥3:1 UI + paper-900 压填色 ≥4.5） */
  accentLight: '#c57800',
  paper50: '#faf6f1',
  paper100: '#f2eee7',
  paper200: '#e2ddd5',
  paper300: '#c8c3bc',
  paper700: '#47413a',
  paper900: '#201911',
  success: '#54b66e',
  warning: '#e0af3b',
  error: '#e24947',
  /** Dark badge error-on L65 */
  errorOnDark: '#ed5350',
  info: '#49abd6',
  accentHoverLight: '#af5c00',
  text3Dark: '#767a82',
  text3Light: '#777068',
  successOnLight: '#005e26',
  warningOnLight: '#845100',
  errorOnLight: '#a5292b',
  infoOnLight: '#005e82'
} as const

const fontBody =
  "-apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif"

/** 按主题构建 Naive overrides（映射 semantic → hex） */
export function buildNaiveOverrides(theme: GsTheme): GlobalThemeOverrides {
  const dark = theme === 'dark'
  const accent = dark ? hex.amber500 : hex.accentLight
  const accentHover = dark ? hex.amber300 : hex.accentHoverLight
  const accentText = dark ? hex.ink950 : hex.paper900
  const bg = dark ? hex.ink950 : hex.paper50
  const surface1 = dark ? hex.ink900 : '#ffffff'
  const border = dark ? hex.ink700 : hex.paper200
  const text1 = dark ? hex.ink100 : hex.paper900
  const text2 = dark ? hex.ink300 : hex.paper700
  const text3 = dark ? hex.text3Dark : hex.text3Light

  return {
    common: {
      primaryColor: accent,
      primaryColorHover: accentHover,
      primaryColorPressed: dark ? hex.amber600 : hex.accentHoverLight,
      primaryColorSuppl: accentHover,
      successColor: hex.success,
      warningColor: hex.warning,
      errorColor: hex.error,
      infoColor: hex.info,
      borderColor: border,
      dividerColor: border,
      bodyColor: bg,
      cardColor: surface1,
      modalColor: surface1,
      popoverColor: surface1,
      tableColor: surface1,
      inputColor: surface1,
      actionColor: dark ? hex.ink800 : hex.paper100,
      textColorBase: text1,
      textColor1: text1,
      textColor2: text2,
      textColor3: text3,
      borderRadius: '10px',
      borderRadiusSmall: '6px',
      fontSize: '16px',
      fontFamily: fontBody
    },
    Button: {
      textColorPrimary: accentText,
      textColorHoverPrimary: accentText,
      textColorPressedPrimary: accentText,
      textColorFocusPrimary: accentText
    },
    Tag: {
      textColorSuccess: dark ? hex.success : hex.successOnLight,
      textColorWarning: dark ? hex.warning : hex.warningOnLight,
      textColorError: dark ? hex.errorOnDark : hex.errorOnLight,
      textColorInfo: dark ? hex.info : hex.infoOnLight
    }
  }
}

/** Naive 内置暗色主题（light 时传 null） */
export function resolveNaiveTheme(theme: GsTheme): GlobalTheme | null {
  return theme === 'dark' ? darkTheme : null
}

/** @deprecated 旧导出名兼容 */
export const themeTokens = {
  colorPrimary: hex.amber500,
  colorPrimaryHover: hex.amber300,
  colorPrimaryPressed: hex.amber600,
  colorOnPrimary: hex.ink950,
  colorSuccess: hex.success,
  colorWarning: hex.warning,
  colorError: hex.error,
  colorBgLayout: hex.ink950,
  colorBgContainer: hex.ink900,
  colorBorder: hex.ink700,
  colorText: hex.ink100,
  colorTextSecondary: hex.ink300,
  borderRadius: '10px',
  borderRadiusSmall: '6px',
  fontSize: '16px',
  fontFamily: fontBody
} as const

/** @deprecated 请用 buildNaiveOverrides(theme) */
export const naiveThemeOverrides = buildNaiveOverrides('dark')
