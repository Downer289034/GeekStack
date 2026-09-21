# T-FE-FEATURE-PIN-BADGE 证据

- mock postId：`9001`（置顶+精华）、`9002`（仅精华）

| 场景 | 截图 | 结果 |
|---|---|---|
| 论坛列表 | 01-forum-list-badges.png | pass |
| 帖子详情 | 02-post-detail-both.png | pass |

```json
[
  {
    "id": "forum-list",
    "status": "pass",
    "detail": "pinned=1; featured=2; postIds=9001,9002"
  },
  {
    "id": "post-detail",
    "status": "pass",
    "detail": "pinned=1; featured=1; hasTitle=true; snippet=置顶且精华示例帖 置顶 精华 Java 浏览 42 回帖 3 2026/9/21 09:30:51  证据帖 #9001：isPinned=1 isFeatured=1  回帖（3） 还没有回帖，来抢沙发  登录 后参与回帖"
  }
]
```
