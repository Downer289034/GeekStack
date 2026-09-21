# T-FE-TAG-CLOUD-COPY · 标签云文案对齐含帖

> 负责人：**开发 B（极客栈前端）**  
> 优先级：P2  
> 派发：2026-09-21（T-FE-SEARCH-ASSISTANT-COPY 关单后）  
> 依据：后端 T-BE-4.2 已关（`4de1982`，标签云 = 可见帖 + PUBLISHED share/skill）

## 背景

`TagsView.vue` 仍写：**「当前为项目分享标签（share-only）；帖子标签待后端字段就绪后再扩展。」**  
后端已含帖，文案过时，易误导。

## 范围

1. 改 `/tags`（及若标签详情有同类过时说明）文案：count 含 **帖子 + 项目 + Skill**（与后端口径一致，简洁即可）。  
2. 「浏览标签」相关入口若写死 share-only，一并改掉。  
3. 截图 1 张入 `docs/assets/t-fe-tag-cloud-copy/`（页头 hint 可见即可）。

## 非范围

- 不改后端 TagCloudMapper  
- 不大改标签云视觉/布局算法  
- 不做搜索/助手再改

## 验收

1. 页面无「share-only / 帖子待就绪」类过时句  
2. 证据截图 + 独立 commit  
3. 三句话汇报

## 汇报格式

做了什么 / 证据 / 疑问。
