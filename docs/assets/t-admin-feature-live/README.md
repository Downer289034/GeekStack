# T-ADMIN-FEATURE-LIVE-SMOKE 路 steward_bot 鍔犵簿/缃《鐪熸満璇佹嵁

> 2026-09-21锝滃紑鍙?A锝滃伐浣滃尯 `2026-09-06-18-49-57`锝滄棤 token / 鏃犲瘑鐮?

## 鐜

| 椤?| 鍊?|
|----|----|
| API | `http://172.25.41.137`锛坧rod nginx 鈫?`geekstack-backend`锛?|
| 璐﹀彿 | `steward_bot`锛堝嚟鎹粎鏈満 OFFLINE锛涙湭鍏ュ簱锛?|
| 鐩爣甯?| `postId=164` |

## 鍐掔儫缁撴灉

| 鍔ㄤ綔 | 璇锋眰 | code |
|------|------|------|
| 鍔犵簿 | `PUT /api/v1/admin/posts/164/featured` `{featured:1}` | **0** |
| 缃《 | `PUT /api/v1/admin/posts/164/pinned` `{pinned:1}` | **0** |
| 鍙栨秷鍔犵簿 | `{featured:0}` | **0** |
| 鍙栨秷缃《 | `{pinned:0}` | **0** |

## 瀹¤锛坥perator=`steward_bot`锛?

鎽樺綍瑙?`admin-audit-excerpt.txt`锛?

- `ADMIN_SET_POST_FEATURED` 路 `operatorId=628` 路 `featured=1` / `0`
- `ADMIN_SET_POST_PINNED` 路 `operatorId=628` 路 `pinned=1` / `0`

API 鎽樿瑙?`smoke-summary.json`銆備緵绠″ MA3 瀵圭収銆?
