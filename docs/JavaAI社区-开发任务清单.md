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

### [x] Task 14.6：发帖/评论/分享事件推送（Agent 对接，契约 v1.0）
**描述**：按《社区事件对接契约》v1.0（`.agent-contract/docs/community-api-contract.md`，d8613fd）实现事件 webhook 推送。`ApplicationEvent` 发布（post/comment/share 三类创建动作）+ `@Async` 监听器 HMAC 签名推送 `POST <agent>:18000/api/v1/events`。**只推 post.created / comment.created / share.created，编辑/删除不推**；请求体直接用 Agent 冻结的 Event Schema（content_id 带 `post_`/`user_`/`share_` 前缀、content_text 全文快照 ≤5000 字符超长截断置 metadata.truncated=true、评论只推自身文本不推父链、多态宿主写 metadata.target_type）。
**验收标准**：① 三类创建动作推送到 Agent 返回 202 并记录 trace_id；② HMAC-SHA256 签名（timestamp + "." + raw_body，hex 小写）+ `X-Steward-Timestamp` 头，密钥 32 字节走 .env（`openssl rand -hex 32` 生成，不入 git）；③ 401/5xx/超时仅记日志不重试、不阻塞发帖；④ **422 不静默**——记 ERROR 级日志并按契约 bug 上报 Agent PM；⑤ 推送失败时发帖主流程无感（测试覆盖）。
**安全红线**：共享密钥线下交换给 Agent PM，不经 git、不经聊天明文长期留存；疑似泄露即轮换（90 天常规轮换 P2 前不强制）。
**备注**：Agent 侧 T10.1（EventContentType.SHARE 增量）由 Agent 开发者随其 T13 提交；编号撞车已双标注（Agent-T18 = 社区 API 对接 ≠ 本清单 T18 = 一键导入项目分享）。
**完成（台账补记 2026-09-18）**：`f93106b` 事件推送合入 main + `091cb07` 联调三项修复（422 真实 HttpStatusCodeException 分流 / trace_id 解析 / event_time 改 OffsetDateTime）+ `09f78c3` 联调样例；测试 WebhookPushListenerTest×7 + EventPayloadSerializationTest×1。此前台账误记"取消"（实为联调暂停，开发已交付）——Agent 对接重启函引用"已交付"属实。遗留：生产/dev .env 的 AGENT_WEBHOOK_URL 与 HMAC 密钥线下交换、Agent 侧验签完成后约定联调窗口

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

### [x] T-FE-2：首页聚合
**描述**：首页 Hero 下方聚合三区块——热榜（/api/v1/hot）、最新项目（/shares 前N）、最新帖子（/posts 前N）；布局按 T-FE-1 盘点的区块建议（裁决：热榜5 / 项目6 / 帖子5；标签聚合一期 share-only）
**验收标准**：三区块真实数据渲染（需先造多样测试数据）；60s 内重复访问不重复打接口（本地缓存/组件守卫）；三断点布局正确；缺接口项走 T-FE-1 的缺口单转新后端
**前置**：T-FE-1 盘点评审通过后细化下发；T-DESIGN-5.4 完成；G1 搜索就绪后可并行 T-FE-3

### [x] T-FE-3：搜索与标签聚合页
**描述**：`/search` Tab 分类型（帖子/项目，不做 type=all）+ 防抖 + q 回显转义；`/tags` 标签云（count 排序、字号/深度按热度）→ `/tags/:name` 复用 `?tag=` 项目列表；导航接入搜索；路由 13→15（G2 share-only，标签页不放帖子侧标签）
**验收标准**：三绿 + WCAG + 零硬编码色值 + 三断点截图；空结果 n-empty；卡片复用论坛/广场样式
**前置**：G1 + G2 接口就绪
**产出**：`docs/assets/tfe3-search/` 截图；M5 收官

---

# M6 夜栈灯码视觉换代（2026-09-11 立项）

> 设计概念：夜栈灯码 v1.1 —— 深墨蓝 × 琥珀灯笼  
> 规范：[geekstack-ui-design-spec.md](./geekstack-ui-design-spec.md)｜原型验收基准：`docs/geekstack-ui-design.html`  
> 旧台账：**T-DESIGN-4（暗色主题）关闭**（由本里程碑完整覆盖）；**T-DESIGN-5.3 三分区色体系废弃**（架构经验保留，实现被替代）  
> 顺序：M6-1 → 2 → 3 → 4 串行，每步截图对照原型；M6-5/6 收口状态与红线

### [x] M6-1：Token 地基
**描述**：primitive + semantic 双层 CSS Variables、`data-theme` 切换、Naive UI `darkTheme` + `themeOverrides` 映射、Space Grotesk / JetBrains Mono（`display=swap`）；旧 `--color-*` 兼容别名
**验收标准**：暗/浅切换语义层换绑；Naive 主色琥珀；三绿 + WCAG 抽检；`docs/assets/m6-1-tokens/` 日夜截图
**产出**：`frontend/src/theme/tokens.css` / `tokens.ts` / `stores/theme.ts`
**备注**：Light 主按钮 `--accent-text` 用 `paper-900`（原型稿白字压 amber-600 实测不足 3:1）；浅色语义色字用 `--*-on` 加深变体

### [x] M6-2：布局骨架
**描述**：顶栏（sticky 实色+1px 线，禁 blur）、容器 1180 / 900 / 760 三档、主栏+右栏 320 grid、Footer；首页按蓝图压缩 Hero + 双栏信息流
**验收标准**：对照 `docs/geekstack-layout-blueprint.html`；三绿 + 三断点截图；push CI 绿灯
**前置**：M6-1
**产出**：`docs/assets/m6-2-layout/`

### [x] M6-3：核心组件
**描述**：按钮 3 变体、信息流（头条+列表行+下划线 Tab）、标签/徽章/Code chip；可参数化供 M6-4 复用
**验收标准**：对照规范 §5；`role=tablist/tab`；三绿 + 零硬编码色 + 截图
**前置**：M6-1
**产出**：`frontend/src/components/gs/*`；`docs/assets/m6-3-components/`

### [x] M6-4：页面迁移
**描述**：广场 → 论坛 → 帖子详情 → 项目详情 → 发布 → 主页 → 搜索/标签 → 404；列表行/头条/Tag/Tab 换 Gs*；业务逻辑零改动
**验收标准**：截图对照蓝图；零硬编码色；三断点；按页分 commit；Primary 口径：导航层唯一 + 每屏表单主按钮合规
**前置**：M6-2 / M6-3
**产出**：`frontend/src/components/gs/GsProjectCard.vue`；页面迁入 Gs*；`docs/assets/m6-4-pages/`
**口径备忘**：登录/注册等页内表单 Primary 按「每屏一个」执行，不与顶栏「写文章」冲突

### [x] M6-5：状态与动效
**描述**：骨架屏（surface-2 + 微呼吸）、v-reveal 入场（IO threshold 0.12）、`:focus-visible` 琥珀环、全局 `prefers-reduced-motion`
**验收标准**：消灭页面级 spinner；只动画 transform/opacity；指数缓动；三绿 + 截图
**前置**：M6-3 / M6-4
**产出**：`GsSkeleton`；`directives/reveal.ts`；`docs/assets/m6-5-motion/`；`docs/geekstack-introduction.md` 入库

### [x] M6-6：红线巡检
**描述**：规范第 8 节 6 条逐项验收 + 对比度抽检 + 键盘 Tab 巡检；含编辑器页补巡
**验收标准**：`docs/M6-6-红线巡检报告.md` 逐条结论；不达标项已修（编辑器 P0 白底挂 M7-1）；三绿 + CI
**前置**：全部
**产出**：`docs/M6-6-红线巡检报告.md`；`docs/assets/m6-6-a11y/`；附录 A 编辑器页

# M7 发帖编辑器（2026-09-11 立项）

> 规范：`docs/geekstack-editor-design-spec.md` v1.0  
> 顺序：M7-1 spike 结论先报 → 批准后再覆盖业务页；不换库除非 spike 失败

### [x] M7-1：md-editor-v3 主题透传 Spike（先结论，后工程）
**描述**：半天内验证 `--md-*` 能否绑夜栈灯码 semantic Token；决定覆盖还是换库
**验收标准**：结论文档；日/夜 × 覆盖关/开 截图；**未改业务发布页**
**前置**：M6-6
**产出**：`docs/M7-1-md-editor-主题透传-spike.md`；`docs/assets/m7-1-spike/`；`/dev/md-editor-spike`
**结论**：**能透传 → 保留库 + CSS 覆盖**（待拍板后落地封装 `<GsMdEditor />`）

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

---

# M9 帖子标签与个人设置（2026-09-14 立项，老板拍板）

> 背景：老板发帖发现分类单选无法表达"Java+AI"交叉内容（分类=频道归属保持单选，正解是补标签多选）；个人资料编辑/密码管理确认缺失（个人主页纯展示、user 模块无 PUT 资料接口）。
> 执行顺序（老板批准的计划表）：**后端** T-TAG-BE → T-SKILL-1 → T-PROFILE-BE；**前端** T-TAG-FE → T-SKILL-2 → T-PROFILE-FE（前后端各自串行，两线并行）。

### [x] T-TAG-BE：帖子标签（后端）
**描述**：post 表加 tags（按 T-BE-4 预研结论选型：JSON 列或标签表）；发布/编辑 API 接收 tags（1-5 个，去重防呆）；列表/详情接口返回 tags；新增按标签筛选（`GET /posts?tag=`）
**验收标准**：三路径测试（发布带标签/编辑改标签/越权）；无标签存量帖兼容（默认空数组）；`?tag=` 筛选与 JSON_CONTAINS 或新表方案一致；mvn verify 末行 + CI
**前置**：无（立即开工）
**完成**：Flyway V4 `post.tags` JSON；`TagNormalizer` 去重上限 5；`GET /posts?tag=`

### [x] T-TAG-FE：帖子标签（前端）
**描述**：发布页加"标签"输入（Enter 生成 chip，最多 5 个——编辑器规范 §3.6 补齐）；论坛列表行/卡片展示 tags；列表页标签筛选 chip
**验收标准**：三绿 + 三断点截图 + 零硬编码色值；chip 样式走中性描边（不占琥珀额度）；发布→列表→筛选全链路实测
**前置**：T-TAG-BE
**完成**：`a9c73fd` 发帖 `n-dynamic-tags`（max 5）+ 列表/搜索/主页行展示 + `?tag=` 筛选 + 详情 chip 跳转

### [x] T-PROFILE-BE：个人设置（后端）
**描述**：`PUT /api/v1/users/me`（昵称/简介，头像 URL）；`PUT /api/v1/users/me/password`（第三方注册用户首次设置免验旧密码，已有密码需验旧密码）；头像上传复用 media API（scene=avatar，2MB）；requireOwned 模式
**验收标准**：三路径测试（本人改/他人隔离/未登录 401）；密码设置后旧登录态失效策略（记录口径）；mvn verify 末行 + CI
**前置**：无（可与 T-SKILL-1 并行）
**完成**：`f32decb`；V6 `token_version` + PUT me/password + MeResponse(hasPassword/oauthBindings)；改密口径见开发进度决策 20。PM 验收：push 86b50af 同步、CI 双 job success（run 35226481560，126 测全绿）；token_version 机制实证（refresh 时比对 JWT ver 与 DB 版本，normalizeVersion 兼容存量用户，旧 refresh 立即 401 / 旧 access 30min TTL 自然过期）；端点三件套与 FE（auth.ts + SettingsView）契约逐一比对一致

### [x] T-PROFILE-FE：个人设置页（前端）
**描述**：新 `/settings` 页：资料编辑表单（昵称/简介/头像上传，复用 media API scene=avatar）；密码管理（区分"设置密码"与"修改密码"两种态）；绑定管理（GitHub/Gitee 绑定状态 + 绑定/解绑按钮——API 已有补 UI）；个人主页加"编辑资料"入口
**验收标准**：三绿 + 三断点截图 + 零硬编码色值；头像上传实测（scene=avatar，2MB 拒绝超限）；解绑保护实测（第三方注册无密码用户不可解绑最后绑定）
**前置**：T-PROFILE-BE
**完成**：`46db92e` `/settings` 资料/密码/绑定 UI；主页「编辑资料」+ 导航「设置」；契约对齐 `PUT /users/me` 与 `hasPassword`/`oauthBindings`（待 T-PROFILE-BE 联调）

---

# M8 Skill 专区（2026-09-14 立项，老板拍板形态 A）

> 背景：老板提出"Skill 专区"——AI 技能分享市场（用户发布自己的 Agent 技能/提示词包，广场浏览/点赞/搜索）。
> 架构决策：独立 skill 模块（不塞进 share）；复用点赞/标签/搜索通过类型扩展接入。
> 一期裁剪：发布/广场/详情/点赞/标签/搜索；二期：评论、Skill 文件包上传（MinIO scene=skill）、评分。

### [x] T-SKILL-1（后端）：数据模型 + API
**描述**：Flyway V4（skill 表 + user_like.target_type ENUM 扩展加 'SKILL'，幂等）；发布/编辑/下架（requireOwned）/广场列表（分页+标签筛选）/详情 API；G1 搜索扩展 type=skill；LikeService target_type 扩展 + ContentLikedEvent 联动通知
**验收标准**：照例正/反/越权三路径测试 + HIDDEN 不泄露 + skill 点赞→通知→热榜联动全链路测试；mvn verify 末行 + CI 链接
**完成**：实际用 Flyway **V5**（V4 已被 T-TAG-BE 占用）；广场 `sort=hot`≈ view+like*2；标签云 UNION skill；`/api/v1/hot` 仍仅帖子。PM 验收：CI success（run 35225243379）；下架契约定为 POST /skills/{id}/hide 返回 SkillResponse，FE e10ceaf 已对齐实证

### [x] T-SKILL-2（前端）：Skill 广场 + 发布 + 详情
**描述**：Skill 广场页（列表行/卡片语言 + 模型适配说明专属展示）；发布表单（标题/摘要/提示词正文 Markdown/模型适配说明/标签）；详情页（提示词代码块精致展示）；导航入口；搜索/标签页接入 skill 类型
**验收标准**：三绿 + 三断点截图 + 零硬编码色值 + WCAG；发布一个真实可用演示 Skill（如"Java 异常分析助手"）作验收数据
**前置**：T-SKILL-1
**完成**：`f7e30f9` `/skills` 三页 + 导航 + 搜索 Tab=`skill`；API 契约冻结对齐计划表（待 T-SKILL-1 联调）

### [x] T-SKILL-3-BE：Skill 版本号与 changelog（后端）
**描述**：Flyway 顺延加 `skill.version`、`skill.changelog`（存量回填 `1.0.0`）；发布/编辑收两字段（version SemVer 核心校验，缺省 `1.0.0`）；详情/广场列表返回
**验收标准**：三路径（本人改/越权 403/未登录 401）+ 非法 SemVer 400；mvn verify + CI
**前置**：T-SKILL-1
**完成**：`757dcf2`；V7 version/changelog + SemVer；详情/广场返回

### [x] T-SKILL-3-FE：发布对齐 SkillHub 规范（前端，与 T-AI-0 并行）
**描述**：①发布/编辑表单加版本号（默认 `1.0.0`，SemVer 核心正则与 BE `SemVer` 对齐）+ 变更说明（选填 textarea，≤2000）；②摘要框支持粘贴 `SKILL.md`：纯前端解析 YAML frontmatter 的 `name`/`description` 预填标题与摘要，失败静默降级为手填；③契约对齐 T-SKILL-3-BE：`POST/PUT` 载荷 `version`（必填）+ `changelog`（选填）；旧数据无版本号展示兜底 `1.0.0`；④详情页展示版本号 + 最近变更说明（不做版本历史列表）
**验收标准**：三绿 + 零硬编码色值 + 三断点截图；粘贴真实 SKILL.md 自动提取演示；契约与 BE 联调通过
**前置**：T-SKILL-3-BE
**完成**：`skillMdFrontmatter.ts` + Publish/Detail/`skill.ts`；样例与截图 `docs/assets/t-skill-3-fe/`（三断点 + 粘贴提取 `paste-extract.json` ok）；契约对齐 BE `SkillPayload`/`SemVer` 核心正则；FE 载荷字段与 `SkillVersionIntegrationTest` 一致
**PM 验收**：`36b783a`（FE 实现）+ `3a52e54`（墨阶提亮修正，老板反馈"黑得看不清"的正规修复，对比度复核 4.92/4.59 ≥4.5，单独 commit）已 push，CI 双 job success（run 35340543682）→ **T-SKILL-3 关闭**。过程备注：push 遭遇网络抖动（代理 502/直连超时），首次"失败"push 实际已到达，经 GitHub API 核实远程 main=3a52e54；本地三绿（直调 node）同步验证

### [ ] 二期预留：评论（target_type 扩展）、Skill 文件包上传（MinIO scene=skill）、评分体系

---

# M10 登录安全加固（2026-09-17 立项，老板拍板）

> 背景：PM 例行登录链路安全审计（亲审 JwtService/UserService/SecurityConfig/OAuthService/OAuthController/GiteeOAuthService/RegisterRequest）结论：整体及格线以上、无致命漏洞，但发现两处必修项——①OAuth 无 state 参数（Login CSRF/绑定劫持面，OWASP 标准必修）；②`/api/v1/auth/**` 无频控（爆破/撞库面）。审计达标项（BCrypt、统一 401 防枚举、refresh 查封禁、AES-GCM 落库、保守合并语义、解绑保护）无需改动。
> 执行顺序（老板批准的计划表增量，2026-09-17 修订：T-SKILL-1 / T-PROFILE-BE 已收官）：**后端** T-SEC-1-BE；**前端** T-SEC-1-FE。Flyway 版本号一律以合并后 main 最大版本 +1 顺延，禁止写死编号。

### [x] T-SEC-1-BE：OAuth state 校验 + 登录频控（后端）
**描述**：①`/login/{provider}` 生成一次性随机 state（SecureRandom ≥128bit，Redis 存 5 分钟 TTL），`/api/v1/oauth/{provider}/code` 转交时必带并校验：缺失/不匹配/过期/重放一律 401，校验即消费（防重放）；GITHUB/GITEE 两平台同改。②auth 三接口（login/register/refresh）失败计数频控：Redis 按 IP+用户名维度，15 分钟内失败 5 次锁定（返回 429），成功登录清零；实现复用 RedisTemplate 并写降级分支测试（Redis 不可用时放行并记日志，参照 notify 模块 flaky 教训钉死降级分支）
**验收标准**：state 四路径测试（缺失/篡改/过期/重放均 401，正常登录不受影响）；频控测试（第 6 次失败 429、成功清零、Redis 宕机降级放行）；现有全部测试不回归；mvn verify 末行 + CI 链接；密码/token 存储零明文红线不变
**前置**：无（T-PROFILE-BE 之后、T-SKILL-1 之前）
**完成**：`d264767`；契约见开发进度决策 21。PM 验收：push 1672657 同步、CI 双 job success（run 35229485018，138 测全绿）；OAuthStateService 实证——`GETDEL` 原子消费防重放竞态、state 绑定 provider、SecureRandom 128bit、前端 state 白名单 hex（兼保 Redis key 安全）、Redis 读写失败均 fail-closed；AuthRateLimiter 实证——IP+用户名、5 次/15min 第 6 次 429、成功清零、降级三处一致且被 DegradeTest 钉死

### [x] T-SEC-1-FE：OAuth state 生成与转交（前端）
**描述**：点击授权按钮前用 `crypto.getRandomValues` 生成 state 存 sessionStorage；`/oauth/callback` 页转交 code 时一并提交 state 并比对，不匹配则清空 pending 提示重新发起登录；不匹配/缺失场景 UI 走现有错误态（不新增样式）
**验收标准**：三绿 + 零硬编码色值；正常 OAuth 登录/绑定全链路实测不受影响；篡改 state 的手动演练被后端拒绝且页面有明确提示
**前置**：T-SEC-1-BE（契约对齐：code 载荷新增 state 字段）
**完成**：`beginOAuth` → sessionStorage + `?state=`；回调页 FE 比对闭环 + `POST /code` 带 state；演练说明 `docs/T-SEC-1-FE-OAuth-state-手动演练.md`。PM 验收：7cf2ca9 已 push、CI 双 job success（run 35291684463，frontend 53s/backend 1m53s）；实现实证——`generateOAuthState` 16 字节 crypto.getRandomValues → 32 hex 合 BE 白名单、`takeExpectedOAuthState` 读即删、回调页三重比对（`!expected || !returned || expected !== returned`）、失败双清（pending+state）走 n-empty、登录页与设置页入口统一走 `beginOAuth`、转交用原生 axios 避开 refresh 拦截器。**M10 关闭，功能开发线全部收官**。遗留浏览器侧两步：篡改 state 演练截图 + 正常登录/绑定回归（按演练文档执行）

---

# M11 全站粒子氛围层（2026-09-18 立项，老板拍板：仅全站页面，不含登录页）

> 背景：UI 设计师交付 GsParticles v1.0（`docs/geekstack-particles-bg.html` + `docs/design-report-2026-09-18.md`）。老板拍板范围：仅全站内容页背景，**登录/注册页不做**（该页已有视频背景，避免双层动画叠加）。
> 设计纪律（设计师自约束，PM 采纳为验收红线）：纯装饰不交互（禁鼠标跟随/连线）、canvas fixed 底层只在留白可见、夜=琥珀萤火 alpha 0.08–0.28 / 日=暖墨浮尘 alpha 减半、"被用户注意到即失败"。

### [x] T-DESIGN-P1：GsParticles 全站氛围层（前端）
**描述**：`GsParticles.vue` 组件，App 级单实例挂载于布局层（router-view 之外），canvas fixed 底层（z-index 低于内容、高于 body 背景）；双主题色值走 token（禁硬编码）；密度 ≤56；路由排除 `/login` `/register` `/oauth/callback`；性能四件套——DPR 封顶 2、`visibilitychange` 页签隐藏即暂停、`prefers-reduced-motion` 不启动、掉帧降级（rAF 间隔 >33ms 连续 60 帧 → 密度减半，再触发 → 停用并记日志）；≤768px 不启动
**验收标准**：三绿 + 零硬编码色值；登录/注册页确认无粒子（路由排除实测）；页签隐藏后 CPU 归零（任务管理器截图）；reduced-motion 下 canvas 不渲染；掉帧降级路径演练记录（可用 devtools 限速模拟）；暗/亮主题各一截图（粒子肉眼几乎不可察觉为准——"注意到即失败"）；不回归现有页面（三页抽查）
**前置**：无（可立即开工，与 T-SEC-1-FE 浏览器侧回归演练同批）
**完成**：PM 直接实现。`GsParticles.vue`（App 级单实例挂 App.vue、三重门控 reduced-motion/≤768px/路由排除、基色读 tokens.css `--particles-rgb` 双主题变量零硬编码、粒子基准 alpha 归一化使主题切换即时生效、GETDEL 同款原子思路的掉帧降级 rAF>33ms×60 帧密度减半→再触发停用、暂停恢复重置 lastT 防误计）；三绿自测过（eslint/vue-tsc/vite build）。**遗留浏览器侧走查**：CPU 归零截图、降级演练、暗/亮主题与登录页无粒子确认

### [x] T-VERIFY-1：粒子层走查 + 资产治理（前端）
**描述**：①粒子浏览器侧走查四项——暗/亮主题切换收敛度（"注意到即失败"）、登录页无粒子、页签切走 CPU 归零（任务管理器截图）、devtools CPU 6x 限速触发降级日志；②T-SEC-1-FE 遗留回归——篡改 pending.state 演练截图 + 正常 Gitee 登录/设置页绑定各一遍（按 `docs/T-SEC-1-FE-OAuth-state-手动演练.md`）；③资产治理：工作区 login-bg.mp4（docs/assets + frontend/src/assets 两处）为 2.6MB 未提交修改——还原或压缩 CRF 28 去音轨后单独提交（HEAD 版本 1.64MB，禁止直接提交变大版本）；双份 mp4 冗余治理（只留 frontend 引用一份）；④设计基线文档入库：`docs/design-report-2026-09-18.md`、`docs/geekstack-particles-bg.html`
**验收标准**：走查项逐条截图/记录回贴任务清单；mp4 入库后 `git cat-file -s` ≤ 1.7MB（或附新素材说明）；文档入库走 docs commit；全部完成后 M11 关闭
**前置**：无
**完成**：证据见 `docs/assets/t-verify-1/`（含 walkthrough/degrade JSON + 四张截图）；资产：脏 2.6MB mp4 还原、删除 docs 副本、设计稿改引 frontend；`git cat-file -s` login-bg = 1719645。Gitee 真登录/绑定仍需人工凭据联调一次。→ **M11 关闭**。PM 验收：b71b65c/f7c40b3 已 push，CI success（run 35304331039）；工作区 mp4 脏文件确认清零、docs/assets 无冗余副本、t-verify-1 证据齐全（degrade.json 实证两级降级链路完整触发：减半至 25 → 停用）；设计文档入库核实。遗留一项不阻塞：Gitee 真登录/绑定由老板本机人工点验（自动化无授权码）

---

# P2-18 头像上传「假 PNG」误拒（2026-09-18 用户反馈，PM 修复）

> 现象：设置页上传头像报「文件内容不是合法图片」。取证：用户上传源文件为标准 PNG 魔数（89 50 4E 47…，112KB），全链路（n-upload 原生 File → FormData → 后端前 12 字节嗅探）无加工——判定为用户侧文件「名实不符」类问题：扩展名 .png / 浏览器声明 image/png，但真实内容为 JPEG 等其他白名单格式（聊天工具/截图软件另存常见）。
> 原校验为「扩展名+Content-Type+魔数三一致」，名实不符即拒——对用户过于严苛。修复策略：**魔数为王**——文件头真实格式在白名单（jpg/png/gif/webp）内即通过并按真实格式存盘；伪装攻击（EXE/HTML 改名 .png）魔数不在白名单依旧拒绝，安全性不降。

### [x] P2-18：ImageValidator 改「魔数为王」
**改动**：`ImageValidator.validate` 签名收敛为 `(byte[] header)`，扩展名/Content-Type 不再参与判定；存盘扩展名按魔数真实格式（假 PNG 内容 JPEG → 存 .jpg）；`MediaUploadService` 调用同步。测试：`ImageValidatorTest` 按新语义重写（新增假 PNG 通过、EXE 魔数拒绝、过短 header 拒绝）；`MediaUploadIntegrationTest` 新增 `fakePngWithJpegContentAcceptedAsJpg` 用例。
**验收**：后端全量测试绿（CI）+ 用户真实文件复传通过。
**备注**：本地 mvn 执行受审批环境限制未能在沙箱内跑通，验证以 CI 全量测试为准（红则修复重推，按 fix 两次连续绿关闭）。生产/dev 栈需重建后生效。
**验收**：`f2386cf` 已 push，CI 双 job success（run 35326870859，backend 全量测试含 `fakePngWithJpegContentAcceptedAsJpg` 用例通过）→ **P2-18 关闭**。生效条件：用户所测环境的 backend 重新构建部署

---

# M12 登录页背景 v2 实景视差（2026-09-18 立项，老板自制实景图）

> 基准：`docs/geekstack-login-bg-handoff.md` v1.0（UI Designer 交接文档）+ 视觉稿 `docs/geekstack-login-bg-v2.html`（v2.3.2 逐屏对照唯一基准）。
> 方案演进（设计侧已论证）：**视频背景正式淘汰**（1.7MB+音轨重；"土"的根因是具象三件套不在媒介）→ 终稿为老板自制暮色实景图（login-bg-dusk.jpg，2848×1600 q85 渐进 JPG 421KB）+ 鼠标视差（反向 ±20px，lerp 0.05 惯性）+ 滚动退场（0.05×scrollY）+ 暗角 + 萤火粒子（44 颗 α 0.06–0.22）。
> 红线（一票否决）：禁 backdrop-blur（卡片 90% 实色+投影）；登录/注册页锁定夜色；禁文字/Logo 外发光、禁前端渲染 HTML 字标（图中已有品牌字）；视差/粒子禁交互化；性能预算最低位。

### [x] T-LOGIN-BG：LoginBackground 组件 + 视频方案拆除（前端）
**描述**：①封装 `<LoginBackground />`（视差壳 scale(1.2) 常驻 20% 出血 + img + 暗角 radial-gradient + 粒子 canvas 三件），仅登录路由挂载，`onUnmounted` 销毁 rAF 与监听，mousemove `passive:true`，单 rAF 循环 lerp 0.05；②登录页粒子复用 GsParticles 参数（44 颗封顶、α 0.06–0.22、只画 arc 禁 shadowBlur、页签隐藏暂停）——App 级实例维持排除 `/login` 不变，两套画布不重叠；③降级矩阵：reduced-motion/触屏（无 hover）→ 静态图+暗角；图加载失败 → `--ink-950` 墨蓝兜底；90 帧 avg<50 → 交互降级静态+粒子减半；④**拆除视频方案**：LoginView 移除 video 元素与四层降级矩阵代码、删除 `frontend/src/assets/login-bg.mp4` 引用与文件；素材只入库 `docs/assets/login-bg-dusk.jpg`（≤450KB，PNG 原图严禁入库）
**验收标准**（对照 handoff §6）：1920×1080 卡片不遮图中「极客栈」品牌字、任何滚动/视差位置不露黑边；视差有惯性缓停非 1:1；滚动退场 0.05× 滚回复位；触屏/reduced-motion 全静态；图挂墨蓝兜底且可正常登录；Performance 面板 GPU 增量 ≤3%；仓库无 mp4/PNG 冗余；三绿 + 零硬编码色值（暗角/兜底色走 token）
**前置**：无
**完成**：`LoginBackground.vue` + LoginView 视频拆除；卡片下移 20vh、去重复品牌字标；素材 `docs/assets/login-bg-dusk.jpg`（430788 B）；`@docs` 别名引用；mp4 已删
**PM 验收**：aeef427/9924e4d 已 push（f2386cf..9924e4d），CI 双 job success（run 35335626538，frontend 58s/backend 1m55s）；实现实证——LERP 0.05 常量、`prefers-reduced-motion` + `pointer: fine` 双 matchMedia 门控、mousemove `passive:true`、滚动 0.05×（`-sy*0.05`）、visibilitychange 暂停、兜底 `--ink-950` token；`frontend/src/assets` 无 mp4 残留、docs 素材 430788B。→ **M12 关闭**。遗留：实机视差手感与品牌字避让由老板浏览器侧过目（纯主观项）

---

# M13 AI 助手（RAG 问答，2026-09-18 立项，老板批准）

> 需求：页面内嵌 AI 助手，基于两类语料回答——①平台知识（使用说明/版规/功能区介绍，需新建）；②社区内容（帖子/分享/文档/Skill，RAG 主体）。回答必须带来源链接。
> 架构决策（随计划批准，预研 T-AI-0 中复核）：**AI 服务独立部署**（Python 3.12 + FastAPI + LangGraph，与 Java 主站解耦，复用 DownerCode 编排经验）；**向量库 Qdrant**（单容器轻量，量级上来再迁 Milvus，embedding 层屏蔽差异）；**LLM 首选小米 MiMo v2.5**（老板拍板意向；官方 OpenAI 兼容 API `api.xiaomimimo.com/v1`，模型 mimo-v2.5 / mimo-v2.5-pro，支持 SSE 流式与 tool_calls，国内直连合规；注意 thinking 模式默认开启、thinking 下 temperature/top_p 被强制覆盖，RAG 场景对比 disabled/enabled 的延迟与质量后定）；**embedding 统一为阿里云 DashScope `text-embedding-v3`（1024 维）**——2026-09-19 依据管家项目组立场回应（rag-reuse-position.md §二）修正：管家侧生产实配即 text-embedding-v3，统一到它管家零成本、极客栈接入 DashScope 计费；原 BGE-M3 仅代码兜底从未实配，且本地跑不可行。**验收铁律是"双方同源"**，型号作为可配置项。
> **双库并存架构（与管家项目组已对齐，rag-reuse-position.md §四）**：治理语料库（版规/判例）留管家侧 Milvus（物理隔离，写权限仅管家）；社区内容语料库挂极客栈侧 Qdrant（T-AI-5 托管），管家 Agent 以**只读凭证**接入；统一 metadata schema（content_id/source_system/content_type/text_url/author_id 脱敏/created_at/visibility/**embedding_model 版本化**/extra open dict）为两边 collection 共同契约——"一次 embedding、一份语料、schema 一致"，引擎不同不影响向量可比性。
> 硬约束：只索引公开内容（HIDDEN/草稿/封禁用户内容不进语料）；回答必带来源链接（防幻觉兜底）；API Key 全走 .env（零明文红线）；用户级限流（复用频控思路）；一期纯 RAG 问答，不做工具调用 agent/语音/推送。
> 排期定位：建议**先上线再接 AI**（公测真实语料避免空转调优）；一期开发可与上线准备并行，部署跟随上线。

### [x] T-AI-0：RAG 链路预研 spike（AI 服务）
**状态**：✅ **关闭（2026-09-20 PM 选型复核通过）**——四块拍板生效，embedding 契约定稿，已放行 T-AI-1
**描述**：搭最小可跑链路——FastAPI 骨架 + LangGraph 单节点图 + Qdrant（docker 单容器）+ DashScope `text-embedding-v3` embedding（1024 维，Key 需线下配置 `DASHSCOPE_API_KEY`）+ MiMo v2.5 API（`api.xiaomimimo.com/v1`，OpenAI 兼容）；用 10 条真实社区内容走通「分块 → embedding → 入库 → 检索 top-5 → 生成带引用回答」；产出选型复核报告（含分块策略 512token/50overlap 建议、成本估算、延迟实测、**MiMo thinking disabled vs enabled 的延迟/质量对比**）
**验收标准**：spike 代码入库 `ai-service/`（骨架可复用）；报告落 docs；关键决策（供应商/分块参数/thinking 开关）复核确认
**双 RAG 线复用性设计（老板拍板的战略要求，报告必备章节）**：~~原五要素~~ **已由管家项目组立场回应（`rag-reuse-position.md`，2026-09-19）对齐并收敛**，预研按已对齐口径执行：①统一 metadata schema 按管家草案骨架（content_id 含来源前缀/source_system/content_type/text_url 不存全文但 **chunk 文本必须入 payload**（否则 RAG 无法工作，需在回函澄清）/author_id 脱敏/created_at/visibility/**embedding_model 版本化字段**/extra open dict）；②治理语料（版规/判例）留管家侧 Milvus 物理隔离，社区内容语料挂极客栈 Qdrant、管家只读接入；③embedding 双方同源 text-embedding-v3（铁律=同源，型号可配置）；④隐私红线按管家需求清单（§五四项：API 拉取不落库/聚合优先/动作 API 幂等+审计/判罚回流只读）双向写死。本章节作为三期谈判技术底稿，回函由 PM 起草
**前置**：无（LLM API Key 已由老板线下配置；**DASHSCOPE_API_KEY 已配置**）
**完成说明（2026-09-20）**：报告 [`docs/T-AI-0-RAG选型复核报告.md`](./T-AI-0-RAG选型复核报告.md) + `docs/assets/t-ai-0/bench_metrics.json`；PM 四块拍板已生效。附加观测：e2e P95 11.6s，T-AI-2 持续采样。

### [x] T-AI-1：语料索引管道（Java 后端）
**状态**：✅ **实现完成（2026-09-20）**。定稿四块已落地：分块 512/50｜thinking disabled（问答侧）｜schema v1.1｜双库并存 + text-embedding-v3。`RagIndexIntegrationTest` + `MarkdownChunkerTest` 通过；checkstyle 通过。**附加观测项：e2e P95 11.6s，T-AI-2 落地后持续采样**
**PM 验收**：PM 隔离提交三个 commit——`68b6fb9`（T-AI-1 本体：rag 包 15 类 + Post/Share/Skill 事件触发点 + 帮助语料 + 集成测试）、`d9617f6`（**ai-service spike 代码补入库**——T-AI-0 验收时遗漏，.venv 已 ignore）、`c46c9b9`（T-M4-1 提权后 403 预期行为注释补强），push 后 CI 双 job success（run 35479597577，backend 1m59s 全量含 RagIndexIntegrationTest + MarkdownChunkerTest）。实现逐项核验：512/50 默认、DashScope 客户端 + 无 Key 确定性伪向量降级（测试不打真实接口）、jkz_ 前缀、chunk_text/text 同值硬约束、parent_content_id 幂等删除、QDRANT_URL=memory 测试默认、失败 WARN 不阻塞、HIDDEN/封禁兜底删向量——定稿口径全项落地。→ **T-AI-1 关闭，M13 主链滚动至 T-AI-2/T-AI-3**
**描述**：事件总线新增 RAG 索引消费者——订阅 ContentCreated/Replied/Updated 事件，过滤公开内容（status=0 且作者未封禁）→ Markdown 去标记分块（512token/50overlap）→ 调 **DashScope text-embedding-v3** → 写极客栈侧 Qdrant 社区内容库（metadata 按统一 schema **v1.1**：content_id 带前缀 `jkz_`/source_system/content_type/text_url/author_id 脱敏/created_at/visibility/embedding_model + **chunk_text/chunk_index/parent_content_id 三字段**——2026-09-20 按管家评审采纳）；**chunk 文本写入 payload**；删除/下架内容同步删向量；附平台帮助语料初始化脚本（使用说明 JSON 一次性灌库）；HTTP 回调失败重试 + 降级日志（参照 Webhook 教训：RestTemplate 必须带超时）
**验收标准**：发布/编辑/删除三路径测试（mock embedding）；HIDDEN 内容不进库断言；metadata schema v1.1 与管家草案逐字段对齐；mvn verify 末行 + CI
**前置**：T-AI-0（embedding 接口契约定稿）
**完成说明**：`com.community.rag`——`RagContentChangedEvent` 驱动 post/share/skill 的 UPSERT/DELETE；公开内容（作者未封禁；share/skill 非 HIDDEN）才入索引；schema v1.1（`text`≡`chunk_text` + chunk 三字段 + `jkz_`）；默认 `QDRANT_URL=memory`，联调改 Qdrant HTTP；无 Key 时确定性伪向量；帮助语料 `classpath:rag/help-corpus.json` + `deploy/rag/`。

### [ ] T-DATA-1：用户数据 API（管家 R2 调查工具数据源，时钟：约第 3 周前）
**背景**：管家 roadmap 09-19 拍板 J1 重定义为极客栈 API 适配层，PY2-R2 调查工具（用户历史/判例/社区统计）原依赖 J1 数据源改为**依赖极客栈数据 API**——第 3 周前未到位则 R2 降级 mock。为避免对方降级，本任务提前规划
**描述**：面向管家（认证后）的只读数据 API：①用户发帖/评论历史（脱敏 ID，分页）；②社区统计（内容量/用户量/判罚量聚合）；③判罚记录回流预留。全部**脱敏 + 只读**，开放范围过隐私口径（聚合优先，明细最小集）
**验收标准**：三路径测试；脱敏断言（响应不含邮箱/密文类字段）；与管家侧 R2 契约对齐（对齐时点：契约 v2 评审）
**前置**：T-M4-1（已关闭）；对齐时点等管家 X1 验签完成

### [ ] T-AI-2：问答链路（AI 服务）
**描述**：LangGraph 流程：问题改写 → Qdrant 检索 top-5（相似度阈值过滤）→ prompt 组装（系统提示词含"仅基于资料回答，资料不足如实说"）→ LLM 流式生成 → 来源列表（url+标题）；SSE 输出；空检索结果走兜底话术
**验收标准**：单测（检索为空/正常/超阈值三分支）；真实问答 10 问人工评测（相关性+引用正确性记录 docs）；延迟 P95 记录
**前置**：T-AI-0、T-AI-1

### [ ] T-AI-3：网关与安全（Java 后端）
**描述**：`/api/v1/ai/ask`（POST，SSE 透传）：JWT 鉴权 → 用户级限流（如 10 问/小时，Redis）→ 请求体清洗（长度上限）→ 转发 ai-service（内网地址走 .env）→ 敏感词/无关问题前置过滤可选；成本日志（token 用量按用户记录）
**验收标准**：未登录 401、超限 429、ai-service 宕机 503 降级话术；三路径测试；CI
**前置**：T-AI-2

### [ ] T-AI-4：前端浮窗
**描述**：右下角悬浮球 → 展开对话面板（夜栈灯码组件语言：面板 surface-1、琥珀仅用于发送按钮）；SSE 流式渲染 Markdown（复用渲染唯一出口防 XSS）；来源链接卡片（favicon+标题，新窗打开）；会话历史 localStorage 本地保留；移动端折叠为全屏底部抽屉
**验收标准**：三绿 + 零硬编码色值 + 三断点截图；流式无卡顿；来源链接可点；未登录提示登录；CI
**前置**：T-AI-3

### [x] T-AI-4-PRE：AI 浮窗静态骨架（前端，mock 接口）
**描述**：`AiChatWidget` 组件——右下角悬浮球 → 展开对话面板（夜栈灯码语言：surface-1 面板、琥珀仅发送按钮）；消息列表 + 输入框 + 发送中态；SSE 渲染层先用 **mock 流**（本地定时器模拟 token 流），真实接口 T-AI-3 后替换；会话历史 localStorage；移动端折叠为全屏底部抽屉；`prefers-reduced-motion` 降级
**验收标准**：三绿 + 零硬编码色值 + 三断点截图；mock 流式渲染无卡顿；未登录点击提示登录；不与现有页面布局冲突（三页抽查）；CI
**前置**：无（真实接入等 T-AI-3）
**完成**：`AiChatWidget.vue` + `aiAskMock.ts` + `aiChatHistory.ts`；App 级挂载；证据 `docs/assets/t-ai-4-pre/`
**PM 验收**：`7c2b54b` 由 PM 隔离提交（严格排除工作区后端 admin/ai-service 进行中改动）并 push，CI 双 job success（run 35438324660，frontend 1m0s/backend 1m55s）；本地三绿（直调 node）同步验证。**T-AI-4-PRE 关闭**，真实 SSE 接入等 T-AI-3

### [ ] T-AI-5：部署与走查
**描述**：docker-compose 增 `ai-service` + `qdrant` 服务（dev 栈先行，生产栈跟随上线）；.env.example 增 LLM/embedding/Qdrant 配置项（占位符）；健康检查；部署指引补交接文档；全链路走查清单执行
**验收标准**：dev 栈全链路实测（发帖→自动索引→提问命中）；compose up 一键起；走查记录入库
**前置**：T-AI-4

---

# M4-min 管理端最小集（2026-09-19 重启，老板放行——Agent-T18 处置回调前置依赖）

> 背景：契约增量清单（community-api-contract-v2-delta.md §三）交底：Agent 处置动作（删帖/禁言）无对应端点——管理员删帖、封禁 API 均不存在。M4 完整管理端继续挂起，**只重启支撑处置闭环的最小集**；加精/置顶/推荐位随三期。

### [x] T-M4-1：管理员鉴权 + 删帖/禁言端点（后端）
**描述**：①管理员鉴权：`user.role` 字段已有（USER/ADMIN），SecurityConfig 对 `/api/v1/admin/**` 增加 `hasRole('ADMIN')` 保护（现有 JWT 过滤器需把 role 写入 Authentication authorities）；②`DELETE /api/v1/admin/posts/{id}` 管理员删帖（软删/隐藏口径与作者自删一致，记录操作者）；③`PUT /api/v1/admin/users/{id}/status` 禁言/解封（0 正常/1 封禁，记操作者与原因可选字段）；④首个 ADMIN 用户产生方式：提供一次性提权 SQL 脚本（`deploy/sql/promote-admin.sql`，运维手动执行，不入自动迁移）；⑤处置动作全审计：操作者/目标/时间/动作落日志（表或日志文件，为 Agent 处置回调"可审计可回放"口径打样）
**验收标准**：三路径测试（ADMIN 成功/USER 403/未登录 401）；封禁后目标用户 login 与 refresh 双双被拒断言（复用 P2-3 口径）；删帖后详情 404 或隐藏口径断言；checkstyle + mvn verify + CI；零明文红线不变
**前置**：无（与 T-AI-0 并行）
**完成说明（2026-09-19）**：JWT `role` claim → `ROLE_*` authorities；`/api/v1/admin/**` hasRole(ADMIN)；删帖/禁言端点 + `ADMIN_AUDIT` 结构化日志；`deploy/sql/promote-admin.sql`；`AdminApiIntegrationTest` 6/6；checkstyle 通过。全量 `mvn verify` 本机因 Redis 不可用导致无关限流/OAuth 用例失败，与本任务无关。
**PM 验收**：`922dbed` 由 PM 隔离提交（排除同工作区 ai-service 进行中文件）并 push，CI 双 job success（run 35438965095，backend 2m37s 全量测试含 AdminApiIntegrationTest——CI 有 Redis 容器，验证了"本机 Redis 不可用"说明属实）。安全审查通过：role claim 旧令牌兜底 USER、status 白名单校验（@Min(0)@Max(1)+服务层双保险）、promote-admin.sql 占位符无真实凭据、ADMIN_AUDIT 结构化审计。**T-M4-1 关闭**——Agent-T18 处置回调前置缺口清零；运维提权后目标用户须重新登录（旧令牌无 role claim 默认 USER，安全兜底）。

---
