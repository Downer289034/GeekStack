# T-AI-5 关单证据（2026-09-20 复跑）

## 确认项
- [x] `deploy/dev/docker-compose.yml` / `deploy/prod/docker-compose.yml` 含 `qdrant` + `ai-service`
- [x] `.github/workflows/ci.yml` 含 `ai-service` job（Python 3.12 + `tests.test_retrieve`）
- [x] 开发栈健康：`/health` 200、`/readyz` 200、`geekstack_community.points_count=1`
- [x] 直连 ask：`retrieval=ok`，来源含 `/posts/3106`
- [x] 本地：ai-service unittest 4/4；backend 聚焦测 8/8

## 原始探针（脱敏）
见 `rerun-probe.json`（无 Key / 无完整模型回答正文）。

## 不阻塞关闭
- 浮窗 UI 三断点目视：依赖已关闭的 T-AI-4
- 生产栈 `deploy.sh`：跟随上线决策

## CI
- Run：https://github.com/Downer289034/GeekStack/actions/runs/35498637125（`bf4b72c`）
- Jobs：frontend ✓ 52s · backend ✓ 2m7s · ai-service ✓ 27s
