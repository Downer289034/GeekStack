# T-ADMIN-FEATURE 契约（管家 AD1 / MA3 映射）

鉴权：`Authorization: Bearer <ADMIN JWT>`（含 `steward_bot`）。`/api/v1/admin/**` → `hasRole('ADMIN')`。  
未登录 → HTTP 401 / `code=40100`；普通 USER → HTTP 403 / `code=40300`；成功 → HTTP 200 / `code=0`。

## 加精 / 取消

```
PUT /api/v1/admin/posts/{id}/featured
Content-Type: application/json

{ "featured": 1 }   // 1=加精，0=取消；重复同值幂等
```

审计：`ADMIN_AUDIT` 日志  
`action=ADMIN_SET_POST_FEATURED operatorId=… operator=… targetPostId=… targetUserId=… featured=0|1`

## 置顶 / 取消

```
PUT /api/v1/admin/posts/{id}/pinned
Content-Type: application/json

{ "pinned": 1 }     // 1=置顶，0=取消；重复同值幂等
```

审计：`action=ADMIN_SET_POST_PINNED … pinned=0|1`

## 读回

`GET /api/v1/posts/{id}`（及列表卡片）响应含：

- `isFeatured`: 0|1  
- `isPinned`: 0|1  

帖不存在 → HTTP 404 / `code=40400`。body 字段越界（非 0/1）→ 参数校验失败（400 系）。

## 非范围

不做推荐算法；不做 Gitee refresh。
