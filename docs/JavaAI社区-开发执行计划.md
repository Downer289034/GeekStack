# Java & AI 交流社区 — 开发执行计划（单人开发版）

> 角色分工：**PM（我）**负责任务下发、评审把关、技术决策；**开发者（你）**负责按任务完成代码。
> 配套文档：《架构与产品规划》《开发任务清单》《质量评审与上线评估标准》（均在 docs/ 下）
> 说明：不写工期，按依赖关系排序，**顺序即优先级**，完成一个再开下一个。

---

## 一、协作机制（工作流）

```
我下发任务 → 你实现（git commit）→ 你回报"Task N 完成" → 我按验收标准评审
     ↑                                              │
     └────── P0 问题打回返工 ←──── 通过则进入下一任务 ←┘
```

- **每完成一个任务 = 一次 git commit**，commit 信息格式：`feat(share): 项目分享发布接口 (#T9)`
- **里程碑节点（M0/M1/M2/M3 完成）必须停下，交我做阶段评审**，评审通过才继续
- 技术上卡住超过半天，直接回报，由我给替代方案，不自行绕路
- 任何任务清单之外的想法，只报给我，记入扩展功能池，不进开发

---

## 二、技术栈总表（照此安装，版本以此为准）

### 后端
| 类别 | 组件 | 版本 | 用途 |
|------|------|------|------|
| 语言 | JDK | 17 | LTS |
| 框架 | Spring Boot | 3.3.x | Web + Security + 事件 |
| 安全 | Spring Security 6 + OAuth2 Client | 随 Boot | JWT 鉴权、GitHub/Gitee 登录 |
| JWT | jjwt | 0.12.x | 令牌签发/校验 |
| ORM | MyBatis-Plus | 3.5.x | CRUD/分页 |
| 数据库 | MySQL | 8.0（utf8mb4） | 业务数据 |
| 迁移 | Flyway | 随 Boot | 建表脚本版本化 |
| 缓存 | Redis | 7 | 缓存/state/热榜 |
| 校验 | Jakarta Validation + hibernate-validator | 随 Boot | 参数校验 |
| 工具 | Lombok | 随 Boot | 消样板代码 |
| 规范 | Checkstyle（Google 变体） | 10.x | 代码规范 |
| 构建 | Maven | 3.9+ | 构建 |

### 前端
| 类别 | 组件 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Vue | 3.4+（`<script setup>`） | SPA |
| 构建 | Vite | 5.x | 构建/dev server |
| 语言 | TypeScript | 5.x | 类型安全 |
| 状态 | Pinia | 2.x | 登录态/全局状态 |
| 路由 | Vue Router | 4.x | SPA 路由 + 守卫 |
| HTTP | Axios | 1.x | 请求封装 + JWT 拦截器 |
| UI | Naive UI | 2.x | 组件库，深度定制鸿蒙设计风主题 |
| 编辑器 | md-editor-v3 | 最新 | Markdown 编辑+预览 |
| 渲染 | markdown-it + DOMPurify | 最新 | 渲染 + XSS 消毒 |
| 规范 | ESLint + Prettier | 最新 | 代码规范 |

### 基础设施
Docker + Docker Compose ｜ Nginx（静态托管 + 反代）｜ Let's Encrypt（certbot）｜ GitHub Actions（CI）

---

## 三、技术架构（落地版）

**模块化单体 + 前后端分离**。分层与模块边界详见《架构与产品规划》第二、三节，此处给落地目录结构。

### 后端目录（包结构即未来扩展边界，必须守住）
```
com.community
├── common/            # Result、全局异常、JWT 工具、Redis 配置、加密工具
├── config/            # Security 配置、OAuth2 Client 注册、MyBatis-Plus、CORS
├── user/              # controller / service / mapper / entity
├── oauth/             # 含 RepoProvider 接口 + github/ gitee/ 两个实现
├── share/
├── forum/
├── notify/            # 仅事件监听器实现，M3 前只有包骨架
└── event/             # 所有 ApplicationEvent 定义（跨模块通信唯一通道）
```
**铁律**：模块间只准调用对方 Service 接口 + 发事件；Controller 不写业务；删除一律逻辑删除。

### 前端目录
```
src/
├── api/               # 按模块分文件的请求封装（user.ts / share.ts / forum.ts）
├── stores/            # Pinia（auth.ts 等）
├── router/            # 路由 + 守卫
├── theme/             # 鸿蒙设计风 token（色彩/圆角/字距，独立文件可整体换肤）
├── layouts/           # 顶部导航 + 内容区 + 页脚
├── views/             # 按页面分目录（home/ share/ forum/ profile/ auth/）
├── components/        # 项目卡片、帖子卡片、评论树等
└── utils/             # markdown 渲染（统一过 DOMPurify）
```

### 部署拓扑（一台服务器）
Nginx（443/80，托管前端产物 + 反代 `/api`）→ Spring Boot jar（Docker）→ MySQL 8 / Redis 7（Docker，数据卷持久化，MySQL 每日备份到对象存储）。

---

## 四、开发顺序（按依赖排列，编号对应任务清单）

### 第 0 步：环境准备（开工前）
- 安装 JDK17、Maven、Node 20+、Docker Desktop、IDEA/VSCode
- 建 git 仓库（后端、前端两个仓库或 monorepo 均可），建 `main` + 开发分支
- 本地起 MySQL 8 + Redis 7（docker run 即可），建库 `community`
- **✅ 完成标准：`java -version`、`node -v`、`docker ps` 全部正常，仓库可 push**

### 阶段 A：奠基（对应 M0，Task 1–6）
按序执行：**T1 后端骨架 → T2 统一响应体/全局异常 → T3 Flyway 建表 → T4 前端骨架+请求封装 → T5 页面骨架路由 → T6 CI**
- T6 提前做 Checkstyle/ESLint 配置，之后每个任务都被 CI 管着
- **🚩 检查点 A（交我评审）：骨架评审**——我检查包结构、响应体规范、建表脚本与规划第五节一致性、前端主题 token 独立性。通过后进入阶段 B。

### 阶段 B：MVP（对应 M1，Task 7–14）
按序执行：**T7 注册登录 API → T8 登录注册页 → T9 项目发布 API+表单 → T10 项目广场+详情 → T11 论坛 API → T12 论坛页+两级回帖 → T13 个人主页 v1 → T14 部署上线**
- T7/T8 完成后登录态贯通，T9 起所有接口都带鉴权测试
- T14 是硬节点：Docker Compose 一键拉起 + HTTPS + **备份脚本并做一次恢复演练** + ICP 备案提前启动（周期 1–3 周，别等开发完才办）
- **🚩 检查点 B（交我评审）：MVP 全量评审 + 上线 Go/No-Go**——按《质量评审与上线评估标准》第二、四节执行，含安全一票否决项。

### 阶段 C：仓库生态（对应 M2，Task 15–18）
按序执行：**T15 GitHub/Gitee OAuth → T16 RepoProvider 适配器+仓库快照 → T17 主页 v2 → T18 一键导入**
- T15 需要先去 GitHub/Gitee 开发者后台注册 OAuth App，回调地址用正式域名
- **🚩 检查点 C（交我评审）：增量评审**——重点查 Token 加密实现和降级逻辑。

### 阶段 D：社区活力（对应 M3，Task 19–22）
按序执行：**T19 点赞收藏 → T20 事件总线+站内通知 → T21 热榜+首页缓存 → T22 压测+慢 SQL 优化**
- **🚩 检查点 D（交我评审）：运行复盘评审**——对照上线评估标准复核生产表现。

### 阶段 E：管理端（M4，暂不启动）
触发条件：用户端成熟 + 用户量起来。届时 RBAC 已就绪，只需新建管理前端工程。

---

## 五、开发纪律（开发者承诺清单）

1. 每任务一 commit、一分支（`feature/task-9-share-api`），合入 main 前自测验收标准
2. `.env` / 密钥 / Token 永不进 git；新凭据一律环境变量
3. 不引入清单外依赖（想加库先问我）；不提前做 M2 之后的功能
4. 每完成一个任务向我回报：`Task N 完成 + commit hash + 一句自测结论`
5. 遇到需求歧义以《架构与产品规划》原文为准，仍有疑问直接问我，不自行脑补

---

## 六、里程碑与我的评审动作对照

| 检查点 | 触发时机 | 我的动作 |
|--------|---------|---------|
| A 骨架评审 | 阶段 A 完成 | 包结构/规范/建表核对，轻量，当天出结论 |
| B MVP 评审 | T14 完成 | 全量评审 + 上线 Go/No-Go，出正式评审报告 |
| C 增量评审 | T18 完成 | OAuth 安全 + 降级逻辑专项 |
| D 复盘评审 | T22 完成 | 生产运行复盘 + M4 规划建议 |

现在就可以开工：**先做第 0 步环境准备，然后从 Task 1 开始。** 每完成一步回报我即可。
