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

### [ ] Task 14.6：发帖/评论/分享事件推送（Agent 对接，契约 v1.0）
**描述**：按《社区事件对接契约》v1.0（`.agent-contract/docs/community-api-contract.md`，d8613fd）实现事件 webhook 推送。`ApplicationEvent` 发布（post/comment/share 三类创建动作）+ `@Async` 监听器 HMAC 签名推送 `POST <agent>:18000/api/v1/events`。**只推 post.created / comment.created / share.created，编辑/删除不推**；请求体直接用 Agent 冻结的 Event Schema（content_id 带 `post_`/`user_`/`share_` 前缀、content_text 全文快照 ≤5000 字符超长截断置 metadata.truncated=true、评论只推自身文本不推父链、多态宿主写 metadata.target_type）。
**验收标准**：① 三类创建动作推送到 Agent 返回 202 并记录 trace_id；② HMAC-SHA256 签名（timestamp + "." + raw_body，hex 小写）+ `X-Steward-Timestamp` 头，密钥 32 字节走 .env（`openssl rand -hex 32` 生成，不入 git）；③ 401/5xx/超时仅记日志不重试、不阻塞发帖；④ **422 不静默**——记 ERROR 级日志并按契约 bug 上报 Agent PM；⑤ 推送失败时发帖主流程无感（测试覆盖）。
**安全红线**：共享密钥线下交换给 Agent PM，不经 git、不经聊天明文长期留存；疑似泄露即轮换（90 天常规轮换 P2 前不强制）。
**备注**：Agent 侧 T10.1（EventContentType.SHARE 增量）由 Agent 开发者随其 T13 提交；编号撞车已双标注（Agent-T18 = 社区 API 对接 ≠ 本清单 T18 = 一键导入项目分享）。

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

---

# M5 前端展示扩充（2026-09-09 立项，老板拍板组合方案）

> 背景：M1–M3 收官后老板反馈"前端展示太少、样式只有白+蓝"。UI 设计师调离，视觉方案由 PM 出
> 执行规范：《T-DESIGN-5-视觉改造执行规范.md》（色值/参数已定，照做即可）
> 通用验收：三绿（lint / vue-tsc / build）+ 三断点截图（375/768/1280）+ WCAG 脚本全过 + 页面零硬编码色值

### [x] T-DESIGN-5.3：广场卡片升级
**描述**：项目广场与论坛列表卡片接入三档阴影与 hover 微动效，语义标签按映射表着色（帖子分类=forum 紫、项目卡=share 青、置顶=warning、已下架=error、热榜前三=红底白字徽标），卡片加封面/首图占位区
**验收标准**：hover 上浮 -2px + shadow 过渡 0.2s；标签色与映射表一致且过对比度脚本；三断点无溢出；零业务代码改动
**前置**：T-DESIGN-5.1 已合入（18c6600）✅
**完成**：`3d75976` design(plaza)

### [x] T-DESIGN-5.4：详情页与空状态
**描述**：详情页主卡接 shadow-md；全站空状态统一 n-empty + 图标 + 引导按钮（"去发布"等）；导航/按钮接入 @vicons/ionicons5 图标
**验收标准**：所有列表页空库状态有引导而非空白；图标随文字色；三绿 + 截图
**前置**：T-DESIGN-5.3

### [ ] T-FE-2：首页聚合
**描述**：首页 Hero 下方聚合三区块——热榜（/api/v1/hot）、最新项目（/shares 前N）、最新帖子（/posts 前N）；布局按 T-FE-1 盘点的区块建议（裁决：热榜5 / 项目6 / 帖子5；标签聚合一期 share-only）
**验收标准**：三区块真实数据渲染（需先造多样测试数据）；60s 内重复访问不重复打接口（本地缓存/组件守卫）；三断点布局正确；缺接口项走 T-FE-1 的缺口单转新后端
**前置**：T-FE-1 盘点评审通过后细化下发；T-DESIGN-5.4 完成；G1 搜索就绪后可并行 T-FE-3

### [ ] T-FE-3：搜索与标签聚合页（预立项）
**描述**：搜索页（标题/内容关键字）+ 标签聚合页（按 tag 过滤列表）；后端接口：G1 `GET /api/v1/search`（LIKE）+ G2 share-only 标签云——已裁决转新后端
**验收标准**：待 G1/G2 就绪后细化
**前置**：G1 + G2 接口就绪

### [ ] T-BE-2：环境验收（新后端入职必过）
**描述**：本地跑通全量测试与构建——`mvn.cmd -B verify`（90 个测试全绿）+ 前端三绿各跑一次；通读《交接指引-新后端.md》与检查点 B/C/D 三份报告；产出疑问清单（文档覆盖不到的盲区）
**验收标准**：90/90 测试绿 + 三绿截图 + 疑问清单文档（docs/T-BE-2-疑问清单.md，允许为空但必须显式产出）

### [ ] T-BE-3：演示测试数据集
**描述**：编写可重复执行的演示数据脚本（SQL 或 API 脚本，入 `deploy/dev/`）：≥3 用户、≥10 帖子（覆盖全部分类、含 1 条 5000+ 字符、含置顶）、≥8 项目分享（含多标签）、≥15 评论（含楼中楼）、点赞/浏览数据——供视觉改造（语义标签/热榜/卡片）与 T-FE-2 首页聚合验收使用
**验收标准**：脚本一键执行可重复（重跑不产生脏数据）；数据覆盖语义标签映射表全部用色场景；README 说明执行方式
**前置**：T-BE-2

### [ ] T-BE-4：搜索/标签聚合接口预研（技术方案，不写实现）
**描述**：输出方案建议文档——帖子/分享关键字搜索（MySQL FULLTEXT ngram vs LIKE 成本对比，8.0 中文支持现状）、标签聚合（V1 的 JSON_CONTAINS 现状 vs 建标签表）、分页口径；给出推荐与工作量估算
**验收标准**：方案文档（docs/T-BE-4-搜索标签接口预研.md）含对比表 + 推荐 + 理由；**不写实现代码**——T-FE-1 缺口单到位后合并成正式实现任务
**前置**：T-BE-2
