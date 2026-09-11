# M7-2 开发栈环境冒烟回报

**时间**：2026-09-11 17:27（UTC+8）  
**范围**：仅 `/opt/community-dev`（prod 同步暂缓）  
**结论**：环境就绪（冒烟通过，非仅容器 Up）

## CI

- 凭据纪律修正提交：`91680e9` `fix(deploy): MinIO 凭据仅从 .env 注入，去掉 compose 默认口令`
- 此前 M7-2 实现 + 部署修复 CI（含 backend 上传用例）：[34583360307](https://github.com/Downer289034/GeekStack/actions/runs/34583360307) success  
- 本轮 push CI：见下方「CI 链接」（回报时以最新 run 为准）

## 凭据与端口口径

| 项 | 做法 |
|---|---|
| MinIO root | 仅服务器 `/opt/community-dev/.env`（chmod 600）；compose 使用 `${MINIO_ROOT_USER}` / `${MINIO_ROOT_PASSWORD}`，**无默认口令** |
| 9000 | 暴露，供本机/调试客户端直连 MinIO API |
| 9080 | nginx `/media/` → MinIO `geekstack/media/`；**前端联调只走 9080**（模拟生产浏览器不直连 MinIO） |

## 冒烟步骤与证据

1. 生成 1×1 PNG，经 `mc`（容器网络）写入 `geekstack/media/smoke/...-m72.png`
2. 本机/服务器 `curl http://127.0.0.1:9080/media/smoke/...-m72.png` 读回

| 检查项 | 结果 |
|---|---|
| HTTP | **200** |
| 字节 | **69** |
| `file` | PNG image data, 1 x 1, 8-bit/color RGB |
| 魔数 | `89 50 4e 47 0d 0a 1a 0a` |
| 公开 URL | http://172.25.41.137:9080/media/smoke/20260911-172711-m72.png |

读回样本（经 9080 拉取，非直连 9000）：

![via-9080 smoke PNG](./assets/m7-2-smoke/via-9080.png)

原始记录：`docs/assets/m7-2-smoke/result.txt`

## 联调入口（给 M7-1）

- 上传 API：`POST /api/v1/media/upload`（JWT + multipart `file`）
- 插入 Markdown：`![](${MINIO_PUBLIC_BASE_URL}/media/yyyy/MM/uuid.ext)`，`MINIO_PUBLIC_BASE_URL=http://172.25.41.137:9080`
