# T-AI-2：问答链路交付说明

> **状态**：实现完成，交 PM 独立复跑  
> **前置**：T-AI-0 / T-AI-1 已关闭  
> **原始评测**：[`docs/assets/t-ai-2/eval.json`](./assets/t-ai-2/eval.json)  
> **实测时间**：2026-09-20 10:32 +08

---

## 1. 链路

```
问题 → rewrite（MiMo，失败回退原问）→ embed → Qdrant top-k
     → 相似度阈值过滤（默认 0.35）
     → 分支：
         empty / below_threshold → 兜底话术（不调生成）
         ok → prompt 组装 → MiMo 生成（thinking=disabled）→ 来源列表
```

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/ask` | 同步 JSON（含 `retrieval` 分支字段） |
| POST | `/v1/ask/stream` | SSE：`token` / `sources` / `done` |

## 2. 验收锚点证据

| 项 | 证据 |
|----|------|
| 三分支单测 | `python -m unittest tests.test_retrieve -v` → 4 passed（empty / ok / below_threshold + SSE 兜底不调 LLM） |
| 10 问人工评测 | 见 §4；原始 JSON 见 assets |
| P95 延迟 | **wall P95 = 9735.7 ms**，avg = 6701.7 ms（评测跳过 rewrite 以控往返；线上 rewrite 另计） |

## 3. 配置（定稿沿用）

- 分块 / embedding / thinking：T-AI-0 拍板不变  
- `retrieve_top_k=5`，`retrieve_score_threshold=0.35`  
- 检索读 `parent_content_id` + `chunk_text`/`text`（对齐 T-AI-1 schema v1.1）

## 4. 10 问评测摘要

| # | 问题 | 分支 | wall_ms | 有 URL | 相关性（人工） | 主引用是否合理 |
|---|------|------|---------|--------|----------------|----------------|
| 1 | 改密后 refresh 是否立即失效 | ok | 8448 | ✓ | 正确（立即失效 / access TTL） | ✓ posts/1002 |
| 2 | Java 异常分析助手能做什么 | ok | 9736 | ✓ | 正确（定位/原因/步骤） | ✓ skills/3001 |
| 3 | 登录失败多少次 429 | ok | 5404 | ✓ | 正确（5 次后第 6 次 429） | ✓ posts/1004 |
| 4 | 头像上传大小限制 | ok | 7425 | ✓ | 正确（avatar 2MB） | ✓ shares/2002 |
| 5 | 版规禁止哪些行为 | ok | 6325 | ✓ | 正确 | ✓ help/rules |
| 6 | 如何用 GitHub 登录 | ok | 6565 | ✓ | 可用；步骤有推断成分 | ✓ welcome + oauth 帖 |
| 7 | token_version 作用 | ok | 6734 | ✓ | 正确 | ✓ posts/1002（top2 有噪声） |
| 8 | 项目分享如何防 XSS | ok | 5452 | ✓ | 正确（md-it + DOMPurify） | ✓ shares/2001 |
| 9 | Flyway 版本号要求 | ok | 6129 | ✓ | 正确（顺延、禁冲突） | ✓ posts/1001 |
| 10 | HIDDEN 谁能看见 | ok | 4799 | ✓ | 正确（仅作者） | ✓ help/rules |

**统计**：10/10 `branch=ok`，10/10 带来源 URL，0 兜底。主引用命中率主观 ≥9/10；问 6/7 存在次要噪声源，可接受一期水位。

**延迟观测**：本轮 wall P95 **&lt;10s**（相对 T-AI-0 e2e 11.6s 观测项有改善；评测跳过 rewrite）。T-AI-3/4 流式上线后继续采样。

## 5. 给 T-AI-3 的接口契约

- 同步：`POST {AI_SERVICE}/v1/ask` → `{question, thinking?, top_k?}`  
- 流式：`POST {AI_SERVICE}/v1/ask/stream` → SSE `token` / `sources` / `done`  
- 宕机：网关 503 + 降级话术（前端已有 mock 可先对照事件名）

## 6. 复现

```bash
cd ai-service
python -m unittest tests.test_retrieve -v
# 需根目录 .env 含 MIMO_API_KEY + DASHSCOPE_API_KEY
python -m scripts.eval_ask
```
