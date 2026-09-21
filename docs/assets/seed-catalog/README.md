# 极客栈真实热门种子（T-SEED-1）

演示/联调用的 **real_*** 账号与热内容目录。与 `demo_*` 占位数据分离；purge 只清 demo，seed 只重写 real。

## 账号（约定口令）

| 用户名 | 用途 |
|--------|------|
| `real_alice` | 开源雷达 / 项目分享 |
| `real_bob` | 论文速读 / 论坛帖 |
| `real_carol` | Skill 策展 |

口令与 demo 种子一致：仓库演示约定为 `password123`（bcrypt 已写入 SQL）。**勿把生产密钥写进本目录**；本机/服务器 MySQL 凭据只放 `.env`（不进 git）。

## 灌库步骤

前置：`community-mysql` 容器 healthy；`MYSQL_PASSWORD` 在 `deploy/dev/.env` 或 `/opt/community-dev/.env`。

```bash
# （可选）只清 demo_* / [DEMO] 标题，不动 real_*
docker exec -i -e MYSQL_PWD="$MYSQL_PASSWORD" community-mysql \
  mysql -ucommunity --default-character-set=utf8mb4 community < deploy/dev/purge-demo-data.sql

# 幂等灌入 real_*（先删同前缀再插入）
bash deploy/dev/seed-real-hot-data.sh
```

Windows：在 Git Bash / WSL 下执行上述 bash；或把 SQL 管道进 `docker exec`（与 `seed-demo-data.sh` 同模式）。

## 冒烟

```bash
# 标签云应出现非 demo 标签
curl -s "http://172.25.41.137/api/v1/tags" | head -c 400

# 或本机后端：curl -s "http://127.0.0.1:8080/api/v1/tags"
# 广场/热帖：GET /api/v1/shares 、GET /api/v1/hot/posts
```

SQL 末尾会打印 `seed-real-hot-data OK` 及 users/posts/shares/skills 计数。

## 幂等与边界

| 脚本 | 作用范围 |
|------|----------|
| `purge-demo-data.sql` | 仅 `demo_*` 用户及其内容 + 标题 `[DEMO]%` 残留；**不删** `real_*` |
| `seed-real-hot-data.sql` | 先清 `real_*` 再插入；**不碰** `demo_*` / 其它业务账号 |

## 目录文件

| 文件 | 说明 |
|------|------|
| `dsh-top50.json` | DSH 仪表盘星标快照 |
| `gen_seed_real.py` | 从快照/清单生成 SQL 的辅助脚本（可选） |
| `../../deploy/dev/seed-real-hot-data.sql` | 实际灌库 SQL |
| `../../deploy/dev/seed-real-hot-data.sh` | 一键灌库入口 |

## 版权与真实性

- 星标数字为公开页面/仪表盘快照，会随时间漂移。
- 帖子仅导读 + 外链，不粘贴论文/博客全文。
- Skill 正文为安装与测试说明，指向原仓库。

## 内容速览

### 项目分享（节选）

- deepseek-ai/deepseek-harness、colleague-skill、anthropics/skills、langchain、vllm、qdrant、DeepSeek-R1 等（完整列表见 SQL / 历史草稿）。

### 论坛帖

标题前缀 `[REAL]`：论文/博客导读（arXiv、HF、Karpathy、Spring AI 等）。

### Skill

指向公开 Agent Skills 仓的安装说明条目。
