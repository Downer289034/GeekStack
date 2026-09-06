# Java & AI 交流社区 — 技术架构与产品规划方案

> 版本：v1.0 ｜ 编制角色：高级项目经理 / 软件系统架构师 ｜ 日期：2026-09-06

---

## 一、需求结构化

### 1.1 核心功能（MVP 必须实现）

| 编号 | 功能     | 原始需求描述                         | 说明                                        |
| -- | ------ | ------------------------------ | ----------------------------------------- |
| C1 | 用户系统   | 用户注册、登录、个人主页                   | PC 端 Web，账号体系是一切功能的地基                     |
| C2 | 代码仓库绑定 | "可以在社区上挂 github 和 gitee 等主流仓库" | OAuth 授权绑定 GitHub / Gitee，个人主页展示仓库主页与项目链接 |
| C3 | 项目分享   | "社区分享日常项目，可以看到分享的项目介绍和地址"      | 发布项目卡片：名称、介绍、技术栈、仓库地址、在线地址（可选）            |
| C4 | 技术论坛   | "Java\&AI 的论坛可以发布疑问和好玩的技术"     | 发帖、回帖、分类（Java / AI / 问答 / 资源），支持 Markdown |

### 1.2 扩展功能（后置，架构预留插槽）

| 编号 | 功能                        | 触发条件            |
| -- | ------------------------- | --------------- |
| E1 | 管理端 Web（内容审核、用户管理、数据看板）   | 用户端社区成熟后开发      |
| E2 | 互动增强：点赞、收藏、关注、消息通知        | M2-M3 迭代        |
| E3 | 全文搜索（帖子/项目/用户）            | 内容量上来之后         |
| E4 | 鸿蒙原生端（ArkUI）或移动端适配        | PC 端稳定后         |
| E5 | 更多仓库平台（GitLab、Codeberg 等） | OAuth 模块抽象后按需接入 |

### 1.3 非功能性需求

- **性能**：首页首屏 ≤ 2s（CDN + 接口缓存）；列表接口分页，禁止全表扫描。
- **安全**：OAuth Token 加密存储（AES-GCM）；接口鉴权（JWT）；防 SQL 注入 / XSS（Markdown 渲染必须 sanitize）；密码 BCrypt 加密。
- **可维护性**：统一代码规范（Checkstyle + ESLint/Prettier）；统一响应体、全局异常处理、结构化日志。
- **可扩展性**：管理端不开发，但权限模型（RBAC）从第一天就设计进用户表；预留事件发布点和后台任务队列。
- **资源现实**：独立开发/小团队，**初期单体架构**，不上微服务、不上 K8s，一台 2C4G 云服务器 + MySQL + Redis 即可起步。

---

## 二、系统架构概述（文字描述版）

**总体形态：模块化单体（Modular Monolith）+ 前后端分离。**

```
┌─────────────────────────────────────────────────────┐
│  表现层  Vue3 SPA（PC Web）                          │
│  Vue3 + Vite + Pinia + Vue Router + 定制鸿蒙风主题    │
└──────────────────────┬──────────────────────────────┘
                       │ HTTPS / RESTful JSON (JWT)
┌──────────────────────▼──────────────────────────────┐
│  接入层  Spring Boot 3 (JDK17)                       │
│  Controller 层：REST API、参数校验、统一响应体、鉴权   │
├─────────────────────────────────────────────────────┤
│  业务层（按模块分包，模块间只通过 Service 接口通信）    │
│  user │ oauth │ share │ forum │ notify │ search      │
│  [事件发布点 ApplicationEvent → 预留通知/审计插槽]     │
├─────────────────────────────────────────────────────┤
│  数据访问层  MyBatis-Plus                            │
│  MySQL 8（业务数据）  Redis（缓存/会话/热榜）          │
└──────────┬───────────────────────────┬──────────────┘
           │ OAuth2                    │ 异步任务（预留）
    GitHub / Gitee OpenAPI        本地任务队列 / 消息通知
```

**各层职责与通信方式：**

1. **表现层**：Vue3 单页应用，只负责渲染与交互，不含业务逻辑。所有数据通过 RESTful API 获取，JWT 放在 `Authorization: Bearer` 头。前端构建产物 Nginx 托管，与后端同域或反向代理，避免 CORS 复杂化。
2. **接入层**：Spring Boot Controller 只做参数校验与编排，禁止写业务逻辑。全局统一响应体 `Result<T>`、全局异常处理器、Spring Security 过滤链完成 JWT 鉴权。
3. **业务层**：**按业务域分包**（`com.community.user`、`com.community.share`、`com.community.forum`……），这是整个架构最重要的决策——包结构即未来拆分的边界。模块之间不允许直接访问对方 Mapper，只能调用对方 Service 接口；跨模块的"副作用"（如"发帖后通知关注者"）通过 Spring `ApplicationEvent` 发布事件解耦——这就是未来接入管理端、通知系统的预留插槽。
4. **数据访问层**：MySQL 8 单库，MyBatis-Plus 提供 CRUD 与分页；Redis 缓存首页项目列表、热帖排行，兼作 OAuth state 与刷新令牌存储。

**演进式架构预留点（当前不实现，只留插槽）：**

| 预留点       | 做法                                                  | 服务于           |
| --------- | --------------------------------------------------- | ------------- |
| RBAC 权限模型 | 用户表带 `role` 字段 + 权限注解 `@PreAuthorize`               | 管理端 E1        |
| 事件总线      | `ApplicationEventPublisher` 发布 `PostCreatedEvent` 等 | 通知、审计、搜索索引    |
| 后台任务      | 统一封装异步任务入口（线程池 + 任务表）                               | 定时拉取仓库数据、通知投递 |
| 第三方仓库适配器  | `RepoProvider` 接口 + GitHub/Gitee 两个实现               | E5 更多平台       |
| API 版本化   | 全部接口挂 `/api/v1/**`                                  | 未来移动端/鸿蒙端复用   |

---

## 三、功能模块清单与说明

### 3.1 用户模块（user）

- **输入**：注册表单（用户名/邮箱/密码）、登录凭据、个人资料编辑。
- **核心操作**：注册、登录、JWT 刷新、查看/编辑个人主页、上传头像。
- **输出**：用户公开主页（昵称、简介、技术栈标签、绑定的仓库、分享的项目、发布的帖子）。
- **数据流向**：`user` 表；头像存本地磁盘 + Nginx 静态服务（初期不上 OSS）。

### 3.2 仓库绑定模块（oauth）

- **输入**：用户点击"绑定 GitHub / Gitee"→ OAuth2 授权码流程回调。
- **核心操作**：发起授权、回调换 Token、解密存储、拉取用户公开仓库列表、个人主页展示仓库卡片（名称/描述/Star/链接）。
- **输出**：`user_oauth_binding`（加密 token）、`user_repo`（仓库快照）。
- **数据流向**：调 GitHub/Gitee OpenAPI → 落库 → 前端主页聚合展示。**Token 用 AES-GCM 加密后入库，密钥放环境变量，绝不进代码库。**

### 3.3 项目分享模块（share）

- **输入**：项目名称、一句话介绍、详细说明（Markdown）、技术栈标签、仓库地址、在线 Demo 地址（可选）、封面图（可选）。
- **核心操作**：发布 / 编辑 / 下架项目分享、浏览项目广场（分页 + 按标签/技术栈筛选）、项目详情页、跳转仓库地址。
- **输出**：`project_share` 表记录；项目广场列表、详情页。

### 3.4 论坛模块（forum）

- **输入**：帖子标题、分类（Java/AI/问答/资源）、正文（Markdown）、回帖内容。
- **核心操作**：发帖 / 编辑 / 删除、分类浏览、回帖（两级：回帖 + 楼中楼）、按最新/最热排序。
- **输出**：`post`、`comment` 表记录；论坛列表页、帖子详情页。
- **注意**：Markdown 渲染前端必须做 XSS sanitize（DOMPurify）。

### 3.5 支撑模块（横切）

- `notify`（M3）：基于事件总线接收 `PostCreatedEvent` 等，先只实现站内信。
- `search`（M3+）：初期 MySQL `LIKE` + 索引兜底，预留 SearchService 接口，量大后换 Elasticsearch，调用方无感。

---

## 四、技术选型与实现要点

| 层面          | 选型                                                                                                                                    | 关键注意事项                                                                                                                  |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| 后端语言        | JDK 17（虚拟线程可用但初期不必）                                                                                                                   | 明确 LTS，享受 record / sealed 语法                                                                                            |
| 后端框架        | Spring Boot 3.3.x                                                                                                                     | Spring Security 6 + OAuth2 Client 做 GitHub/Gitee 登录与绑定                                                                  |
| ORM         | MyBatis-Plus 3.5.x                                                                                                                    | 团队上手快；分页插件统一配置                                                                                                          |
| 数据库         | MySQL 8.0（utf8mb4）                                                                                                                    | 所有表含 `created_at/updated_at/deleted`（逻辑删除）                                                                              |
| 缓存          | Redis 7                                                                                                                               | 首页缓存 60s、OAuth state 5min 过期、热榜 ZSet                                                                                    |
| 前端框架        | Vue 3.4 + Vite 5 + TypeScript                                                                                                         | Composition API + `<script setup>`                                                                                      |
| 状态/路由       | Pinia + Vue Router 4                                                                                                                  | 路由守卫做 JWT 校验                                                                                                            |
| UI 方案       | **PC Web：Vue3 + Naive UI（或 Element Plus）深度定制"鸿蒙设计风"主题**（HarmonyOS Design 的色彩/圆角/字距 token）；**鸿蒙原生端为 E4 扩展项，届时用 ArkUI 直连接口或复用 /api/v1** | 需要说明：鸿蒙 ArkUI 是原生开发框架，无法直接嵌在 Vue3 PC 网页里。因此落地策略 = PC 端 Vue3 采用鸿蒙设计语言（视觉一致），真鸿蒙端作为后续阶段复用同一套 API。这与你的技术栈要求不冲突，且避免一开始就过度设计 |
| Markdown 渲染 | 前端 markdown-it + DOMPurify                                                                                                            | 后端存储原文，渲染在前端做                                                                                                           |
| 文件存储        | 初期本地磁盘 + Nginx；预留 OSS 接口                                                                                                              | 封装 `StorageService` 接口，后续切云存储只改实现                                                                                       |
| 部署          | 云服务器 1 台：Nginx + Spring Boot jar + MySQL + Redis（Docker Compose）                                                                      | 不上 K8s；GitHub Actions 自动构建                                                                                              |

**第三方集成要点（GitHub/Gitee OAuth）：**

1. 使用 Spring Security OAuth2 Client 双 Client 注册，`scope` 最小化（GitHub: `read:user user:email`，Gitee: `user_info projects`）。
2. 绑定流程：已登录用户 → `/oauth2/authorization/github` → 回调 → 按"当前登录用户 + provider + openid"落绑定表；未登录时回调即视为第三方登录。
3. Token 加密（AES-GCM，密钥来自环境变量）；GitHub token 有效期长但遵守最小权限，Gitee token 需处理刷新。
4. 仓库列表通过 `RepoProvider` 适配器接口拉取，失败降级为只展示用户手填的仓库链接。

---

## 五、数据库核心表结构设计

```sql
-- 用户
user(id PK, username UNIQUE, email UNIQUE, password_hash NULL,   -- 第三方登录用户可为空
     nickname, avatar_url, bio, tech_stack JSON, role ENUM('USER','ADMIN'),
     status, created_at, updated_at, deleted)

-- 第三方账号绑定（核心：一个用户可绑多个平台）
user_oauth_binding(id PK, user_id FK, provider ENUM('GITHUB','GITEE'),
     openid, access_token_enc, refresh_token, expires_at, created_at,
     UNIQUE(provider, openid))

-- 绑定的仓库快照（用于主页展示，定时/登录时刷新）
user_repo(id PK, user_id FK, provider, repo_full_name, repo_url,
     description, stars, pushed_at, synced_at)

-- 项目分享
project_share(id PK, user_id FK, title, summary, content_md, cover_url,
     repo_url, demo_url, tags JSON, view_count, status ENUM('PUBLISHED','HIDDEN'),
     created_at, updated_at, deleted)

-- 论坛帖子
post(id PK, user_id FK, category_id FK, title, content_md,
     view_count, reply_count, last_reply_at, is_pinned, is_hot,
     created_at, updated_at, deleted)

-- 帖子分类（预置：Java / AI / 问答 / 资源分享）
post_category(id PK, name, slug, sort_order)

-- 评论（支持帖子和项目分享两种宿主，两级结构）
comment(id PK, target_type ENUM('POST','SHARE'), target_id,
     user_id FK, parent_id NULL, content_md, like_count,
     created_at, deleted)

-- 站内通知（M3，表先建好，事件驱动写入）
notification(id PK, user_id FK, type, title, content, is_read, created_at)
```

**关系说明**：user 1—N oauth_binding / repo / project_share / post / comment；comment 通过 `target_type + target_id` 多态挂载到帖子或项目分享，避免建两张评论表；索引必建：`post(category_id, created_at)`、`project_share(status, created_at)`、`comment(target_type, target_id)`。

---

## 六、项目开发路线图

```
M0 立项奠基（第 1 周）
 └ 脚手架：后端工程初始化/统一响应体/全局异常/代码规范；前端工程初始化/主题定制/路由骨架；数据库建表
M1 MVP 上线（第 2–5 周）
 ├ 用户注册/登录/JWT
 ├ 项目分享：发布 + 项目广场 + 详情页
 ├ 论坛：发帖 + 分类浏览 + 两级回帖
 └ 部署上线：Docker Compose + HTTPS，邀请种子用户内测
M2 仓库生态（第 6–8 周）
 ├ GitHub/Gitee OAuth 登录与绑定
 ├ 个人主页 v2：仓库卡片 + 技术栈展示
 └ 项目广场支持"从我的仓库一键导入"
M3 社区活力（第 9–11 周）
 ├ 点赞/收藏、站内通知（事件总线驱动）
 ├ 热帖榜单（Redis ZSet）、MySQL 级搜索
 └ 性能专项：首页缓存、慢 SQL 优化
M4 管理端接入（用户端成熟后）
 └ 直接启用预留的 RBAC + 独立管理前端工程调 /api/v1，不改动用户端
```

**关键里程碑验收标准**：M1 结束时真实用户可完成"注册 → 分享一个项目 → 在论坛发帖回帖"完整闭环并可公网访问。

---

## 七、风险与建议

| 风险                    | 应对                                                    |
| --------------------- | ----------------------------------------------------- |
| 鸿蒙 UI 期望与 PC Web 现实不符 | 本方案已明确：PC 用 Vue3 + 鸿蒙设计语言主题；真鸿蒙端作为 E4 后置。请与相关方确认此理解一致 |
| 独立开发精力有限，范围蔓延         | 严格执行本路线图，任何新想法进"扩展功能池"，MVP 期间不做                       |
| Gitee API 限流/不稳定      | 仓库数据做快照落库，接口失败时展示旧快照                                  |
| Markdown XSS          | 渲染统一走前端 DOMPurify 白名单，后端只存原文                          |
| 单体变"大泥球"              | 用包结构守住模块边界 + Checkstyle 强制依赖规则，这是未来一切扩展的底线            |
