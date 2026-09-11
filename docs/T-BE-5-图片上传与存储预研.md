# T-BE-5 图片上传与存储预研（只研不做）

> 产出日期：2026-09-11 ｜ 任务：T-BE-5（T-BE-4 预研模式）｜ 状态：**方案文档，无实现**  
> 对齐输入：`docs/JavaAI社区-架构与产品规划.md`（StorageService / 本地磁盘预留 OSS）、`docs/geekstack-editor-design-spec.md` §4.3、现状 `ProjectShare.coverUrl` / `User.avatarUrl`、双栈 Docker 拓扑  
> **硬约束**：待 PM 批本文后，再立正式实现任务；本文不得被解读为已批准开工。

---

## 一、现状盘点（代码实物）

| 能力 | 现状 | 含义 |
|------|------|------|
| 分享封面 | 实体 `ProjectShare.coverUrl` 有列；**`SharePayload` 无 cover 字段** | 库能存 URL，发布/编辑 API **尚未接通**封面 |
| 用户头像 | `User.avatarUrl`；OAuth 写入第三方头像 URL | 无本地上传入口；展示侧多为首字母占位 |
| 上传接口 | **无** `multipart` Controller / 无 `StorageService` | 编辑器「粘贴/拖拽上传」与封面上传均阻塞 |
| 架构预设 | 「初期本地磁盘 + Nginx；封装 StorageService，后续切云」 | 与「可替换对象存储」同向；MinIO = S3 兼容实现，比裸磁盘更贴插槽 |
| 编辑器规格 | 单图 ≤10MB；jpg/png/webp/gif；成功插入 `![](url)` | 前端契约已有，等后端 URL 出口 |
| 部署 | prod：Nginx + backend + mysql + redis；**无对象存储容器** | 加 MinIO 需改 compose + Nginx 反代/直出策略 |
| 演示数据 | seed 用 `picsum.photos` 外链当封面 | 视觉验收可走外链；正式能力需自建存储 |

架构原文倾向「本地磁盘」，但同时要求 **StorageService 接口防锁死**。在现有 Docker 四容器拓扑上，**MinIO 比裸挂载目录更贴「OSS 插槽」**（S3 API、桶策略、可迁真实 OSS），且仍可自托管、零云账单——本文按 **MinIO Docker 方案** 展开（非否定本地磁盘，对比见第二节）。

---

## 二、存储选型：本地磁盘 vs MinIO（Docker）

### 2.1 对比

| 维度 | 本地磁盘 + Nginx 静态 | MinIO（S3 兼容，Docker） |
|------|----------------------|---------------------------|
| 与架构 | 字面「初期本地磁盘」 | 字面「预留 OSS」；`StorageService` 以 S3 API 实现，切阿里云/R2 只换 endpoint |
| 运维 | 简单；备份=拷目录 | 多一容器；卷备份；需 root 用户密钥进 `.env` |
| 权限/桶 | 自管路径约定 | Bucket + 策略；可公私桶分离（公有读封面 vs 私有草稿） |
| 直链 | Nginx `location /uploads/` | Nginx 反代 MinIO **或** 预签名 URL；公有桶可永久 URL |
| 体量 | 单机够用 | 同单机；API 形态利于以后水平扩展 |
| 开发体验 | 本机路径差异（Win/WSL） | 与 MySQL/Redis 一样 `docker compose`；dev/prod 对称 |

### 2.2 建议（预研倾向，非开工令）

1. **一期上 MinIO（Docker）**，后端只依赖 `StorageService`（put / delete / publicUrl），SDK 用 AWS S3 兼容客户端（MinIO 官方或 AWS SDK v2）。  
2. **不做**「先本地磁盘再迁 MinIO」两段实现——多一次迁移与路径分叉；架构「本地磁盘」视为时代背景，插槽语义以 StorageService 为准。  
3. **桶策略一期：公有读、私有写**——上传必须经后端鉴权；读封面/正文图走稳定 HTTPS URL（Nginx 反代），避免前端持有 MinIO 密钥。  
4. **明确不做（当前）**：图片 CDN、服务端缩略图流水线、病毒扫描（可列后续）。

---

## 三、MinIO Docker 部署方案（草案）

### 3.1 拓扑（贴合现有双栈）

| 栈 | 建议 | 端口暴露 |
|----|------|----------|
| **prod** `/opt/community` | 新增 `geekstack-minio`；**不对外暴露** 9000/9001，仅 Docker 网络；Nginx 反代公开读路径 | `expose: 9000` |
| **dev** `/opt/community-dev` | 新增 `community-minio`；可选对本机暴露 `9000` 便于排障（控制台 9001 仅内网） | 开发可 `9000:9000`，控制台默认不映射或绑 `127.0.0.1` |

与 MySQL 同纪律：**生产对象存储 API 不直接打公网**；浏览器只打本站域名下的 `/media/`（或 `/uploads/`）。

### 3.2 Compose 片段（示意，非落地文件）

```yaml
# 建议服务名：minio；镜像：minio/minio:RELEASE.*（钉死具体 tag，禁止 latest）
minio:
  image: minio/minio:RELEASE.2024-xx-xxTxx-xx-xxZ   # 实施时钉死
  container_name: geekstack-minio                   # dev: community-minio
  command: server /data --console-address ":9001"
  environment:
    MINIO_ROOT_USER: ${MINIO_ROOT_USER}
    MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}      # ≥8 字符，.env chmod 600
  volumes:
    - minio-data:/data
  expose:
    - "9000"
  # prod 不映射 9001 到宿主机；运维用 docker exec / 内网
  healthcheck:
    test: ["CMD", "mc", "ready", "local"]           # 或 curl /minio/health/live
    interval: 10s
    timeout: 5s
    retries: 12
```

初始化（一次性，实施脚本）：

1. 创建 bucket：`geekstack`（名可配置）。  
2. 匿名只读前缀策略：`media/*` GetObject 允许；其余拒绝。  
3. 应用使用 **独立 IAM 风格用户**（非 root）：仅 `PutObject`/`DeleteObject`/`GetObject` 限定桶——root 仅 bootstrap。  
4. 凭据进 `/opt/community/.env` 与本机 `.env`：`MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET`、`MINIO_PUBLIC_BASE_URL`（对外 URL 前缀）。

### 3.3 Nginx 公开读（推荐）

在现有 `geekstack.conf` 增加：

```nginx
# 对象只读反代（浏览器不直连 MinIO）
location /media/ {
    proxy_pass http://minio:9000/geekstack/media/;
    proxy_set_header Host $host;
    # 可选：缓存头、禁 cookie 转发
}
```

后端写入对象键：`media/{yyyy}/{MM}/{uuid}.{ext}`；对外返回：`{PUBLIC_BASE}/media/{yyyy}/{MM}/{uuid}.{ext}`。  
`PUBLIC_BASE`：prod = 站点源（`https://域名` 或当前 `http://172.25.41.137`）；dev = `http://172.25.41.137` 或本机反代。

### 3.4 备份

- 卷 `minio-data` 纳入现有备份叙事（与 mysql 卷并列）；一期可 **日拷卷或 `mc mirror`**。  
- 恢复演练与 DB 分开列项（实施任务验收）。

### 3.5 CI

- GitHub Actions **默认不起 MinIO**；上传集成测用 **Testcontainers MinIO** 或 mock `StorageService`。  
- 单元测钉死：类型拒绝 / 超限 / 未登录 401——不依赖真 MinIO。

---

## 四、上传接口契约草案

### 4.1 路径与鉴权

| 项 | 草案 |
|----|------|
| 方法/路径 | `POST /api/v1/media/upload` |
| Content-Type | `multipart/form-data`，字段名 **`file`**（单文件） |
| 鉴权 | **必须登录**（JWT）；匿名 → 401（对齐现有写接口） |
| 可选 query/form | `scene=cover\|avatar\|editor`（可选；影响大小上限或前缀，见安全节） |
| 成功 | `200` + `Result<{ url, key, contentType, size }>` |
| 失败 | `400` 类型/大小/空文件；`401` 未登录；`413` 可选（若 Nginx/`max-file-size` 先拒） |

**不在一期**：

- 多文件批量上传  
- 分片/断点续传  
- 直接浏览器→MinIO 预签名 PUT（密钥面更大；二期可作编辑器加速）

### 4.2 响应示例（形态）

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "url": "http://172.25.41.137/media/2026/09/a1b2c3d4e5f6.webp",
    "key": "media/2026/09/a1b2c3d4e5f6.webp",
    "contentType": "image/webp",
    "size": 204800
  }
}
```

前端：编辑器成功后插入 `![](url)`；分享封面把 `url` 写入后续接通的 `coverUrl` 字段（**另立小任务**：`SharePayload` 增 `coverUrl` + 校验为本站 `/media/` 前缀或 https）。

### 4.3 与现有模块边界

| 层 | 建议 |
|----|------|
| 包 | `com.community.media`（或 `storage`）：Controller + `StorageService` 接口 + `MinioStorageService` |
| 禁止 | Controller 直操 MinIO SDK；其它模块禁止 import MinIO 客户端 |
| 消费方 | share（封面）、user（头像，可二期）、forum/editor（Markdown 插图）只拿 URL 字符串 |

### 4.4 测试纪律（实施时）

正/反/越权三路径最低集：

1. 登录 + 合法 png → 200 且 url 非空  
2. 未登录 → 401  
3. 非法扩展名（如 `.html` / 伪装）→ 400  
4. 超大小 → 400  
5. 空 body → 400  

可选：mock StorageService 断言 put 被调用；Testcontainers 一条冒烟。

---

## 五、安全约束（硬条件草案）

### 5.1 类型白名单

| 规则 | 草案 |
|------|------|
| 允许扩展名 | `jpg` / `jpeg` / `png` / `webp` / `gif`（小写归一） |
| 允许 Content-Type | `image/jpeg`、`image/png`、`image/webp`、`image/gif` |
| 校验顺序 | **① 扩展名白名单 → ② 声明的 Content-Type 白名单 → ③ 魔数（文件头）嗅探** 三者一致才接收 |
| 拒绝 | svg（XSS 面）、html/js、无扩展名、双扩展名（`x.jpg.html`）、Content-Type 与魔数不符 |

魔数：至少识别 JPEG/PNG/WebP/GIF 头；实施可用轻量库或手写前几字节。

### 5.2 大小限制

| scene | 上限（对齐编辑器规格） |
|-------|------------------------|
| `editor`（默认） | **10 MiB** |
| `cover` | **5 MiB**（封面无需原图） |
| `avatar` | **2 MiB** |

另：

- Spring：`spring.servlet.multipart.max-file-size` / `max-request-size` ≥ 场景最大值（建议统一 **10 MiB**，场景在业务层再收紧）。  
- Nginx：`client_max_body_size 10m;`（`/api/` location）。  
- 超限优先业务 400 文案清晰；网关 413 作为兜底。

### 5.3 文件名与对象键

| 规则 | 草案 |
|------|------|
| **丢弃**用户原始文件名 | 不入库、不拼进对象键（路径穿越 / 怪异 Unicode / 探测） |
| 对象键 | `media/{yyyy}/{MM}/{uuid}.{ext}`；`ext` 来自白名单映射，非用户字符串 |
| UUID | 标准 UUID 去横线或 `UUID.randomUUID()` 字符串 |
| 禁止 | `../`、绝对路径、用户 id 可枚举连续序号做唯一名（可用 userId 作**前缀审计**，不可作唯一熵源） |
| 可选审计前缀 | `media/{userId}/{yyyy}/{MM}/{uuid}.{ext}`——便于按用户清理；**须防**仅靠递增 userId 撞库枚举（公有读下 UUID 仍是主熵） |

响应里的 `url` 只含生成键；原始 `Filename` 头忽略。

### 5.4 其它

- 上传接口 **不** `permitAll`。  
- 不信任客户端压缩声明；不执行图片内容当 HTML。  
- 日志：可记 `userId + key + size + contentType`；**不**记完整本地临时路径中的原始名。  
- 密钥：MinIO 凭据与 JWT 同级红线——不进 git、不进聊天、不进日志。

---

## 六、和实施相关的周边（非本文开工范围）

| 项 | 说明 |
|----|------|
| `SharePayload.coverUrl` | 实体有、API 无；实施上传后需接通，并限制为本站 media URL（防任意外链钓鱼可另议：一期可允许 https 外链兼容 seed） |
| 编辑器前端 | 规格已写；等 `url` 契约稳定即可对接 T-FE 任务 |
| 孤儿文件 | 上传成功但未引用：一期可接受；二期定时 GC 或「仅发布时上传」 |
| 删除分享/换封面 | 是否删旧对象：一期可不删（简单）；二期异步清理 |

---

## 七、请 PM 拍板的问题

1. **存储**：是否批准一期 **MinIO Docker**（否决则退回本地磁盘 + Nginx）？  
2. **公开读路径**：`/media/` 反代是否作为对外唯一读入口？  
3. **大小**：编辑器 10MB / 封面 5MB / 头像 2MB 是否锁定？  
4. **封面字段**：上传任务是否捆绑 `SharePayload.coverUrl` 接通，还是拆第二个小任务？  
5. **对象键是否带 userId 前缀**（审计 vs 极简路径）？

---

## 八、正式实现任务建议切分（将来，非当前）

仅在本文批复后立项，例如：

1. **T-BE-M1**：prod/dev compose + `.env` + Nginx `/media/` + bucket 初始化脚本  
2. **T-BE-M2**：`StorageService` + `POST /api/v1/media/upload` + 安全校验 + 测试  
3. **T-BE-M3**（可选捆绑）：`SharePayload.coverUrl` + 前端封面/编辑器对接（或交前端任务）

**明确不做（当前）**：代码、Flyway、compose 合入、SDK 依赖、CI 改动。

---

## 九、结论一句话

> 用 Docker 化 MinIO 落实 StorageService/S3 插槽；浏览器只经鉴权上传接口与 Nginx `/media/` 读；类型白名单 + 魔数 + 分场景大小 + UUID 对象键丢弃原始文件名——批后再开工。
