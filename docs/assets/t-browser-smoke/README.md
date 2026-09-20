# 浏览器人工回归包（OAuth + 登录页视差）

> 日期：2026-09-20 ｜ 开发 B ｜ 工作区 `2026-09-06-18-49-57`

## 逐条结果

| # | 项 | 状态 | 说明 | 证据 |
|---|----|------|------|------|
| 1 | 登录页入口/品牌/OAuth 按钮 | ✅ 通过 | title=登录 / 注册 · 极客栈 GeekStack; brand=true; oauthBtns=2; login-bg=1 | `01-login-entry.png` |
| 2 | 视差手感 + 粒子不抢输入 | ✅ 通过 | transformBefore=matrix(1.2, 0, 0, 1.2, 0, 0); transformAfter=matrix(1.2, 0, 0, 1.2, -7.41, -5.77); moved=true; inputFocus=true | `02-login-parallax.png` |
| 3 | prefers-reduced-motion 静态壳 | ✅ 通过 | is-static=1; transformStable=true; transform=matrix(1.2, 0, 0, 1.2, 0, 0) | `03-login-reduced-motion.png` |
| 4 | 触屏/移动端静态壳可点表单 | ✅ 通过 | is-static=1; inputFocus=true; viewport=390x844 | `04-login-touch.png` |
| 5 | GitHub OAuth 入口→授权页 | ✅ 通过 | 已达 GitHub IdP（未登录会话落在 login，return_to 含 authorize）。完整授权/回调需人工账号。url=https://github.com/login?client_id=Ov23licTKtimIZiNJYLJ&return_to=%2Flogin%2Foauth%2Fauthorize%3Fclient_id%3DOv23licTKtimIZiNJYLJ%26redirect | `05-oauth-github-authorize.png` |
| 6 | Gitee OAuth 入口→授权页 | ✅ 通过 | 已达 Gitee IdP（未登录会话落在 login，redirect 含 authorize）。完整授权/回调需人工账号。url=https://gitee.com/login?redirect_to_url=https%3A%2F%2Fgitee.com%2Foauth%2Fauthorize%3Fclient_id%3Dfbe67cc7dd611157126ed5f8cad0082b91d73ac65b | `06-oauth-gitee-authorize.png` |

## 范围说明

- OAuth：**密钥已配置**，本包验证到 IdP 授权页（302→GitHub/Gitee）。完整「点授权→回调换票」需人工用真实第三方账号在浏览器完成，本环境未代登。
- 登录页 v2：视差位移、品牌可见、reduced-motion/触屏降级、输入框可聚焦（粒子不抢交互）。
- 未改视觉大样式；发现的问题记在下方。

## 问题清单

- （无）

## 重跑

```bash
node frontend/scripts/t-browser-smoke-capture.mjs
```
