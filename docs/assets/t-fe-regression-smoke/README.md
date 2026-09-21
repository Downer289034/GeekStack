# T-FE-REGRESSION-SMOKE 证据

> 近期改动轻量回归：搜索文案 / 标签三 Tab / 精华置顶 / 同步按钮

| # | 场景 | 截图 | 结果 |
|---|---|---|---|
| 1 | 搜索「找内容」+ 助手「社区问答」 | 01-search-find-content.png · 02-assistant-community-qa.png | pass |
| 2 | 标签云含帖 + 详情三 Tab | 03-tags-cloud-hint.png · 04-tag-detail-tabs.png | pass |
| 3 | 论坛置顶/精华徽章 | 05-forum-pin-feature-badges.png | pass |
| 4 | Owner 同步按钮（未强制同步） | 06-profile-sync-btns.png | pass |

```json
[
  {
    "id": "search-assistant-copy",
    "status": "pass",
    "detail": "findTitle=1; findHint=1; qaTitle=1; fabTitle=社区问答"
  },
  {
    "id": "tags-three-tabs",
    "status": "pass",
    "detail": "cloudHint=1; tabs=3; roleTabs=1/1/1"
  },
  {
    "id": "forum-badges",
    "status": "pass",
    "detail": "pinned=1; featured=2"
  },
  {
    "id": "profile-sync-btns",
    "status": "pass",
    "detail": "github=true; gitee=true"
  }
]
```
