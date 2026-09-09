import type { GlobalThemeOverrides } from 'naive-ui'

/**
 * 鸿蒙设计风主题 token（HarmonyOS Design）
 * 整站换肤只需调整本文件 + tokens.css，不触碰任何组件。
 */
export const themeTokens = {
  /** 品牌色：华为蓝 */
  colorPrimary: '#0a59f7',
  /** hover 取 #1467e8：白字压底 / 作文字均 5.08:1（原 #3178ff 仅 3.99:1，WCAG AA 不达标） */
  colorPrimaryHover: '#1467e8',
  colorPrimaryPressed: '#0540b4',
  /**
   * Hero 渐变浅端（T-DESIGN-5）：规范原稿 #3178ff 白字仅 3.99:1（不达标）。
   * 校正为 #2666f0（白字 4.95:1）。渐变：pressed → primary → soft。
   */
  colorPrimarySoft: '#2666f0',
  /**
   * 品牌色上的前景字/图标（P2-17）：主按钮、选中 chip、热榜徽标等「压在 primary 上」的白。
   * 与 colorBgContainer 同值但语义独立——容器底可变灰，on-primary 仍保持品牌上的对比白。
   */
  colorOnPrimary: '#ffffff',
  colorSuccess: '#34c724',
  colorWarning: '#ff9500',
  colorError: '#d73035',
  /**
   * 语义色「文字变体」：供标签文字等小字号彩色文本使用。
   * 基色（上）仅作底色/图标；文字一律用变体，保证着色标签底上 ≥4.5:1。
   * 实测（标签着色底 = 基色 12% 透明度叠白）：success 4.61 / warning 4.62 / error 4.63。
   */
  colorSuccessText: '#217f17',
  colorWarningText: '#a15e00',
  colorErrorText: '#c72d31',
  /**
   * 内容分区色（T-DESIGN-5）：基色作图标底/色条/标签底，文字一律用变体。
   * 实测（标签着色底 = 基色 12% 透明度叠白）：forum 5.85 / share 5.31 / notice 5.11。
   */
  colorForum: '#7a5af8',
  colorShare: '#0e9384',
  colorNotice: '#e06616',
  colorForumText: '#5b3fd1',
  colorShareText: '#0a6e63',
  colorNoticeText: '#a54a0f',
  /** 页面底色 / 卡片底色 */
  colorBgLayout: '#f1f3f5',
  colorBgContainer: '#ffffff',
  /** 边框 / 分隔线专用（不再借用页面底色 --color-bg-layout） */
  colorBorder: '#dfe3e8',
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
    errorColorHover: '#de454a',
    errorColorPressed: '#b7282d',
    errorColorSuppl: '#de454a',
    borderColor: themeTokens.colorBorder,
    dividerColor: themeTokens.colorBorder,
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
  },
  /**
   * 标签文字走语义色「文字变体」：naive-ui 默认用基色作标签文字，
   * 在着色标签底上对比度 2.2~3.7 不达标（T-DESIGN-1 F1-F3）。
   */
  Tag: {
    textColorSuccess: themeTokens.colorSuccessText,
    textColorWarning: themeTokens.colorWarningText,
    textColorError: themeTokens.colorErrorText
  }
}
