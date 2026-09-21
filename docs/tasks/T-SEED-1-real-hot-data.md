# T-SEED-1 · 真实热数据种子入库（修损坏脚本）

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1 · 演示/联调数据底座  
> 派发：2026-09-20（T-BE-4.2 关单后）

## 背景

工作区已有未跟踪草稿：`deploy/dev/seed-real-hot-data.sql` / `.sh`、`purge-demo-data.sql`、`docs/assets/seed-catalog/`。  
`seed-real-hot-data.sh` 曾被错误处理（`tr -d '\\r'` 之类）**剥掉字母 r**，出现 `eal`/`docke`/`souce` 等损坏，不可直接用。

## 范围

1. **修复** `deploy/dev/seed-real-hot-data.sh`（及若 SQL 同损则一并修）：命令、路径、账号说明可读可跑。  
2. 确认 `purge-demo-data.sql` 只清 `demo_*`（或文档约定前缀），不误删 `real_*` / 业务账号。  
3. 补短 README：`docs/assets/seed-catalog/README.md`（或 deploy/dev 旁）——用法、幂等约定、演示账号（**密码勿写死进 git 明文长文；可用约定口令 + .env 说明**）。  
4. 本机/dev 冒烟一句：purge（可选）→ seed → `GET /api/v1/tags` 或热榜/广场有非 demo 内容。  
5. **独立 commit** 入库上述脚本 + 目录；密钥/本机 `.env` 不进 git。

## 非范围

- 不改搜索/标签业务代码（4.2 已关）  
- 不改前端；不强制重建 prod 镜像（可另单）  
- 不把生产库当实验场

## 验收

1. `bash deploy/dev/seed-real-hot-data.sh`（或文档等价步骤）可执行，无剥字母损坏  
2. README 可独立照做；commit 无 secret  
3. 三句话汇报（含冒烟命令摘要）

## 完成说明（开发 A，2026-09-21）

- 修复 `seed-real-hot-data.sh`（`tr -d '\\r'` 误剥 `r` → `docker`/`source`/`real_*` 已还原）
- `purge-demo-data.sql` 边界注释；`docs/assets/seed-catalog/README.md` 用法/幂等
- SQL 本体未损（仍为 `real_*`）

## 汇报格式

做了什么 / 证据 / 疑问。
