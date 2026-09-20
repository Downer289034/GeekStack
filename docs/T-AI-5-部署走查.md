# T-AI-5：部署与走查

> 日期：2026-09-20 ｜ M13 收官 ｜ 后端主导 compose / CI，前端配合浮窗全链走查

## 1. 目标

把 AI 助手从「本机 uvicorn」接到开发/生产 compose：`qdrant` + `ai-service`，Java 写向量、Python 检索生成，网关 `POST /api/v1/ai/ask` 透传 SSE。

## 2. 一键起（开发栈）

```bash
# 仓库根；需能 SSH 到 172.25.41.137，且 /opt/community-dev/.env 已含：
# MIMO_API_KEY / DASHSCOPE_API_KEY（真实值勿入库）
# Windows：可用 deploy/dev/merge-ai-env.sh 合并本机根 .env 的 AI 键（不打印值）
bash deploy/dev/sync-ai-stack.sh
```

本机根目录 `.env`（对照 `.env.example`）关键项：

| 变量 | 开发栈建议值 |
|------|----------------|
| `AI_SERVICE_URL` | `http://172.25.41.137:8090` |
| `QDRANT_URL` | `http://172.25.41.137:6333`（**勿**在同一 shell 留 `QDRANT_URL=:memory:`，会被进程环境盖住；Java 已优先选 HTTP） |
| `QDRANT_COLLECTION` | `geekstack_community`（与 ai-service 同名） |
| `DASHSCOPE_API_KEY` | 与 Java `community.rag` / ai-service 同源 |
| `EMBEDDING_MODEL` | `text-embedding-v3` |

健康探针：

```bash
curl -sf http://172.25.41.137:8090/health
curl -sf http://172.25.41.137:6333/readyz
```

## 3. 生产栈跟随

1. 服务器 `/opt/community/.env` 补齐与开发栈相同的 AI 占位键（Key 真实值）。
2. **显式**同步 compose：`scp deploy/prod/docker-compose.yml root@172.25.41.137:/opt/community/`（`deploy.sh` 默认不覆盖 compose，避免误伤）。
3. `bash deploy/prod/deploy.sh`：同 commit 部署 jar + dist，并在已入栈时重建 `ai-service`。
4. backend 环境变量：`AI_SERVICE_URL=http://ai-service:8090`、`QDRANT_URL=http://qdrant:6333`（compose 已写死内网名）。
5. **不**对 backend 硬依赖 ai-service：上游宕机 → 网关 503，主站仍可起。

`health-check.sh`：若容器名 `geekstack-ai-service` 存在，则要求 `/health` UP，否则告警 `ai_service_down`。

## 4. 全链路走查清单（发帖 → 索引 → 提问命中）

| # | 步骤 | 期望 | 角色 |
|---|------|------|------|
| 1 | `sync-ai-stack.sh` 后 `docker compose ps` | `community-qdrant` / `community-ai-service` Up | 后端 |
| 2 | 本机 backend：`QDRANT_URL` 指向 6333，`DASHSCOPE_API_KEY` 非空 | 启动日志 `RAG vector store=http://...6333` | 后端 |
| 3 | （可选）`RAG_SEED_HELP=true` 重启一次 | 帮助语料入 `geekstack_community` | 后端 |
| 4 | 登录发帖：标题含唯一标记词（如 `M13WALK`），正文含可检索句 | 201/200；后台 `RAG upsert` | 前端+后端 |
| 5 | Qdrant：`GET /collections/geekstack_community` points_count ≥ 1 | 索引写入成功 | 后端 |
| 6 | 浮窗 / 网关提问：问刚发帖内容 | SSE `token`/`sources`/`done`；来源 URL 含该帖 | 前端 |
| 7 | 未登录点浮窗 | 引导登录 | 前端 |
| 8 | 停 `ai-service` 再问 | JSON/文案「AI 助手暂时不可用…」（503） | 前后端 |
| 9 | 恢复服务后连续超限提问 | 「提问过于频繁…」（429） | 前后端 |

## 5. CI

`.github/workflows/ci.yml` 新增 job **`ai-service`**：Python 3.12 + `python -m unittest tests.test_retrieve -v`（无 Key、`:memory:`）。

## 6. 本轮验证记录（2026-09-20）

| 项 | 结果 |
|----|------|
| compose 增 qdrant + ai-service（dev + prod） | ✅ |
| `ai-service/Dockerfile` + `.dockerignore` | ✅ |
| 根 / ai-service `.env.example` 对齐 `text-embedding-v3` + `geekstack_community` | ✅ |
| CI `ai-service` job | ✅ workflow 已改 |
| `python -m unittest tests.test_retrieve` | ✅ 4/4 |
| 开发栈拉起 | ✅ `community-qdrant` + `community-ai-service`；`/health` + `/readyz` 200 |
| 发帖→索引→提问命中 | ✅ 见下方勾选 |
| 网关 SSE | ✅ 修复 Java `HttpClient` 默认 h2c Upgrade → uvicorn 422；强制 **HTTP/1.1** |

### 服务器 / 联调勾选

- [x] 开发栈 `qdrant` + `ai-service` Up，`/health` + `/readyz` 200
- [x] 本机 backend 写 Qdrant（日志 `RAG vector store=http://172.25.41.137:6333`），发帖 `jkz_post_3106` 后 `points_count=1`
- [x] 直连 `POST :8090/v1/ask` retrieval=ok，来源含 `/posts/3106`
- [x] 网关 `POST :8081/api/v1/ai/ask` HTTP 200，SSE 含 `token`/`sources`/`done` 且命中 `/posts/3106`
- [ ] 浮窗 UI 三断点目视（前端配合，依赖已关闭的 T-AI-4）
- [ ] 生产栈显式同步 compose 后 `deploy.sh`（跟随上线，不阻塞本任务关闭）

### 关单复跑（2026-09-20 16:00，开发 A）

| 项 | 结果 |
|----|------|
| `GET :8090/health` | 200 `status=UP` phase=T-AI-5 |
| `GET :6333/readyz` | 200 |
| `GET :6333/collections/geekstack_community` | `points_count=1` |
| `POST :8090/v1/ask` | 200 `retrieval=ok`，来源含 `/posts/3106`（score≈0.72） |
| ai-service unittest | 4/4 |
| backend 聚焦测 | `AiAskGateway`+`RagIndex`+`MarkdownChunker` 8/8 BUILD SUCCESS |
| 证据目录 | `docs/assets/t-ai-5/`（脱敏，无 Key） |

## 7. 交接补丁（摘录）

- 开发栈端口新增：**6333**（Qdrant）、**8090**（ai-service）。
- collection **必须**两边同名：`geekstack_community`。
- 旧 spike 默认 `geekstack_rag_spike` 已废弃；勿混用。
- 独立 `ai-service/docker-compose.yml` 仅本地单起 Qdrant；联调以 `deploy/dev` 为准。
- 网关客户端必须 **HTTP/1.1**（见 `AiGatewayClient`），否则 uvicorn 对 Upgrade 请求返回 422，网关误报 503。
- 若 shell 曾跑过 ai-service，请 `Remove-Item Env:QDRANT_URL` 或设为 HTTP，避免 `:memory:` 污染 Java 进程。
