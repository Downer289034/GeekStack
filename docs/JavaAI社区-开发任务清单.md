# Java & AI 交流社区 — 开发任务清单

> **规格来源**：《JavaAI社区-架构与产品规划.md》v1.0
> **技术栈**：JDK17 + Spring Boot 3.3 + MyBatis-Plus + MySQL 8 + Redis ｜ Vue3 + Vite + Pinia + 定制鸿蒙风主题
> **范围约束**：仅用户 PC 端社区；**不含管理端**；每个任务可在 30–60 分钟内完成
> **进度标记**：`[ ]` 待办 → `[x]` 完成

---

## M0 立项奠基（第 1 周）

### [ ] Task 1：后端工程初始化
**描述**：创建 Spring Boot 3.3（JDK17）工程，包结构按业务域划分：`user / oauth / share / forum / notify / common`。
**验收标准**：
- 项目可启动，`/actuator/health` 返回 UP
- 包结构符合模块化分层约定（模块间禁止直接互访 Mapper）
**产出**：后端仓库骨架

### [ ] Task 2：统一响应体与全局异常处理
**描述**：实现 `Result<T>`（code/message/data）、全局异常处理器、业务异常类 `BusinessException`。
**验收标准**：任意 Controller 抛异常返回统一 JSON 结构，HTTP 状态码语义正确

### [ ] Task 3：数据库建表脚本
**描述**：按规划文档第五节编写 Flyway 迁移脚本 V1：user、user_oauth_binding、user_repo、project_share、post、post_category、comment、notification。
**验收标准**：`flyway migrate` 一次通过；所有表含 created_at/updated_at/deleted；关键索引齐全

### [ ] Task 4：前端工程初始化
**描述**：Vite + Vue3 + TS + Pinia + Vue Router + Naive UI，定制鸿蒙设计风主题 token（色彩/圆角/字体），搭建 `/api` 请求封装（Axios + JWT 拦截器 + 401 跳转）。
**验收标准**：`npm run dev` 可访问；主题 token 文件独立可换；请求封装带统一错误提示

### [ ] Task 5：页面骨架与路由
**描述**：布局（顶部导航 + 内容区 + 页脚）+ 路由：首页、项目广场、项目详情、论坛列表、帖子详情、个人主页、登录/注册。
**验收标准**：所有路由可跳转，移动端最小宽度 1024 下布局不破（PC 端优先）

### [ ] Task 6：代码规范与 CI
**描述**：后端 Checkstyle + 前端 ESLint/Prettier；GitHub Actions：push 触发 lint + build。
**验收标准**：CI 绿灯；不合规范的提交被拦截

---

## M1 MVP（第 2–5 周）

### [ ] Task 7：用户注册/登录 API
**描述**：用户名+密码注册（BCrypt）、登录签发 JWT（含 refresh）、参数校验、防重复用户名/邮箱。
**验收标准**：注册→登录→带 JWT 访问 `/api/v1/users/me` 成功；错误密码返回统一错误码

### [ ] Task 8：登录/注册页面
**描述**：对接 Task 7 API，表单校验与错误提示，登录态存 Pinia + localStorage。
**验收标准**：注册后自动登录；刷新页面登录态保持；登出清除凭据

### [ ] Task 9：项目分享发布 API + 表单页
**描述**：POST /api/v1/shares（title/summary/content_md/repo_url/demo_url/tags），发布者可编辑/下架；前端 Markdown 编辑器（推荐 md-editor-v3）+ 实时预览。
**验收标准**：未登录发布返回 401；字段校验完整；编辑仅限作者本人

### [ ] Task 10：项目广场与详情页
**描述**：GET /api/v1/shares 分页列表（支持 tags 筛选）+ 详情接口；前端卡片列表 + 详情页（Markdown 渲染 + DOMPurify 消毒）。
**验收标准**：列表分页正确；详情页浏览数 +1；恶意脚本内容被消毒不执行

### [ ] Task 11：论坛发帖与分类 API
**描述**：post CRUD + 预置分类（Java/AI/问答/资源）种子数据；列表按最新/最热（reply_count+view 加权）排序。
**验收标准**：分类过滤正确；删帖为逻辑删除，列表不再出现

### [ ] Task 12：论坛页面与两级回帖
**描述**：论坛列表页 + 帖子详情页；comment 多态挂载 POST，两级结构（回帖+楼中楼）。
**验收标准**：回帖后 reply_count 与 last_reply_at 正确更新；楼中楼缩进展示正确

### [ ] Task 13：个人主页 v1
**描述**：GET /api/v1/users/{username}/profile 返回资料+项目+帖子聚合；前端主页展示三块内容。
**验收标准**：访问任意用户主页能看到其分享与发帖列表；未注册用户名返回 404 页

### [ ] Task 14：部署上线（M1 收口）
**描述**：Docker Compose（Nginx + 后端 jar + MySQL + Redis）部署到云服务器，HTTPS 证书，后端日志落盘。
**验收标准**：公网域名 HTTPS 可访问；"注册→分享项目→论坛发帖回帖"全链路真实走通
**里程碑**：✅ MVP 上线，邀请 5–10 名种子用户内测

---

## M2 仓库生态（第 6–8 周）

### [ ] Task 15：GitHub/Gitee OAuth 登录与绑定
**描述**：Spring Security OAuth2 Client 双 Client 注册；回调处理"未登录=第三方登录 / 已登录=绑定"；Token AES-GCM 加密入库（密钥走环境变量）。
**验收标准**：GitHub 与 Gitee 均可登录、可绑定到已有账号；库中无明文 Token

### [ ] Task 16：仓库适配器与同步
**描述**：`RepoProvider` 接口 + GitHub/Gitee 实现，拉取用户公开仓库写入 user_repo 快照；登录/绑定时触发刷新，失败保留旧快照。
**验收标准**：断网/限流时主页仍能展示上次快照；不同平台数据不串

### [ ] Task 17：个人主页 v2
**描述**：主页增加仓库卡片区（名称/描述/Star/链接）+ 技术栈标签展示。
**验收标准**：绑定后主页自动出现仓库卡片；可手动隐藏某个仓库

### [ ] Task 18：一键导入项目分享
**描述**：项目发布表单支持"从我的仓库选择"，自动填充 title/summary/repo_url。
**验收标准**：选择仓库后表单自动填充且可修改；未绑定用户看不到该入口

---

## M3 社区活力（第 9–11 周）

### [ ] Task 19：点赞与收藏
**描述**：帖子/项目/评论的点赞（Redis 计数 + 定时落库），帖子的收藏。
**验收标准**：重复点赞幂等；计数在 Redis 宕机后不丢（落库兜底）

### [ ] Task 20：站内通知（事件总线）
**描述**：发布 PostRepliedEvent / ShareLikedEvent 等事件，notify 模块消费写 notification 表；前端顶栏红点 + 通知列表。
**验收标准**：回帖/被赞后目标用户收到通知；通知模块与其他模块无直接代码依赖（仅事件）

### [ ] Task 21：热帖榜单与首页缓存
**描述**：Redis ZSet 维护 24h 热帖；首页项目/热榜接口加 60s 缓存。
**验收标准**：首页接口 P95 < 200ms（缓存命中时）；榜单按热度实时变化

### [ ] Task 22：M3 收口压测与慢 SQL 优化
**描述**：对列表/详情接口做基准压测，开启慢 SQL 日志并优化索引。
**验收标准**：核心接口无慢 SQL（>200ms）记录；压测报告归档

---

## M4 管理端接入（用户端成熟后，仅预留，不排期）

- [ ] Task A（预留已就绪）：RBAC 字段与 `@PreAuthorize` 已在 M1 落地，届时新建独立管理前端工程复用 `/api/v1`，用户端零改动。

---

## 质量红线（每个任务提交前自查）

- [ ] 所有接口挂 `/api/v1` 前缀，统一响应体
- [ ] 无明文密钥/Token 进代码库；新增外部凭据一律环境变量 + 加密
- [ ] 用户输入的 Markdown 前端渲染必过 DOMPurify
- [ ] 列表接口必须分页，禁止无界查询
- [ ] 所有删除均为逻辑删除
- [ ] 提交前 Checkstyle / ESLint 通过，CI 绿灯
- [ ] 不引入 MVP 范围之外的功能（新想法记入扩展功能池）
