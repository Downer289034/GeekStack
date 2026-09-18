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
