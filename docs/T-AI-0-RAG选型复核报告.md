# T-AI-0：RAG 链路预研选型复核报告

> **状态**：✅ **PM 选型复核通过（2026-09-20 07:45），四块拍板生效 = embedding 接口契约定稿 = T-AI-1 放行**  
> **实测时间**：2026-09-20 07:30 +08  
> **原始数据**：[`docs/assets/t-ai-0/bench_metrics.json`](./assets/t-ai-0/bench_metrics.json)  
> **复现**：`cd ai-service && python -m scripts.run_spike` / `python -m scripts.bench_report`  
> **流转**：本报告仅交 PM 复核，**不直发后端群**；四块拍板通过 = embedding 接口契约定稿 = 放行 T-AI-1。

---

## 0. 执行摘要（建议拍板）

| # | 拍板项 | 建议结论 | 实测支撑 |
|---|--------|----------|----------|
| 1 | 分块参数 | **512 token / 50 overlap** | 三档对比 + 任务清单基线；语料偏短，长文再校准见 §1 |
| 2 | thinking 开关 | **RAG 默认 `disabled`** | disabled P95 5.9s vs enabled P95 11.6s；引用率同为 5/5 |
| 3 | schema 字段 | **采纳统一草案 + chunk `text` 入 payload** | payload 抽检 `schema.ok=true`，字段清单见 §3 |
| 4 | 双 RAG 复用性 | **双库并存 + embedding 同源 text-embedding-v3** | 对齐管家口径；社区侧 Qdrant 已落地见 §4 |

**链路已跑通**：10 条语料 → DashScope embedding(1024) → Qdrant → MiMo top-5 RAG → 带 URL 回答。

---

## 1. 分块参数（拍板块①）

### 1.1 实测（10 篇 spike 语料，tiktoken cl100k_base）

| size | overlap | chunk_count | avg_tokens | max_tokens |
|------|---------|-------------|------------|------------|
| 256 | 32 | 10 | 92.9 | 176 |
| **512** | **50** | **10** | **92.9** | **176** |
| 1024 | 100 | 10 | 92.9 | 176 |

### 1.2 结论与诚实边界

- **建议拍板：512 / 50**（与任务清单基线一致，适合 top-5 上下文预算）。
- **边界**：当前样本单篇均 &lt;256 token，三档块数相同，**无法在本 spike 上拉开召回差异**。长帖/长 Skill 上线后，T-AI-1 应用真实内容复测块数与检索命中，必要时再调。
- 清洗策略：Markdown 去代码块/链接标记后滑窗；空正文回退标题。

---

## 2. MiMo thinking 对比（拍板块②）

模型：`mimo-v2.5` @ `api.xiaomimimo.com/v1`。同题 5 问 × 两模式；回答均要求带来源。

### 2.1 延迟与用量

| 模式 | n | avg_ms | **p95_ms** | prompt_tokens | completion_tokens | 含 URL 回答 |
|------|---|--------|------------|---------------|-------------------|-------------|
| **disabled** | 5 | 4164.7 | **5883.5** | 3055 | 574 | **5/5** |
| enabled | 5 | 7597.3 | **11596.8** | 3045 | 1373 | **5/5** |

相对 disabled：enabled **avg ≈ +82%**，**P95 ≈ +97%**，completion tokens ≈ **2.4×**（含 reasoning）。

### 2.2 质量观感（抽样）

- 两模式对「改密后 refresh 是否立即失效」「Java 异常分析助手用途」均答对要点并带来源 URL。
- enabled 有 `reasoning_content`（样例约 277–446 字），最终答案更铺陈，**事实正确性未观察到额外增益**（RAG 资料已足够）。
- e2e（retrieve+embed+LLM，thinking=disabled，8 问）：**avg 6445ms / P95 11635ms**。

### 2.3 建议拍板

**RAG 问答默认 `MIMO_THINKING=disabled`**。灰区/复杂推理场景再开 enabled（非一期范围）。thinking 开启时官方强制 temperature/top_p，已在客户端按文档处理。

---

## 3. Schema 字段（拍板块③）

### 3.1 统一 metadata（社区 Qdrant payload 实测）

| 字段 | 必填 | spike 样例/约定 | 抽检 |
|------|------|----------------|------|
| `content_id` | ✓ | `jkz_post_1002`（社区内容 `jkz_` 前缀） | ✓ |
| `source_system` | ✓ | `geekstack` | ✓ |
| `content_type` | ✓ | `post` / `share` / `skill` / `help` | ✓ |
| `text_url` | ✓ | 详情页 URL（不存全文原文，见下） | ✓ |
| `author_id` | ✓ | 脱敏 `u_9917787`（非用户名/邮箱） | ✓ |
| `created_at` | ✓ | ISO 时间 | ✓ |
| `visibility` | ✓ | `public`（一期只索引公开） | ✓ |
| `embedding_model` | ✓ | `text-embedding-v3`（版本化） | ✓ |
| **`text`** | ✓ | **chunk 正文必须入 payload**（否则 RAG 无上下文） | ✓ 非空 |
| `extra` | ○ | open dict，预留 | ✓ 空对象 |
| `chunk_id` / `chunk_index` | ✓ | 工程字段，删向量/幂等用 | ✓ |

`schema.ok = true`，`missing = []`（见 raw JSON）。

### 3.2 建议拍板

T-AI-1 按上表落库；**`text` 入 payload 为硬约束**（回函管家时保留此澄清）。全文原帖仍以业务库为准，`text_url` 只做溯源。

---

## 4. 双 RAG 复用性设计（拍板块④）

对照任务清单已对齐口径（管家 `rag-reuse-position.md` §二/§四；仓库内以任务清单收敛文为准）：

| 维度 | 对齐结论 | spike/本报告落实 |
|------|----------|------------------|
| Embedding 同源 | 双方 **DashScope `text-embedding-v3` / 1024 维**；铁律=同源，型号可配置 | 已替换原 BGE-M3；实测 dim=1024 |
| 双库物理隔离 | **治理语料（版规/判例）→ 管家 Milvus**；**社区内容 → 极客栈 Qdrant**；管家只读接入社区库 | spike 写社区侧 Qdrant（本机 `:memory:`；联调可 Docker 6333） |
| Schema 一致 | 上表为两边 collection 共同契约 | payload 已按表写入 |
| 隐私红线 | API 拉取不落库敏感原文 / 聚合优先 / 动作 API 幂等+审计 / 判罚回流只读 | 作者脱敏；一期仅 `visibility=public`；处置审计打样见 T-M4-1 `ADMIN_AUDIT` |
| 引擎差异 | Milvus ≠ Qdrant，**向量可比性靠同源 embedding + 同 schema**，不靠引擎同构 | 设计接受 |

### 4.1 DashScope 接入实测（支撑块③/④）

| 指标 | 值 |
|------|-----|
| 模型 | `text-embedding-v3` |
| 维度 | **1024** |
| 10 chunk 批量写入 | wall ≈ 798ms（含批调度）；API 侧累计 ≈ 147ms |
| Query 延迟 | avg **193ms** / P95 **261ms**（5 次采样） |
| Token | 1165（10 chunks） |
| 粗算成本 | ≈ $0.000117 / 本批；$0.1/1M 假设下日 1000 问 query ≈ **$0.012/日** |

### 4.2 建议拍板

确认双库并存 + 同源 embedding；T-AI-1 只写极客栈 Qdrant 社区库；帮助语料用 `jkz_help_*` 一次性灌库，与治理判例（管家 Milvus）分离。

---

## 5. 成本粗算（辅助，非拍板项）

假设：日 1000 问、thinking=disabled、MiMo $1/$3 per 1M in/out、embedding $0.1/1M。

| 项 | 日成本 (USD) | 月×30 |
|----|--------------|-------|
| LLM | 0.955 | ≈ 28.7 |
| Embedding (query) | 0.012 | ≈ 0.35 |
| **合计** | **≈ 0.97** | **≈ 29** |

价格为套餐粗算，上线前按实账单校准。

---

## 6. 环境与复现说明

- Key：根目录 `.env` 的 `MIMO_API_KEY` / `DASHSCOPE_API_KEY`（零明文入库）。
- 向量库：本报告默认 `QDRANT_URL=:memory:`（无 Docker 阻塞）；生产/联调改 `http://127.0.0.1:6333` + `docker compose up -d`。
- 代码目录：`ai-service/`（骨架可复用至 T-AI-2）。

---

## 7. PM 拍板清单（✅ 2026-09-20 复核通过，原始数据 bench_metrics.json 交叉核对一致）

- [x] **分块**：512 / 50（长文复测留 T-AI-1）——样本偏短的诚实边界已声明，按基线取值
- [x] **thinking**：RAG 默认 disabled——P95 +97%、tokens 2.4×、引用率无增益，数据充分
- [x] **schema**：上表字段 + `text` 入 payload + `jkz_` 前缀 + `embedding_model` 版本化——`schema.ok=true` 抽检过；管家 v1.1 三字段（chunk_text/chunk_index/parent_content_id）并入
- [x] **双 RAG**：社区 Qdrant + 管家 Milvus 隔离；embedding 同源 text-embedding-v3

**复核附加观测项（随 T-AI-1/T-AI-2 落地持续关注，非打回项）**：e2e 端到端 P95 11635ms 偏高（8 问小样本，含 retrieve+embed+LLM 全链）——T-AI-4 流式渲染掩盖感知延迟 + T-AI-2 上线后持续采样，若 P95 持续 >10s 需查 Qdrant 部署形态（:memory: → Docker 的差异）与 MiMo 网络抖动。

拍板已转发并生效。**T-AI-1 已按定稿落地**（`com.community.rag`，schema v1.1）。
