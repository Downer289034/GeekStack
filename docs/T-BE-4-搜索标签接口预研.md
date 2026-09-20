# T-BE-4 · 搜索 / 标签接口预研（只研不做）

> 产出：2026-09-20 ｜ 负责人：开发 A ｜ **方案文档，无实现代码**  
> 任务书：`docs/tasks/T-BE-4-search-tag-research.md`  
> 对照：现状代码（`SearchService` / `TagCloudMapper` / `com.community.rag` / `AiAskController`）；旧稿 `docs/T-BE-4-搜索与标签接口预研.md`（2026-09-09，**已过时**，本文取代其拍板效力）  
> 参考：`docs/T-AI-0-RAG选型复核报告.md`、`docs/T-FE-1-首页与搜索盘点.md`

---

## 一、现状盘点（2026-09-20 代码实物）

| 能力 | 现状 | 关键路径 |
|------|------|----------|
| 关键字搜索 | `GET /api/v1/search?q=&type=post\|share\|skill&current&size`；**MySQL `LIKE %q%`**（`LikeEscape`）；无 FULLTEXT | `SearchController` / `SearchService` |
| 搜索字段 | post：`title\|content_md`；share：`title\|summary\|content_md`；skill：另含 `model_adaptation_md`；share/skill 强制 `PUBLISHED` | 同上 |
| `type=all` | **不做**（缺省/非法 type → 400） | `SearchService` |
| 标签列 | `post.tags`（V4）、`project_share.tags`（V1）、`skill.tags`（V5）均为 **JSON 数组** | Flyway V1/V4/V5 |
| 单标签过滤 | `GET /posts?tag=`、`/shares?tag=`、`/skills?tag=` → `JSON_CONTAINS(tags, JSON_QUOTE(?))` | Post/Share/Skill Service |
| 标签云 | `GET /api/v1/tags`：`JSON_TABLE` 展开 **PUBLISHED 的 share+skill**，**不含 post**；按 count 降序 | `TagCloudMapper` |
| 搜索类索引 | **无** FULLTEXT；无 title/content/tags 专用索引；仅有 category/status/created 列表索引 | Flyway |
| AI 助手 | `POST /api/v1/ai/ask`（JWT+限流+SSE）→ ai-service `/v1/ask*` → Qdrant 向量检索 + LLM | `AiAskController` / `rag` 包 |

旧稿「无搜索 Controller / post 无 tags」已被后续 T-FE-3 / T-TAG-BE / Skill 等实现追上；**演进决策应从「已有 LIKE + JSON」出发**，而非从零选型。

---

## 二、关键字搜索：FULLTEXT（ngram）vs LIKE

### 2.1 对比表

| 维度 | 维持 / 优化 `LIKE %q%` | InnoDB FULLTEXT + **ngram** |
|------|------------------------|-----------------------------|
| 中文 | 子串匹配，**不依赖分词**；短词/代码片段友好 | 依赖 `ngram` parser；`ngram_token_size` 默认 2，改 parser **不可轻易回退** |
| 索引可用性 | 前导 `%` **无法走 BTree**；正文扫表成本随数据量线性升 | 可建 `FULLTEXT(title, content_md)`；`MATCH … AGAINST` 有相关度 |
| MySQL 8.0 现状 | 本栈已是 8.0，LIKE 行为稳定 | 需确认实例已装/启用 ngram；云厂商参数是否允许改 `ngram_token_size` |
| 相关度排序 | 无；当前 `ORDER BY created_at, id` | 有 score，但中文短查询噪声大；短语查询语义与产品预期常不一致 |
| 写入开销 | 无 | FULLTEXT 维护成本；大 MEDIUMTEXT 更新更重 |
| 运维 / 迁移 | 零组件；可只加「title 前缀索引」等弱优化 | Flyway 加索引后难回滚；换 ES 时双废弃 |
| 与架构预设 | 命中「初期 LIKE，预留 SearchService，量大换 ES」 | 多一层过渡技术债 |
| 与 RAG | 职责清晰：关键词精确子串 | 易与「语义搜索」产品话术混淆，需强约束文案 |

### 2.2 风险点（FULLTEXT 中文）

1. **配置锁定**：`ngram_token_size=2` 时单字查询行为差；改为 1 索引体积暴涨。  
2. **停用词 / 最短词长**：InnoDB FULLTEXT 对短 token 有默认限制，中文两字词边界坑多。  
3. **布尔模式 vs 自然语言模式**：产品「包含关键字」更接近 LIKE；FULLTEXT 默认相关度排序会「搜不到刚发的帖」。  
4. **多表异构**：post/share/skill 字段集合不同，三套 FULLTEXT 或一张宽表，迁移成本不低。

### 2.3 本节结论（推荐）

- **近期（演示～公测）**：**继续 LIKE**，不立 FULLTEXT 实现单。  
- **优化（可选小单）**：① 默认只搜 `title+summary`（正文可选开关）；② 对 `title` 做前缀/生成列策略评估；③ 慢查询阈值 + 缓存热词——工作量见第六节。  
- **中长期**：行数/慢查询证明 LIKE 不够时，**直接评估 ES/OpenSearch 插槽**，跳过 MySQL FULLTEXT 过渡层。

---

## 三、标签聚合：JSON_CONTAINS / JSON_TABLE vs 独立标签表

### 3.1 对比表

| 维度 | 维持 JSON 数组 | 规范化 `tag` + `*_tag` 关联表 |
|------|----------------|------------------------------|
| 现状契合 | **零迁移**；过滤/云已上线 | 需 Flyway + 回填 + 写入双路径 |
| 单标签过滤 | `JSON_CONTAINS`；列表场景可接受 | `JOIN` + 索引更稳 |
| 标签云 count | `JSON_TABLE` + `GROUP BY`（现实现）；share+skill UNION | `COUNT(*)` 自然；易含 post |
| 分页口径 | 云接口当前 **一次返回全量排序列表**（应用层可 limit）；详情页用各业务 `?tag=` 分页 | 同左，但 count 语义更干净 |
| 写入 | `TagNormalizer` 去重上限即可 | 发帖/编辑多表事务 |
| 同义词 / 改名 | JSON 难治理 | 表级 rename/merge 可行 |
| 复杂度 | 低，适合当前体量 | 中；标签成为核心导航后再上 |

### 3.2 分页与 count 口径（必须钉死）

| 场景 | 推荐口径 |
|------|----------|
| 标签云 `GET /tags` | 全局聚合：每个 tag 的 **内容条数** = 命中该 tag 的 PUBLISHED share 数 + PUBLISHED skill 数（**当前不含 post**）；排序 `count DESC, name ASC` |
| 标签详情 | **不混搜**：`GET /posts?tag=` / `/shares?tag=` / `/skills?tag=` 分 Tab 分页；`PageResponse.total` = 该类型下精确匹配条数 |
| 搜索 `GET /search` | 与标签无关；`total` = 该 `type` 下 LIKE 命中条数；**不做 type=all 混合分页**（避免跨类型 cursor 语义） |

**缺口**：标签云未计入 `post.tags`——与「帖子已支持标签」产品预期可能不一致，见第七节疑问。

### 3.3 本节结论（推荐）

- **维持 JSON + JSON_TABLE/JSON_CONTAINS**，不急建标签表。  
- 若产品要求云含帖子：优先 **改 TagCloudMapper UNION 加 post**（小实现单），而非先上关联表。  
- **触发建表条件**（满足任一再立 T-BE-4.2）：标签云 P95 持续恶化；需要同义词/运营改名；标签成为强导航且过滤 QPS 高。

---

## 四、与 RAG 边界（何时 SQL、何时向量）

| 维度 | 站内搜索 `/api/v1/search` | AI 助手 `/api/v1/ai/ask` → Qdrant |
|------|---------------------------|----------------------------------|
| 用户意图 | 「找含某词的帖/分享/Skill 列表」 | 「用社区内容回答问题」 |
| 匹配 | SQL 子串 / 精确字段 | Embedding 语义相似 + LLM 生成 |
| 结果形态 | 分页卡片列表 | SSE 流式回答 + sources |
| 鉴权 | 公开（可见性在 SQL 过滤） | JWT + 限流 |
| 失败降级 | 空列表 / 400 | 503 / 无 Key 伪向量等（见 T-AI） |
| **禁止** | 用 RAG 冒充搜索列表 | 用 LIKE 结果直接当「助手答案」 |

**原则（拍板建议）**

1. **产品入口分离**：顶栏/搜索页 → 只调 `/search`；助手浮层/对话 → 只调 `/ai/ask`。  
2. **不在 SearchService 内调 Qdrant**；不在 ai-service 内复刻业务列表分页。  
3. 若未来要「语义搜列表」：另立产品能力（如 `/search/semantic`），仍返回列表契约，**不**走问答 prompt。  
4. 索引侧：RAG 写 Qdrant 与站内 LIKE **双轨**；删帖/隐藏需两边一致（现有 `RagContentChangedEvent`），但不共享查询路径。

---

## 五、推荐方案（可拍板）

| # | 决策 | 理由 |
|---|------|------|
| R1 | **关键字搜索继续 LIKE**；不上 MySQL FULLTEXT ngram | 已上线、中文子串够用；FULLTEXT 配置/噪声/迁移风险高于收益；量大直连 ES |
| R2 | **标签继续 JSON**；云继续 JSON_TABLE | 已实现且体量匹配；建表 ROI 不足 |
| R3 | **明确 RAG 边界**：搜索 ≠ 助手 | 避免重复造检索与产品话术混淆 |
| R4 | **实现拆单**（见下），本预研不写代码 | 符合任务书；避免「预研=开工」 |

### 建议拆单（实现时）

| 单号（建议） | 内容 | 优先级 |
|--------------|------|--------|
| **T-BE-4.1** | 搜索体验小优化：可选缩小字段范围、慢查询日志/告警、title 索引可行性验证（仍可不改契约） | P2 |
| **T-BE-4.2** | 标签云纳入 `post.tags`（改 SQL UNION + 测例）；文档同步 count 口径 | P2（若产品要） |
| **T-BE-4.3** | （触发条件满足后）标签表迁移 + 双写回填 | P3 |
| **T-BE-4.4** | （触发条件满足后）ES/OpenSearch 插槽设计 | P3 |

---

## 六、工作量估算（人天，仅实现单参考）

| 项 | 人天 | 说明 |
|----|------|------|
| T-BE-4 本文档 | 0.5 | 本单已完成 |
| T-BE-4.1 搜索微优化 | **1～1.5** | 契约几乎不变；含测例与慢查询观察 |
| T-BE-4.2 标签云含 post | **0.5～1** | Mapper + 集成测 + 前端若需提示文案（FE 另估） |
| T-BE-4.3 标签表 | **3～5** | 迁移、回填、写入改造、双读窗口、回滚预案 |
| T-BE-4.4 ES 插槽 | **5～8** | 含同步管道与双写；不含集群运维采购 |

**本单不消耗实现人天。**

---

## 七、遗留疑问（需 PM / 产品）

1. 标签云是否必须统计 **帖子**？当前仅 share+skill。  
2. 搜索是否接受「默认不搜正文」以换延迟？（需 FE/产品确认）  
3. 公测前是否有行数目标（如 post 10 万）作为 LIKE→ES 触发线？  
4. 助手与搜索入口文案是否已统一「搜索找内容 / 助手问答」？（避免用户把 `/search` 当 Chat）

---

## 八、非范围（本单遵守）

- 不写搜索/标签实现、不改 Flyway、不改前端  
- 不做 OAuth / Admin 加精改动  
- 不将旧稿 `T-BE-4-搜索与标签接口预研.md` 当作现行结论（保留作历史即可）
