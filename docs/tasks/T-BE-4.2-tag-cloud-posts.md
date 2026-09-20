# T-BE-4.2 · 标签云纳入帖子 tags

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P2  
> 派发：2026-09-20（T-BE-4 关单 + PM 拍板「云须含帖」）  
> 依据：`docs/T-BE-4-搜索标签接口预研.md` §三 / §七 PM 回执

## 背景

`GET /api/v1/tags` 当前 `JSON_TABLE` 只聚合 **PUBLISHED share + skill**，不含 `post.tags`。帖子已支持标签与 `?tag=` 过滤，云与产品预期不一致。

## 范围

1. `TagCloudMapper`（或等价）：UNION 增加 **可见帖子** 的 tags 展开；count = share + skill + post 命中条数之和（同名 tag 合并）。  
2. 帖子可见性与列表一致（勿把已删/不可见帖算进云）。  
3. 排序维持 `count DESC, name ASC`（或与现实现一致）。  
4. 集成测：有 post 标签、无 share/skill 时云仍能出现该 tag；断言 count。  
5. 同步改 `TagService` 注释 / 预研文档口径一句（可选，本仓 docs）。

## 非范围

- 不建标签表（那是 T-BE-4.3）  
- 不做搜索字段缩范围（T-BE-4.1，暂不开）  
- 不改前端（若文案写「项目/Skill」过窄，另派 B）  
- 不改 OAuth / Admin

## 验收

1. `GET /tags` 含帖子贡献的 tag 与正确合并 count  
2. 相关测例绿；独立 commit  
3. 三句话汇报

## 完成说明（开发 A，2026-09-20）

- `TagCloudMapper` UNION 可见帖（`deleted=0`）；count = post+share+skill 合并  
- 测例：`postOnlyTagAppearsInCloud`、`postAndShareSameTagMergesCount`、`deletedPostTagsExcludedFromCloud`

## 汇报格式

做了什么 / 证据 / 疑问。
