# T-BE-4.1 · 搜索慢查询观察（只观测不改契约）

> 产出：2026-09-21 ｜ 开发 A ｜ 工作区 `2026-09-06-18-49-57`  
> 任务书：`docs/tasks/T-BE-4.1-search-observe.md`  
> 依据：`docs/T-BE-4-搜索标签接口预研.md`（公测前默认仍搜正文）  
> **本单无 SQL/契约变更**

---

## 1. 观测环境

| 项 | 值 |
|----|----|
| community-dev MySQL | `172.25.41.137:3306` / DB `community`（seed 后体量） |
| prod API | `http://172.25.41.137/api/v1/search`（`geekstack-backend`，数据更少） |
| 方法 | SQL：`EXPLAIN` / `EXPLAIN ANALYZE` + 客户端 10 次计时；HTTP：丢弃首包暖机后 10 次 |
| 原始数字 | `docs/assets/t-be-4.1/`（脱敏，无凭据） |

### 行数量级（可见搜索范围）

| 库 | post `deleted=0` | share `PUBLISHED` | skill `PUBLISHED` | content_md 均长/最大（字符） |
|----|------------------|-------------------|-------------------|------------------------------|
| community-dev | **20** | **21** | **12** | post ~181 / 338；share ~245 / 371；skill ~224 / 289 |
| prod | **7** | **1** | **0** | post 均长 ~17（几乎空壳） |

结论：当前两侧均为 **十～几十行** 量级，尚不足以触发 LIKE 全表扫的产品级痛点；观察价值在于**基线 + 执行计划形态**，非压测定论。

---

## 2. 样例 q 与命中（community-dev SQL COUNT）

等价于 `SearchService` 字段范围（post：`title|content_md`；share：+`summary`；skill：+`model_adaptation_md`）。

| type | q | 命中条数 | 客户端 RTT avg / P95 / max (ms)※ |
|------|---|----------|----------------------------------|
| post | `Java` | 1 | 59.5 / 62.6 / 62.6 |
| post | `Spring` | 1 | 61.2 / 68.2 / 68.2 |
| post | `a`（宽匹配） | 20 | 68.7 / 81.6 / 81.6 |
| share | `Spring` | 0 | 68.3 / 79.2 / 79.2 |
| share | `Docker` | 0 | 67.2 / 87.0 / 87.0 |
| share | `a` | 21 | 114.4 / 521.5† / 521.5 |
| skill | `RAG` | 0 | 64.1 / 68.0 / 68.0 |
| skill | `Agent` | 8 | 90.5 / 124.5 / 124.5 |
| skill | `a` | 12 | 84.2 / 109.1 / 109.1 |

※含每次新建 `mysql` 客户端连接，**非纯引擎耗时**；基线约 60ms。  
†`share/a` 单次尖刺，疑连接偶发；引擎侧见下节毫秒级。

### prod HTTP（端到端，暖机后 10 次）

| type | q | `total` | avg / P95 / max (ms) |
|------|---|---------|----------------------|
| post | `Java` | 0 | 29.7 / 32.7 / 32.7 |
| post | `Spring` | 0 | 30.7 / 51.0 / 51.0 |
| post | `a` | 7 | 36.3 / 45.7 / 45.7 |
| post | `不存在的关键词XYZ123` | 0 | 29.3 / 36.9 / 36.9 |
| share | `Spring`/`Docker`/`a` | 0 | ~24–33 / ≤51 |
| skill | `RAG`/`Agent`/`a` | 0 | ~24–26 / ≤41 |

prod P95 **&lt; 55ms**，与体量一致；skill 在 prod 无 PUBLISHED 行，仅验证路由可达。

---

## 3. 执行计划要点（community-dev）

| 查询 | type | key | Extra / 实际 |
|------|------|-----|--------------|
| post `LIKE %Java%` | **ALL** 全表扫 | 无 | `Using where; Using filesort`；`EXPLAIN ANALYZE` actual **~3.7ms**（扫 22 行） |
| post `LIKE %a%` | ALL | 无 | actual **~2ms**，命中 20/22 |
| share `LIKE %Spring%` | range | `idx_share_status_created` | 先按 status 索引再 LIKE 过滤；actual **~0.8ms** |
| skill `LIKE %RAG%` | index | `idx_skill_status_created` | actual **~0.3ms**（扫 12 行） |

与预研一致：前导 `%` **无法走 BTree 文本索引**；post 走全表 + filesort；share/skill 仅借 status 列表索引缩小候选，LIKE 仍为行上过滤。

---

## 4. 是否需开正式 T-BE-4.1（缩字段 / FULLTEXT / ES）

| 判据 | 现状 |
|------|------|
| 行数 | 十～几十；正文均长数百字符 |
| 引擎 actual | ≤数毫秒 |
| prod HTTP P95 | &lt; 55ms |
| 用户可感知慢 | **未见** |

**建议：暂不开正式 T-BE-4.1 实现单。**  
公测前维持「搜正文」契约（预研默认）。触发再开的证据门槛建议：

1. 单表可见行 **≥ 1e4～1e5**，或 `content_md` 均长显著增大；且  
2. `/search` P95（服务端日志或 APM）持续 **≥ 200～300ms**；或  
3. 慢查询日志出现同形 `LIKE %…%` 占主导。

届时优先评估：**默认只搜 title(+summary)** 开关 → 再考虑 ES，仍跳过 MySQL FULLTEXT 过渡（与 T-BE-4 预研一致）。

---

## 5. 非范围复核

- 未改搜索 SQL / API 契约  
- 未上 FULLTEXT / ES  
- 未改前端
