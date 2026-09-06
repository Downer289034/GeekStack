import type { GlobalThemeOverrides } from 'naive-ui'

/**
 * 鸿蒙设计风主题 token（HarmonyOS Design）
 * 整站换肤只需调整本文件 + tokens.css，不触碰任何组件。
 */
export const themeTokens = {
  /** 品牌色：华为蓝 */
  colorPrimary: '#0a59f7',
  colorPrimaryHover: '#3178ff',
  colorPrimaryPressed: '#0540b4',
  colorSuccess: '#34c724',
  colorWarning: '#ff9500',
  colorError: '#fa383e',
  /** 页面底色 / 卡片底色 */
  colorBgLayout: '#f1f3f5',
  colorBgContainer: '#ffffff',
  /** 文字 */
  colorText: '#182431',
  colorTextSecondary: '#5e6772',
  /** 鸿蒙标志性大圆角与字体 */
  borderRadius: '12px',
  borderRadiusSmall: '8px',
  fontSize: '14px',
  fontFamily: "'HarmonyOS Sans SC', 'PingFang SC', 'Microsoft YaHei', 'Segoe UI', sans-serif"
} as const

/** 派生：Naive UI 主题覆盖（common 级全局生效） */
export const naiveThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: themeTokens.colorPrimary,
    primaryColorHover: themeTokens.colorPrimaryHover,
    primaryColorPressed: themeTokens.colorPrimaryPressed,
    primaryColorSuppl: themeTokens.colorPrimaryHover,
    successColor: themeTokens.colorSuccess,
    warningColor: themeTokens.colorWarning,
    errorColor: themeTokens.colorError,
    bodyColor: themeTokens.colorBgLayout,
    cardColor: themeTokens.colorBgContainer,
    textColorBase: themeTokens.colorText,
    textColor1: themeTokens.colorText,
    textColor2: themeTokens.colorText,
    textColor3: themeTokens.colorTextSecondary,
    borderRadius: themeTokens.borderRadius,
    borderRadiusSmall: themeTokens.borderRadiusSmall,
    fontSize: themeTokens.fontSize,
    fontFamily: themeTokens.fontFamily
  }
}
