# T-DESIGN-5 视觉改造执行规范（无设计师版）

> **【已废弃 · M6】** 2026-09-11 起视觉规范切换为《[geekstack-ui-design-spec.md](./geekstack-ui-design-spec.md)》（夜栈灯码 v1.1）。  
> 本文件仅作 M5 历史档案；**三分区色（forum/share/notice）体系标记废弃**（架构经验保留，实现被替代）。  
> **T-DESIGN-4（暗色主题）已关闭**——能力由 M6-1 `data-theme` + Naive `darkTheme` 完整覆盖。

> 背景：UI 设计师调离，本规范由 PM 出，**代替设计稿**。前端照规范实施，实施后截图交 PM 评审。
> 原则：只扩 `tokens.ts` / `tokens.css` + 页面样式层；组件结构改动仍需报批；WCAG AA 标准不降。

## 一、色板扩展（tokens.ts 纯加法）

```ts
// 内容分区色：让"帖子/项目/公告"在视觉上可区分（用作图标底、区块标题色条、标签底）
colorForum: '#7a5af8',      // 紫 —— 论坛/帖子（文字变体 #5b3fd1，着色底 4.6:1 量级，实施时按 wcag_verify.py 实测校正）
colorShare: '#0e9384',      // 青 —— 项目分享（文字变体 #0a6e63）
colorNotice: '#e06616',     // 橙 —— 通知/公告（文字变体 #a54a0f）
```

**规则**：每个新色必须成对出现（基色 + 文字变体），文字变体经 `wcag_verify.py` 实测 ≥4.5:1 后才可提交——沿用既有验收方式。

## 二、Hero 渐变横幅（首页最大观感提升点）

首页顶部新增横幅区（约 180px 高，组件结构改动已预批）：

```css
background: linear-gradient(135deg, #0540b4 0%, #0a59f7 45%, #2666f0 100%);
border-radius: 12px;  /* 沿用品牌大圆角 */
```

- 内容：品牌 Slogan「代码江湖的落脚点」+ 一句副文案 + 两个快捷按钮（白底品牌字"发布项目" / 描边白字"逛论坛"）
- 白字压此渐变最深处 #0540b4 对比度 8.75:1，中段 #0a59f7 5.55:1，浅端校正为 #2666f0 4.95:1（原稿 #3178ff 实测仅 3.99:1，已迭代）
- 右侧可放淡装饰图形（纯 CSS 圆形/网格，opacity ≤0.15，禁止外链图片）

## 三、卡片层次（三档阴影入 tokens.css）

```css
--shadow-sm: 0 1px 2px rgba(24, 36, 49, 0.06);
--shadow-md: 0 4px 12px rgba(24, 36, 49, 0.08);
--shadow-lg: 0 8px 24px rgba(24, 36, 49, 0.12);
```

- 列表卡片默认 `--shadow-sm`，**hover 升 `--shadow-md` + `translateY(-2px)`，transition 0.2s ease**
- 详情页主卡片用 `--shadow-md`；弹层/抽屉由 Naive UI 自带
- 卡片圆角统一 12px（已在 Token），杜绝页面里散落的 4px/8px

## 四、语义色标签用起来（老板痛点直接解药）

现有 success/warning/error/forum/share/notice 文字变体，按此映射接线：

| 内容 | 标签样式 |
|------|----------|
| 帖子分类（问答/分享/讨论） | forum 紫系浅底 |
| 项目分享卡 | share 青系浅底 |
| 置顶/公告 | warning 橙系 |
| 已解决/已下架 | error 红系 |
| 热榜前三 | error 底白字徽标（1/2/3） |

实现统一走 Naive UI `n-tag` 主题接线（T-DESIGN-2 已建 `textColor*` 模式，照抄扩展）。

## 五、图标体系

- 引入 `@vicons/ionicons5`（Naive UI 官方配套，零风格冲突）
- 最低要求：主导航四项、空状态（配 `n-empty` + 引导按钮"去发布"）、区块标题左侧小图标、按钮内操作图标（发布/搜索/点赞）
- 图标色随文字色，不单独设色

## 六、实施顺序（每步可独立提交）

1. **色板 + 阴影 Token 扩展**（tokens.ts/css，半小时）
2. **首页 Hero + 区块化改造**（观感提升最大，优先）
3. **广场卡片升级**（阴影/hover/语义标签/封面占位）
4. **详情页与空状态**（图标 + n-empty）
5. 每步完成：lint / vue-tsc / build 三绿 + 改前改后截图

## 七、验收标准（PM 评审口径）

- 三断点（375/768/1280）截图：无溢出、hero 渐变完整、标签色符合映射表
- 新增色对全部过 `wcag_verify.py`（≥4.5:1）
- 全站仅存在 Token 定义的颜色字面量，页面内零硬编码色值（grep 审查）
- 三绿 + 无新增 chunk 体积异常
