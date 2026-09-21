# T-BE-4.1-LIGHT · 搜索慢查询观察（只观测不改契约）

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P3 · T-BE-4 预研跟进  
> 派发：2026-09-21（T-DEPLOY-BACKEND-SYNC-IMAGE 关单后）  
> 依据：`docs/T-BE-4-搜索标签接口预研.md`（公测前默认仍搜正文；有证据再缩字段）

## 范围

1. 对 `GET /api/v1/search`（post/share/skill 各一）在 **community-dev** 或本机跑若干典型 q，记录耗时（日志或 EXPLAIN，脱敏）。  
2. 一页 `docs/T-BE-4.1-搜索观察.md`：样例 q、行数量级、P95/最大耗时、是否需开正式 4.1。  
3. **不改**搜索 SQL/契约（除非发现明确 bug）。

## 非范围

- 不上 FULLTEXT / ES  
- 不改前端

## 验收

1. 观察文档入库；独立 commit（仅 docs 可）  
2. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。
