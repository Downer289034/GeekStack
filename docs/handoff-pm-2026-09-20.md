# 极客栈 · 项目经理交接文档

> 交接日期：2026-09-20 ｜ 交接人：PM（会话轮换） ｜ 读者：下一任 PM / 开发 / 老板
> 用途：5 分钟接手项目全景。任务明细以 `docs/JavaAI社区-开发任务清单.md` 为唯一清单源。

---

## 一、项目全景

**极客栈（GeekStack）**：Java 17 + Spring Boot 3 / Vue3 + TS 技术社区，求职作品集。
仓库：`github.com/Downer289034/GeekStack`（main 分支，CI 双 job：frontend/backend）。
另有并行项目 DownerCode（Python/LangGraph 编码助手，gitee 私库）——与本项目无任务交叉，勿混。

**里程碑状态**：
- M1–M11 全部收官（核心社区/热榜/视觉换代/编辑器/Skill/个人设置/OAuth 安全加固/粒子氛围层/登录页 v2）；
- **M12 登录页背景 v2**：已关闭（LoginBackground 实景视差，视频方案已淘汰删除）；
- **M13 AI 助手**：进行中（见 §三）；
- 对接线：智能社区管家 Agent 合作（见 §四）。

## 二、开发纪律（必须沿用）

1. 任务清单唯一源：`docs/JavaAI社区-开发任务清单.md`（勾选 + PM 验收记录格式见既有条目）；
2. 交付 = 独立 commit + push + CI 绿（frontend/backend 双 job）+ 证据入 `docs/assets/t-*/`；fix 关闭须两次连续 green；
3. PM 独立复跑：不采信自测汇报，关键代码亲自读、测试亲自跑、数字与原始 JSON 交叉核对；
4. 安全红线（一票否决）：宿主机不执行 Agent 生成代码 / 零明文凭据（key 只进根目录 `.env`，gitignore 生效） / 工具调用审计 / 第三方库 API 查签名后用；
5. 本机开发环境坑：npm 触发沙箱 wsl.exe 黑名单 → 直调 node（`C:\Users\lenovo\.workbuddy\binaries\node\versions\22.22.2-3\node.exe` + node_modules 工具链入口）；本地 mvn 全量曾被审批拦截 → CI 全量为权威；网络抖动（代理 502）时 push 失败可能实际已到达 → **远程状态一律 `gh api` 核实**，本地 ahead 数字不可信；跨人协作可代推；
6. `.env`（根目录）已含真实 Key：MIMO_API_KEY / DASHSCOPE_API_KEY / GITEE_CLIENT_SECRET / STEWARD_WEBHOOK_SECRET / JWT_SECRET 等——**零入库零聊天**，交接线下。

## 三、M13 AI 助手（进行中，六任务已关五）

架构定稿（T-AI-0 复核报告 `docs/T-AI-0-RAG选型复核报告.md`，拍板全过）：
分块 512/50 ｜ MiMo v2.5 thinking=disabled（P95 5.9s vs 11.6s）｜ embedding DashScope text-embedding-v3（1024 维，与管家同源——铁律"双方同源"，型号可配置）｜ 双库并存（社区内容=我方 Qdrant，治理语料=管家 Milvus）｜ schema v1.1（统一字段 + chunk_text/chunk_index/parent_content_id + text 入 payload 硬约束 + jkz_ 前缀 + embedding_model 版本化）。

| 任务 | 状态 | 要点 |
|---|---|---|
| T-AI-0 预研 | ✅ | 报告+bench_metrics.json 可复跑；e2e P95 11.6s 观测项 |
| T-AI-1 语料管道 | ✅ | rag 包（事件消费→分块→DashScope→Qdrant）；无 Key 伪向量降级；QDRANT_URL=memory 测试默认 |
| T-AI-2 问答链路 | ✅ | ai-service：rewrite→检索阈值 0.35→MiMo 生成；/v1/ask + /v1/ask/stream（SSE token/sources/done）；10 问评测 10/10 |
| T-AI-3 网关 | ✅ | /api/v1/ai/ask：JWT→限流 10/h→503 降级（先连后写）；AI_COST 日志 |
| T-AI-4-PRE/4 | ✅ | AiChatWidget 真实 SSE（fetch+ReadableStream）；mock 已删 |
| **T-AI-5 部署走查** | 🟢 **当前唯一在途** | compose 增 ai-service+qdrant；**CI 增 ai-service job（unittest，治理欠账）**；全链实测：发帖→自动索引→提问命中 |

## 四、管家 Agent 对接线（重要，跨项目）

合作方 PM 文档全在 `E:\ZT-VUE-SSM\2026-09-06-20-54-50\`（另一工作区）：master-plan.md（其唯一计划源）、rag-reuse-position/reply.md、key-clarification.md、jikezhan-three-decisions-reply.md、multi-agent-roadmap.md。

**双 RAG 线架构（已谈判闭环）**：治理语料（版规/判例）留管家 Milvus 物理隔离；社区内容库（我方 Qdrant）管家只读接入；embedding 同源 text-embedding-v3；DashScope Key 双方各自持有绝不共享（key-clarification.md 口径，已两次纠偏对方）。

**三项拍板回执已出待发出**：`20-54-50/docs/jikezhan-three-decisions-reply.md`——①RAG 边界确认（受众隔离：站内助手面向用户/管家面向运营者）；②联调窗口提议周三 9/23 上午（我方周一前置：secret 生成线下交换 + AGENT_WEBHOOK_URL 配置）；③P3 人力条件裁决（优先并行 2 人，备选串行 MA3 精华聚合先行）。**附件必须带上契约增量清单**：`.agent-contract/docs/community-api-contract-v2-delta.md`（gitignore 目录，邮件附件发出）。

**关键设计**：AD1 处置转发需要机器认证——我方方案已写入回执：专用服务账号 `steward_bot` 提权 ADMIN + 管家侧 refresh 循环（JWT 30min 不能用于 M2M）。联调窗口一并验证。

**管家侧时钟**：AD2 联调（本周）；T-DATA-1 用户数据 API 第 3 周前（对方 R2 依赖，未到位其降级 mock）；加精/置顶 API 随三期。

## 五、待办与悬挂项（按优先级）

1. **T-AI-5 部署走查**（后端主导+前端配合）：compose 增 ai-service+qdrant、ai-service CI job、全链实测、M13 关闭；
2. **T-SKILL-3** 已全关闭；**T-DATA-1** 已立项未开工（对齐时点=管家 X1 完成后的契约 v2 评审，实际可提前）；
3. **上线决策**（最大悬挂项）：CF 公测 vs 国内+ICP——M13 部署跟随上线，建议先上线；
4. 浏览器侧人工项：T-SEC-1 的 Gitee 真登录/绑定回归、T-AI-0/T-LOGIN-BG 的主观走查（视差手感/品牌字避让/粒子收敛度）；
5. 杂项：工作区曾有的 tokens.css 墨阶提亮已入库；CI 弃用警告（setup-java v5/Node24/Ubuntu26）找空窗升级；`.env.example` 的 EMBEDDING_MODEL 仍标 bge-m3 待 spike 修正为 text-embedding-v3。

## 六、验收证据索引（独立复跑入口）

- M13 全链报告：`docs/T-AI-0-RAG选型复核报告.md` + `docs/T-AI-2-问答链路交付.md` + `docs/assets/t-ai-0|t-ai-1|t-ai-2|t-ai-4/`；
- 契约与谈判：`.agent-contract/docs/community-api-contract.md`（v1.0）+ `community-api-contract-v2-delta.md`（增量）；
- 复现：前端三绿 `cd frontend && node node_modules/eslint/bin/eslint.js . && node node_modules/vue-tsc/bin/vue-tsc.js --noEmit && node node_modules/vite/bin/vite.js build`；后端 `mvn.cmd test`（CI 为权威）；ai-service `python -m unittest tests.test_retrieve -v`；spike `python -m scripts.run_spike / bench_report`。
