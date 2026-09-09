# T-BE-4 搜索 / 标签接口预研（只研不做）

> 产出日期：2026-09-09 ｜ 任务：T-BE-4 ｜ 状态：**方案文档，无实现**  
> 对齐输入：`docs/T-FE-1-首页与搜索盘点.md`（G1/G2）、`docs/JavaAI社区-架构与产品规划.md`（search 插槽）、现状 `ShareService` JSON_CONTAINS  
> **硬约束**：待 T-FE-1 缺口单经 PM 拍板后，再立正式实现任务；本文不得被解读为已批准开工。

---

## 一、现状盘点（代码实物）

| 能力 | 现状 | 含义 |
|------|------|------|
| 项目标签筛选 | `GET /api/v1/shares?tag=` → `JSON_CONTAINS(tags, JSON_QUOTE(?))` | **已有**精确单标签过滤；无标签云计数接口 |
| 帖子标签 | `post` 表无 `tags` 列 | 论坛侧无法按标签滤；G2「跨帖子标签」需加字段或放弃帖侧 |
| 全文搜索 | 无 Controller / 无 FULLTEXT 索引 | G1 阻塞 T-FE-3 |
| 架构预设 | `search` 模块 M3+：「初期 MySQL LIKE + 索引兜底，预留 SearchService，量大换 ES」 | 与「先简单后替换」一致 |
| 分页 | MyBatis-Plus `Page` + `PageResponse`；列表普遍 `id` 次级稳定序 | 新接口应复用，勿另起分页语义 |

T22 备注：广场列表首请求 P95≈198ms（含 JSON_CONTAINS + 冷启动），缓存后预期显著下降——标签筛选现状可接受，但标签云若对全表扫 JSON 需另估。

---

## 二、全文搜索：FULLTEXT ngram vs LIKE

### 2.1 候选对比

| 维度 | `LIKE %q%`（架构默认） | InnoDB FULLTEXT + ngram |
|------|------------------------|-------------------------|
| 中文 | 可用（无分词，按子串） | ngram 解析器可拆中文；**版本/配置敏感**（`ngram_token_size`，默认 2） |
| 索引 | 难用普通 BTree 吃 `%前缀`；大表易全表扫 | 可建 FULLTEXT；短语/相关度有 `MATCH...AGAINST` |
| 运维 | 零额外组件 | 需迁移加索引；改 parser 成本高；**Flyway 不可回改**已执行脚本 |
| 相关度 | 无，只能 `ORDER BY created_at/id` | 有 `MATCH` score，但中文短查询噪音常见 |
| 与架构 | **直接命中**「初期 LIKE」 | 提前吃坑，且 T-FE-1 问题 3 尚未拍板「是否必须短语分词」 |
| 演进 | SearchService 接口后换 ES，调用方无感 | 若先上 FULLTEXT，换 ES 时索引与 SQL 双废弃 |

### 2.2 建议（预研倾向，非开工令）

1. **一期实现优先 LIKE**：`title` / `summary` / `content_md` 前缀策略可再收窄（例如只搜 title+summary，正文可选），对齐架构与 T-FE-1「LIKE 是否即可上线」待拍板项。  
2. **不在一期上 ngram FULLTEXT**：中文 token、短查询、迁移不可逆风险高于收益；若 PM 强制「相关度/分词」，优先评估 **ES/OpenSearch 插槽** 而非 MySQL FULLTEXT 过渡。  
3. **契约草案（沿用 T-FE-1，待批）**：  
   `GET /api/v1/search?q=&type=all|post|share&current&size`  
   - 空 `q`：400 或空列表（实现任务里钉死 + 正反测）  
   - `type=all`：两路查询合并或串行，**分页口径见第四节**（易踩坑）  
   - 鉴权：公开只返回帖子全量可见 + share `PUBLISHED`（对齐现有可见性）

### 2.3 开放问题（等 T-FE-1 拍板）

- 是否接受「仅 title+summary，不含正文」以降延迟？  
- `type=all` 的混合分页要不要一期就做，还是 Tab 分类型各查各的？

---

## 三、标签聚合：JSON_CONTAINS vs 标签表

### 3.1 候选对比

| 维度 | 维持 JSON 数组 + `JSON_CONTAINS` | 规范化 `tag` + `share_tag`（帖侧另议） |
|------|----------------------------------|----------------------------------------|
| 现状契合 | **零迁移**；广场筛选已通电 | 需 V4 迁移、双写/回填、改写入路径 |
| 标签云 `name+count` | `JSON_TABLE`（MySQL 8）展开再 `GROUP BY`；或应用层聚合缓存 | `SELECT tag_id, COUNT(*)` 自然 |
| 性能 | 单标签过滤已压测压线；**全站标签云**随分享量上升变贵 | 计数与筛选更稳；写入多一次 |
| 帖子侧 | JSON 方案也要先给 `post.tags`；或明确 **一期仅项目**（T-FE-1 问题 2） | 同样要定帖侧是否纳入 |
| 复杂度 | 低；符合「演示与首页验收优先」 | 中；适合标签成为核心导航之后 |

### 3.2 建议（预研倾向）

1. **一期标签聚合 = 项目-only**：`GET /api/v1/tags` 基于 `project_share.tags` 聚合（JSON_TABLE 或定时/缓存物化）；详情列表继续 `GET /shares?tag=`。  
2. **暂不建标签表**：待 T-FE-3 真实流量与标签基数证明 JSON 聚合不够，再立迁移任务。  
3. **帖子标签**：默认 **不做**，除非 PM 否决「一期仅项目」；若要做，优先加 `post.tags JSON` 与分享同构，而不是先上关联表。

### 3.3 契约草案（待批）

| 接口 | 说明 |
|------|------|
| `GET /api/v1/tags?limit=50` | `[{ "name", "count" }]`，仅统计 `PUBLISHED` 且未删除分享 |
| `GET /api/v1/shares?tag=&current&size` | **已有**，标签详情页复用 |

---

## 四、分页口径（搜索 / 标签详情必须先定）

| 场景 | 推荐口径 | 理由 |
|------|----------|------|
| 单类型列表（post 或 share） | 与现网一致：`current`+`size`，MyBatis-Plus，**次级 `ORDER BY id DESC`** | 稳定翻页，防同时间戳抖动 |
| `type=all` 混合流 | **一期避免真混合全局分页**；改为前端 Tab 分类型各请求，或服务端返回 `{ posts: Page, shares: Page }` 双页 | 混合 offset 在两表 + 不同排序键下难做稳定「第 N 页」；相关度未定时更乱 |
| 标签云 | 不分页或只 `limit`；不按 `current` | 体量小；需要时再加 |
| 空结果 | `records=[]`，`total=0`，HTTP 200 | 对齐现有 `PageResponse` |

**禁止**：`selectPage` + `wrapper.last("LIMIT n")` 双重 LIMIT（交接指引坑）。

---

## 五、与 T-FE-1 拍板项的映射

| T-FE-1 问题 | 本文立场 |
|-------------|----------|
| 1. 首页三区块条数 | 与搜索无关；不阻塞本预研 |
| 2. 标签聚合一期是否仅项目 | **建议接受**；实现任务以此为默认范围 |
| 3. 搜索 LIKE 是否即可 | **建议接受 LIKE 一期**；FULLTEXT ngram 不立项 |

---

## 六、正式实现任务建议切分（将来，非当前）

仅在 PM 关闭 T-FE-1 缺口单后立项，例如：

1. **T-BE-S1**：`SearchService` + `GET /search`（LIKE，type=post|share，分页+可见性+三路径测试）  
2. **T-BE-S2**：`GET /tags`（JSON_TABLE 或等价聚合 + 缓存策略）  
3. **T-BE-S3**（可选）：`post.tags` + 论坛按 tag 滤——仅当否决「仅项目」时  

**明确不做（当前）**：代码、Flyway、Controller、前端联调。

---

## 七、结论一句话

> 搜索跟架构走 LIKE + SearchService 插槽；标签一期复用 JSON + 现有 `?tag=`，标签云只读聚合；混合搜索分页用双列表/Tab 避开假全局页；FULLTEXT ngram 与标签表留待有证据再议。
