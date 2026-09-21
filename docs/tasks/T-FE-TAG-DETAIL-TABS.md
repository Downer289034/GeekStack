# T-FE-TAG-DETAIL-TABS · 标签详情分类型列表

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-TAG-CLOUD-COPY 关单后）  
> 背景：标签云 count 已含帖/项目/Skill，但 `/tags/:name` 仍只 `pageShares`，与云口径不一致。

## 范围

1. `TagDetailView`：按类型 Tab（至少 **帖子 / 项目 / Skill**），各自分页拉已有接口（`?tag=` 或现有 pagePosts / pageShares / pageSkills）。  
2. 文案：总数/空态按当前 Tab（勿再写死「共 N 个项目」当唯一口径）。  
3. 默认 Tab：可 `项目` 或按 URL `?type=`；刷新保持。  
4. 截图：同标签下至少两个 Tab 有内容或空态差异，入 `docs/assets/t-fe-tag-detail-tabs/`。

## 非范围

- 不改后端聚合 count API  
- 不做 type=all 混合无限流  
- 不改广场/搜索主流程

## 验收

1. 点标签云进入详情，可切到帖子/Skill，不再只能看项目  
2. 证据截图 + 独立 commit  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。
