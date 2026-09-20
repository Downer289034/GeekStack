# T-AI-5 关单证据（2026-09-20）

> 后端关单复跑（开发 A）+ 浮窗全链走查（开发 B）并存。勿删任一侧证据。

---

## 一、后端关单复跑（开发 A）

### 确认项
- [x] `deploy/dev/docker-compose.yml` / `deploy/prod/docker-compose.yml` 含 `qdrant` + `ai-service`
- [x] `.github/workflows/ci.yml` 含 `ai-service` job（Python 3.12 + `tests.test_retrieve`）
- [x] 开发栈拉起：`/health` 200、`/readyz` 200、`geekstack_community.points_count=1`
- [x] 直连 ask：`retrieval=ok`，来源含 `/posts/3106`
- [x] 本地：ai-service unittest 4/4；backend 聚焦测 8/8

### 原始探针（脱敏）
见 `rerun-probe.json`（无 Key / 无模型原文回答）。

### 不阻塞项（后端口径）
- ~~浮窗 UI 三断点目视~~ → 见下方「二、前端浮窗全链」覆盖
- 生产栈 `deploy.sh` 跟随上线决策

### CI（后端关单时）
- Run：https://github.com/Downer289034/GeekStack/actions/runs/35498637125（`bf4b72c`）
- Jobs：frontend / backend / ai-service 三绿

---

## 二、前端浮窗全链走查（开发 B）

> 标记帖：`M13WALKFE1789892183978`（id=`3109`）｜真实 SSE，非 T-AI-4 路由 mock

### 五条路径

| # | 路径 | 证据 | 结果 |
|---|------|------|------|
| 1 | 发帖（唯一标记）→ 浮窗提问命中来源 | `01-post-ask-hit.png` + `_ask_raw.txt` | ✅ 来源含 `jkz_post_3109` / `/posts/3109` |
| 2 | 来源卡片展示/点击 | `02-source-click.png` | ✅ |
| 3 | 未登录引导 | `03-unlogged-hint.png` | ✅ 「登录后即可使用 AI 助手」+ 去登录 |
| 4 | 停 `community-ai-service` → 503 | `04-503.png` | ✅ 「AI 助手暂时不可用，请稍后再试」 |
| 5 | 超限 → 429 | `05-429.png` | ✅ 「提问过于频繁，请稍后再试」 |

结构化摘要：`walkthrough.json`。

### 联调拓扑
- 前端 Vite `:5173` → 本机 backend `:8081`
- backend → 开发栈 `ai-service :8090` + `Qdrant :6333`（`geekstack_community`）

### 前端修复（走查阻塞项）
仅 `Accept: text/event-stream` 时，网关 503/429 的 JSON 触发 `HttpMediaTypeNotAcceptableException`。  
已改为：`Accept: text/event-stream, application/json`（`frontend/src/api/aiAsk.ts`）。

### 重跑
```bash
node frontend/scripts/t-ai-5-walk-capture.mjs
# AI5_SKIP_HIT=1 可复用已有 01–03，仅补 503/429
```
注意：503 用例需配额未满账号（限流先于上游探测）；脚本对 503 使用新注册用户。
