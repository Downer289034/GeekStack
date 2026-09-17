# T-SEC-1-FE OAuth state 手动演练

> 契约：开发进度决策 21 · BE `T-SEC-1-BE` 已合 main

## 正常路径（登录 / 绑定）

1. 登录页点击「使用 GitHub 登录」或设置页「去绑定」
2. DevTools → Application → Session Storage：应出现 `oauth_csrf_state_github`（或 gitee），值 ≥32 位 hex
3. 完成平台授权 → 回调 `/oauth/callback/{provider}` → 首页或绑定成功
4. Network：`POST /api/v1/oauth/{PROVIDER}/code` 请求体含 `{ code, state }`；sessionStorage 中对应 key 已清除

## 篡改 state（FE 比对 — 截图 1）

1. 正常点击授权，在平台页 **不要** 立刻点同意；回到 DevTools
2. Application → Local Storage → `oauth_pending_github`（BE callback 写入后才有）
3. 编辑 JSON，将 `state` 末尾改 1 个字符（或整段替换）
4. 刷新 `/oauth/callback/github` 或等待轮询
5. **预期**：页面 `n-empty` 文案「登录校验失败（state 不匹配），请重新发起第三方登录」；无 token 写入；pending 已清空

## 篡改 state（仅 BE — 可选截图 2）

1. 正常走完授权，在转交 **前** 用 DevTools 把 sessionStorage 的 `oauth_csrf_state_*` 删掉，但保留 localStorage pending 原样
2. **预期**：同上 FE 拒绝（expected 缺失），不调用成功登录

3. 或：sessionStorage 与 pending.state 一致，但在 `POST .../code` 前用断点/重放把 body 里 `state` 改成 Redis 中不存在的 hex
4. **预期**：401 + toast「无效的 OAuth state」（或拦截器等价文案）

## 三绿（本地）

```bash
cd frontend
npx eslint src/api/oauth.ts src/views/auth/OAuthCallbackView.vue src/views/auth/LoginView.vue src/views/profile/SettingsView.vue
npx vue-tsc --noEmit
npm run build
```

截图建议存放：`docs/assets/t-sec-1-fe/`（state 不匹配 empty 态 + Network 401）
