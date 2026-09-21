-- =====================================================================
-- REAL 热门种子（幂等）：GitHub / 论文博客导读 / Skill 社区
-- 账号：real_alice / real_bob / real_carol ，密码均为 password123
-- 数据来源：
--   1) Desktop deepseek-harness-dashboard.html（dsh-plugin 星标快照）
--   2) 公开 GitHub 热门仓 + Agent Skills 社区仓
--   3) arXiv / HF / 经典博客（仅导读+外链，无全文）
-- 用法：bash seed-real-hot-data.sh
-- =====================================================================

SET NAMES utf8mb4;
SET @demo_pw := '$2a$10$Uyvz95pTPx/qOjTmn7vwXeI.lFVGMTtwCaN4qkGTcU.1jjSYmw3yG';

-- ---------- 1) 清理 real_* ----------
DELETE ul FROM user_like ul
 INNER JOIN `user` u ON u.id = ul.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN post p ON ul.target_type = 'POST' AND ul.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN project_share s ON ul.target_type = 'SHARE' AND ul.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN skill sk ON ul.target_type = 'SKILL' AND ul.target_id = sk.id
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE ul FROM user_like ul
 INNER JOIN comment c ON ul.target_type = 'COMMENT' AND ul.target_id = c.id
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE uf FROM user_favorite uf
 INNER JOIN `user` u ON u.id = uf.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE uf FROM user_favorite uf
 INNER JOIN post p ON uf.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE n FROM notification n
 INNER JOIN `user` u ON u.id = n.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\' AND c.parent_id IS NOT NULL;

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN post p ON c.target_type = 'POST' AND c.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE c FROM comment c
 INNER JOIN project_share s ON c.target_type = 'SHARE' AND c.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE sk FROM skill sk
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE p FROM post p
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE s FROM project_share s
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE r FROM user_repo r
 INNER JOIN `user` u ON u.id = r.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE b FROM user_oauth_binding b
 INNER JOIN `user` u ON u.id = b.user_id
 WHERE u.username LIKE 'real\\_%' ESCAPE '\\';

DELETE FROM `user` WHERE username LIKE 'real\\_%' ESCAPE '\\';

-- ---------- 2) 用户 ----------
INSERT INTO `user` (username, email, password_hash, nickname, bio, tech_stack, role, status) VALUES
('real_alice', 'real_alice@geekstack.local', @demo_pw, 'Alice·开源雷达',
 '收录 GitHub / DSH 真实热门项目', JSON_ARRAY('TypeScript', 'Agent', 'GitHub'), 'USER', 0),
('real_bob', 'real_bob@geekstack.local', @demo_pw, 'Bob·论文速读',
 'arXiv / HF / 博客导读，不搬运全文', JSON_ARRAY('AI', 'RAG', 'Python'), 'USER', 0),
('real_carol', 'real_carol@geekstack.local', @demo_pw, 'Carol·Skill 策展',
 'Agent Skills / DSH 插件社区策展人', JSON_ARRAY('Skill', 'Claude', 'DSH'), 'USER', 0);

SET @alice := (SELECT id FROM `user` WHERE username = 'real_alice');
SET @bob   := (SELECT id FROM `user` WHERE username = 'real_bob');
SET @carol := (SELECT id FROM `user` WHERE username = 'real_carol');
SET @cat_java := (SELECT id FROM post_category WHERE slug = 'java');
SET @cat_ai   := (SELECT id FROM post_category WHERE slug = 'ai');
SET @cat_qa   := (SELECT id FROM post_category WHERE slug = 'qa');
SET @cat_res  := (SELECT id FROM post_category WHERE slug = 'resource');

-- ---------- 3) 论坛帖（论文/博客导读）----------
INSERT INTO post (user_id, category_id, title, content_md, view_count, reply_count, like_count, is_pinned, is_hot) VALUES
(@bob, @cat_ai, '[REAL] 读 DeepSeek-R1：用纯 RL 激励推理能力',
 '## [REAL] 读 DeepSeek-R1：用纯 RL 激励推理能力

**类型**：arxiv
**原文**：https://arxiv.org/abs/2501.12948

### 导读
DeepSeek-R1 / R1-Zero 通过大规模强化学习涌现反思与验证行为，并开源多档蒸馏模型。本文是社区导读帖，正文为摘要+要点，原文见 arXiv。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 9200, 0, 180, 1, 1),
(@bob, @cat_ai, '[REAL] Attention Is All You Need（Transformer 原点）',
 '## [REAL] Attention Is All You Need（Transformer 原点）

**类型**：arxiv
**原文**：https://arxiv.org/abs/1706.03762

### 导读
Vaswani et al. 2017。自注意力替代 RNN/CNN 的序列建模范式，是当今几乎所有 LLM 的结构祖先。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 8800, 0, 160, 0, 1),
(@bob, @cat_ai, '[REAL] RAG 实践札记：检索增强生成怎么落地',
 '## [REAL] RAG 实践札记：检索增强生成怎么落地

**类型**：blog
**原文**：https://www.pinecone.io/learn/retrieval-augmented-generation/

### 导读
从向量库、切分、重排到评测的工程向导读；配合 Qdrant/LangChain 做本地实验。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 6100, 0, 95, 0, 1),
(@bob, @cat_ai, '[REAL] Hugging Face：DeepSeek Moment 一周年观察',
 '## [REAL] Hugging Face：DeepSeek Moment 一周年观察

**类型**：blog
**原文**：https://huggingface.co/blog/huggingface/one-year-since-the-deepseek-moment-blog-3

### 导读
HF 对 2025 DeepSeek 时刻后开源生态的回顾：论文热度、组织关注度与后续开源模型浪潮。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 5400, 0, 88, 0, 1),
(@carol, @cat_res, '[REAL] Agent Skills 规范与 anthropics/skills 速览',
 '## [REAL] Agent Skills 规范与 anthropics/skills 速览

**类型**：github
**原文**：https://github.com/anthropics/skills

### 导读
Skill = 可动态加载的说明/脚本/资源包。官方仓含文档技能、创意技能与 agentskills.io 规范入口。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 7200, 0, 120, 0, 1),
(@alice, @cat_res, '[REAL] DSH 生态热榜快照（来自 deepseek-harness-dashboard）',
 '## [REAL] DSH 生态热榜快照（来自 deepseek-harness-dashboard）

**类型**：local
**原文**：file:///Desktop/deepseek-harness-dashboard.html

### 导读
桌面 DSH 仪表盘导出的 dsh-plugin 主题仓库星标快照：harness 4万+、colleague-skill 2万+，可作为广场种子来源。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 6800, 0, 110, 0, 1),
(@alice, @cat_java, '[REAL] Spring AI 参考：把 LLM 接入 Spring Boot',
 '## [REAL] Spring AI 参考：把 LLM 接入 Spring Boot

**类型**：docs
**原文**：https://docs.spring.io/spring-ai/reference/

### 导读
官方文档导读：ChatClient、Embedding、VectorStore 与 Advisor。适合 Java 栈同学对照极客栈 AI 模块。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 3100, 0, 40, 0, 0),
(@bob, @cat_ai, '[REAL] Karpathy：A Recipe for Training Neural Networks',
 '## [REAL] Karpathy：A Recipe for Training Neural Networks

**类型**：blog
**原文**：https://karpathy.github.io/2019/04/25/recipe/

### 导读
经典训练菜谱：先 overfit 再正则、可视化、学习率与数据管道检查清单。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 4900, 0, 70, 0, 1),
(@carol, @cat_qa, '[REAL] 问：Agent Skill 和 MCP Server 怎么分工？',
 '## [REAL] 问：Agent Skill 和 MCP Server 怎么分工？

**类型**：discuss
**原文**：https://modelcontextprotocol.io/

### 导读
Skill 偏「教 Agent 怎么做某类任务」；MCP 偏「给 Agent 标准化工具/资源接口」。社区讨论帖，欢迎补案例。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 2700, 0, 35, 0, 0),
(@carol, @cat_res, '[REAL] Composio awesome-claude-skills 精选入口',
 '## [REAL] Composio awesome-claude-skills 精选入口

**类型**：github
**原文**：https://github.com/ComposioHQ/awesome-claude-skills

### 导读
社区 awesome 列表：Claude Skills / 资源 / 工具合集，适合当 Skill 广场冷启动目录。

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。',
 4500, 0, 75, 0, 1);

-- ---------- 4) 项目分享（真实 GitHub / DSH）----------
INSERT INTO project_share
  (user_id, title, summary, content_md, cover_url, repo_url, demo_url, tags, view_count, like_count, status)
VALUES
(@alice, 'DeepSeek Harness：Everything is a Plugin', 'TypeScript 插件式 Agent Harness，DSH 生态核心运行时',
 '## DeepSeek Harness：Everything is a Plugin

> 来源快照：GitHub / DSH 仪表盘 · ★≈40,606

**仓库**：[deepseek-ai/deepseek-harness](https://github.com/deepseek-ai/deepseek-harness)

**一句话**：TypeScript 插件式 Agent Harness，DSH 生态核心运行时

**主语言**：TypeScript

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/deepseek-ai/deepseek-harness',
 'https://github.com/deepseek-ai/deepseek-harness', NULL,
 JSON_ARRAY('TypeScript', 'Agent', 'Plugin'), 5075, 507, 'PUBLISHED'),
(@carol, 'colleague-skill：把告别写成可安装 Skill', '数字生命风格的同事 Skill，社区高星代表作',
 '## colleague-skill：把告别写成可安装 Skill

> 来源快照：GitHub / DSH 仪表盘 · ★≈21,086

**仓库**：[titanwings/colleague-skill](https://github.com/titanwings/colleague-skill)

**一句话**：数字生命风格的同事 Skill，社区高星代表作

**主语言**：Python

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/titanwings/colleague-skill',
 'https://github.com/titanwings/colleague-skill', NULL,
 JSON_ARRAY('Python', 'Skill', 'Agent'), 2635, 263, 'PUBLISHED'),
(@alice, 'iPolloWork：自进化 Agent 工作区', '可编辑代码/设计/演示的源码可用 AI Workspace，对接 DSH',
 '## iPolloWork：自进化 Agent 工作区

> 来源快照：GitHub / DSH 仪表盘 · ★≈3,589

**仓库**：[Devin-AXIS/iPolloWork](https://github.com/Devin-AXIS/iPolloWork)

**一句话**：可编辑代码/设计/演示的源码可用 AI Workspace，对接 DSH

**主语言**：HTML

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/Devin-AXIS/iPolloWork',
 'https://github.com/Devin-AXIS/iPolloWork', NULL,
 JSON_ARRAY('HTML', 'Agent', 'Workspace'), 448, 44, 'PUBLISHED'),
(@alice, 'OpenBiliClaw：本地优先内容发现 Agent', '跨 B站/小红书/抖音/YouTube 等平台的私有内容发现 Agent',
 '## OpenBiliClaw：本地优先内容发现 Agent

> 来源快照：GitHub / DSH 仪表盘 · ★≈1,939

**仓库**：[whiteguo233/OpenBiliClaw](https://github.com/whiteguo233/OpenBiliClaw)

**一句话**：跨 B站/小红书/抖音/YouTube 等平台的私有内容发现 Agent

**主语言**：Python

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/whiteguo233/OpenBiliClaw',
 'https://github.com/whiteguo233/OpenBiliClaw', NULL,
 JSON_ARRAY('Python', 'Agent', 'Content'), 242, 24, 'PUBLISHED'),
(@alice, 'deeptide：为 DeepSeek 打造的 macOS Coding Agent', 'Swift-native macOS coding agent，Built by DeepSeek for DeepSeek',
 '## deeptide：为 DeepSeek 打造的 macOS Coding Agent

> 来源快照：GitHub / DSH 仪表盘 · ★≈1,018

**仓库**：[paean-ai/deeptide](https://github.com/paean-ai/deeptide)

**一句话**：Swift-native macOS coding agent，Built by DeepSeek for DeepSeek

**主语言**：Rust

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/paean-ai/deeptide',
 'https://github.com/paean-ai/deeptide', NULL,
 JSON_ARRAY('Swift', 'macOS', 'Agent'), 127, 12, 'PUBLISHED'),
(@carol, 'modlens：DSH 首个视觉插件', '给纯文本 Coding Agent 的视觉桥：粘贴图片→结构化 JSON 证据',
 '## modlens：DSH 首个视觉插件

> 来源快照：GitHub / DSH 仪表盘 · ★≈711

**仓库**：[liustack/modlens](https://github.com/liustack/modlens)

**一句话**：给纯文本 Coding Agent 的视觉桥：粘贴图片→结构化 JSON 证据

**主语言**：TypeScript

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/liustack/modlens',
 'https://github.com/liustack/modlens', NULL,
 JSON_ARRAY('TypeScript', 'Vision', 'Plugin'), 100, 8, 'PUBLISHED'),
(@carol, 'dsh-web-ui：DSH Web UI 插件与皮肤集', '任务板/Git 图/侧栏/远程移动端/皮肤中心',
 '## dsh-web-ui：DSH Web UI 插件与皮肤集

> 来源快照：GitHub / DSH 仪表盘 · ★≈542

**仓库**：[zhu1090093659/dsh-web-ui](https://github.com/zhu1090093659/dsh-web-ui)

**一句话**：任务板/Git 图/侧栏/远程移动端/皮肤中心

**主语言**：TypeScript

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/zhu1090093659/dsh-web-ui',
 'https://github.com/zhu1090093659/dsh-web-ui', NULL,
 JSON_ARRAY('TypeScript', 'UI', 'Plugin'), 100, 6, 'PUBLISHED'),
(@carol, 'agent-vision-toolkit：文本模型看图工具箱', '多图理解、OCR、UI 还原、GUI 自动化，可接入主流 Agent',
 '## agent-vision-toolkit：文本模型看图工具箱

> 来源快照：GitHub / DSH 仪表盘 · ★≈531

**仓库**：[Anionex/agent-vision-toolkit](https://github.com/Anionex/agent-vision-toolkit)

**一句话**：多图理解、OCR、UI 还原、GUI 自动化，可接入主流 Agent

**主语言**：Python

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/Anionex/agent-vision-toolkit',
 'https://github.com/Anionex/agent-vision-toolkit', NULL,
 JSON_ARRAY('Python', 'Vision', 'Skill'), 100, 6, 'PUBLISHED'),
(@alice, 'awesome-dsh-plugins：DSH 插件雷达索引', '自动扫描 dsh-plugin 候选，合格后进入精选目录',
 '## awesome-dsh-plugins：DSH 插件雷达索引

> 来源快照：GitHub / DSH 仪表盘 · ★≈314

**仓库**：[AdamPlatin123/awesome-dsh-plugins](https://github.com/AdamPlatin123/awesome-dsh-plugins)

**一句话**：自动扫描 dsh-plugin 候选，合格后进入精选目录

**主语言**：Shell

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/AdamPlatin123/awesome-dsh-plugins',
 'https://github.com/AdamPlatin123/awesome-dsh-plugins', NULL,
 JSON_ARRAY('Shell', 'Catalog', 'Plugin'), 100, 5, 'PUBLISHED'),
(@alice, 'mobius：自进化开源 Agent OS', '连接团队、AI agents、设备与算力的 Agent OS',
 '## mobius：自进化开源 Agent OS

> 来源快照：GitHub / DSH 仪表盘 · ★≈917

**仓库**：[nutshellai-tech/mobius](https://github.com/nutshellai-tech/mobius)

**一句话**：连接团队、AI agents、设备与算力的 Agent OS

**主语言**：TypeScript

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/nutshellai-tech/mobius',
 'https://github.com/nutshellai-tech/mobius', NULL,
 JSON_ARRAY('TypeScript', 'OS', 'Agent'), 114, 11, 'PUBLISHED'),
(@carol, 'anthropics/skills：官方 Agent Skills 样例库', 'Anthropic 公开 Agent Skills 规范与文档/创意/企业工作流样例',
 '## anthropics/skills：官方 Agent Skills 样例库

> 来源快照：GitHub / DSH 仪表盘 · ★≈172,000

**仓库**：[anthropics/skills](https://github.com/anthropics/skills)

**一句话**：Anthropic 公开 Agent Skills 规范与文档/创意/企业工作流样例

**主语言**：Skill

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/anthropics/skills',
 'https://github.com/anthropics/skills', NULL,
 JSON_ARRAY('Skill', 'Claude', 'Spec'), 21500, 2150, 'PUBLISHED'),
(@bob, 'LangChain：LLM 应用编排框架', '链式调用、Agent、RAG 工具生态的事实标准之一',
 '## LangChain：LLM 应用编排框架

> 来源快照：GitHub / DSH 仪表盘 · ★≈110,000

**仓库**：[langchain-ai/langchain](https://github.com/langchain-ai/langchain)

**一句话**：链式调用、Agent、RAG 工具生态的事实标准之一

**主语言**：Python

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/langchain-ai/langchain',
 'https://github.com/langchain-ai/langchain', NULL,
 JSON_ARRAY('Python', 'LLM', 'RAG'), 13750, 1375, 'PUBLISHED'),
(@bob, 'vLLM：高吞吐 LLM 推理引擎', 'PagedAttention 驱动的开源推理服务，生产部署常用',
 '## vLLM：高吞吐 LLM 推理引擎

> 来源快照：GitHub / DSH 仪表盘 · ★≈55,000

**仓库**：[vllm-project/vllm](https://github.com/vllm-project/vllm)

**一句话**：PagedAttention 驱动的开源推理服务，生产部署常用

**主语言**：Python

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/vllm-project/vllm',
 'https://github.com/vllm-project/vllm', NULL,
 JSON_ARRAY('Python', 'Inference', 'CUDA'), 6875, 687, 'PUBLISHED'),
(@bob, 'Qdrant：向量数据库', 'Rust 实现的高性能向量检索，极客栈 RAG 同款组件',
 '## Qdrant：向量数据库

> 来源快照：GitHub / DSH 仪表盘 · ★≈25,000

**仓库**：[qdrant/qdrant](https://github.com/qdrant/qdrant)

**一句话**：Rust 实现的高性能向量检索，极客栈 RAG 同款组件

**主语言**：Rust

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/qdrant/qdrant',
 'https://github.com/qdrant/qdrant', NULL,
 JSON_ARRAY('Rust', 'Vector', 'RAG'), 3125, 312, 'PUBLISHED'),
(@bob, 'DeepSeek-R1：推理模型开源仓', 'R1/R1-Zero 与蒸馏模型发布仓，配套 arXiv:2501.12948',
 '## DeepSeek-R1：推理模型开源仓

> 来源快照：GitHub / DSH 仪表盘 · ★≈90,000

**仓库**：[deepseek-ai/DeepSeek-R1](https://github.com/deepseek-ai/DeepSeek-R1)

**一句话**：R1/R1-Zero 与蒸馏模型发布仓，配套 arXiv:2501.12948

**主语言**：LLM

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。',
 'https://opengraph.githubassets.com/1/deepseek-ai/DeepSeek-R1',
 'https://github.com/deepseek-ai/DeepSeek-R1', NULL,
 JSON_ARRAY('LLM', 'RL', 'Reasoning'), 11250, 1125, 'PUBLISHED');

-- ---------- 5) Skill 专区（社区热门）----------
INSERT INTO skill
  (user_id, title, summary, content_md, model_adaptation_md, tags, view_count, like_count, status)
VALUES
(@carol, 'SKILL: Document Skills（anthropics）', '官方文档类 Skill：docx/pdf/pptx/xlsx 创建与编辑工作流样例',
 '# SKILL: Document Skills（anthropics）

官方文档类 Skill：docx/pdf/pptx/xlsx 创建与编辑工作流样例

来源：https://github.com/anthropics/skills

## 用途
教 Agent 按可重复流程处理办公文档。

## 安装提示
在 Claude Code / 兼容运行时注册 marketplace 后按官方 README 启用。

## 模型适配
Claude / 兼容 Agent Skills 运行时

## 社区点赞参考
演示 like_count≈9800（可按真实热度再调）
',
 'Claude / 兼容 Agent Skills 运行时',
 JSON_ARRAY('Skill', 'Document', 'Official'), 117600, 9800, 'PUBLISHED'),
(@alice, 'SKILL: colleague-skill 数字同事', '把「同事」封装为可安装 Skill，社区热度极高',
 '# SKILL: colleague-skill 数字同事

把「同事」封装为可安装 Skill，社区热度极高

来源：https://github.com/titanwings/colleague-skill

## 用途
人格化协作、告别仪式感、可复用提示与脚本。

## 测试点
安装 → 触发问候/协作指令 → 观察 Skill 加载与工具调用。

## 模型适配
Claude / DSH / 通用 Skill 宿主

## 社区点赞参考
演示 like_count≈8500（可按真实热度再调）
',
 'Claude / DSH / 通用 Skill 宿主',
 JSON_ARRAY('Skill', 'Persona', 'Python'), 102000, 8500, 'PUBLISHED'),
(@carol, 'SKILL: UI/UX Pro Max', '为专业 UI/UX 提供设计智能的 Agent Skill',
 '# SKILL: UI/UX Pro Max

为专业 UI/UX 提供设计智能的 Agent Skill

来源：https://github.com/nextlevelbuilder/ui-ux-pro-max-skill

## 用途
落地页/管理端布局、对比度与组件层级建议。

## 测试点
给一张需求 → 产出信息架构与视觉约束清单。

## 模型适配
Claude Code / Cursor

## 社区点赞参考
演示 like_count≈7200（可按真实热度再调）
',
 'Claude Code / Cursor',
 JSON_ARRAY('Skill', 'UI', 'Design'), 86400, 7200, 'PUBLISHED'),
(@alice, 'SKILL: Andrej Karpathy Skills（CLAUDE.md）', '单文件 CLAUDE.md 改进 Coding Agent 行为',
 '# SKILL: Andrej Karpathy Skills（CLAUDE.md）

单文件 CLAUDE.md 改进 Coding Agent 行为

来源：https://github.com/multica-ai/andrej-karpathy-skills

## 用途
针对 LLM 编码常见坑（幻觉 API、过度重构）给出约束。

## 模型适配
Claude Code

## 社区点赞参考
演示 like_count≈6900（可按真实热度再调）
',
 'Claude Code',
 JSON_ARRAY('Skill', 'Coding', 'Prompt'), 82800, 6900, 'PUBLISHED'),
(@carol, 'SKILL: Agent Vision Toolkit', '纯文本模型看图：OCR / UI 还原 / GUI 自动化',
 '# SKILL: Agent Vision Toolkit

纯文本模型看图：OCR / UI 还原 / GUI 自动化

来源：https://github.com/Anionex/agent-vision-toolkit

## 用途
粘贴截图 → 结构化证据 JSON。

## 测试点
前端还原、长截图 OCR、多图问答。

## 模型适配
DSH / Claude / Cursor

## 社区点赞参考
演示 like_count≈5300（可按真实热度再调）
',
 'DSH / Claude / Cursor',
 JSON_ARRAY('Skill', 'Vision', 'OCR'), 63600, 5300, 'PUBLISHED'),
(@alice, 'SKILL: modlens（DSH Vision Plugin）', 'DSH 原生视觉插件：图片→结构化证据',
 '# SKILL: modlens（DSH Vision Plugin）

DSH 原生视觉插件：图片→结构化证据

来源：https://github.com/liustack/modlens

## 用途
作为 dsh-plugin 安装，给文本-only agent 补视觉。

## 模型适配
DeepSeek Harness

## 社区点赞参考
演示 like_count≈4800（可按真实热度再调）
',
 'DeepSeek Harness',
 JSON_ARRAY('Plugin', 'Vision', 'DSH'), 57600, 4800, 'PUBLISHED'),
(@carol, 'SKILL: caveman token 压缩', '用极少 token 说话，降低上下文消耗的 Skill',
 '# SKILL: caveman token 压缩

用极少 token 说话，降低上下文消耗的 Skill

来源：https://github.com/JuliusBrussee/caveman

## 用途
长会话成本控制；输出仍可被 Agent 理解。

## 模型适配
Claude Code

## 社区点赞参考
演示 like_count≈4500（可按真实热度再调）
',
 'Claude Code',
 JSON_ARRAY('Skill', 'Token', 'Efficiency'), 54000, 4500, 'PUBLISHED'),
(@alice, 'SKILL: addyosmani/agent-skills', '生产级工程 Skill 合集（前端/性能/可访问性）',
 '# SKILL: addyosmani/agent-skills

生产级工程 Skill 合集（前端/性能/可访问性）

来源：https://github.com/addyosmani/agent-skills

## 用途
Web 性能、工程实践检查清单式 Skill。

## 模型适配
Claude / Antigravity

## 社区点赞参考
演示 like_count≈4200（可按真实热度再调）
',
 'Claude / Antigravity',
 JSON_ARRAY('Skill', 'Engineering', 'Web'), 50400, 4200, 'PUBLISHED'),
(@carol, 'SKILL: ProMentor 导师模式', '扫描架构、生成阶梯 Chapter、判题与 Code Review',
 '# SKILL: ProMentor 导师模式

扫描架构、生成阶梯 Chapter、判题与 Code Review

来源：https://github.com/Lyn-77/ProMentor

## 用途
把 Coding Agent 变成带练习的导师。

## 模型适配
Claude / Cursor / Codex

## 社区点赞参考
演示 like_count≈2100（可按真实热度再调）
',
 'Claude / Cursor / Codex',
 JSON_ARRAY('Skill', 'Education', 'Review'), 25200, 2100, 'PUBLISHED'),
(@alice, 'SKILL: graphify 知识图谱化仓库', '把代码/SQL/文档变成可查询知识图谱',
 '# SKILL: graphify 知识图谱化仓库

把代码/SQL/文档变成可查询知识图谱

来源：https://github.com/safishamsi/graphify

## 用途
跨文件依赖理解、架构问答。

## 模型适配
Claude Code / Cursor / Gemini CLI

## 社区点赞参考
演示 like_count≈3900（可按真实热度再调）
',
 'Claude Code / Cursor / Gemini CLI',
 JSON_ARRAY('Skill', 'Graph', 'Codebase'), 46800, 3900, 'PUBLISHED'),
(@carol, 'SKILL: DSH Web UI 工作台扩展', '任务板、Git 图、侧栏 Tab、皮肤中心',
 '# SKILL: DSH Web UI 工作台扩展

任务板、Git 图、侧栏 Tab、皮肤中心

来源：https://github.com/zhu1090093659/dsh-web-ui

## 用途
提升 DSH Web 交互面；可测插件安装与皮肤切换。

## 模型适配
DeepSeek Harness Web UI

## 社区点赞参考
演示 like_count≈3000（可按真实热度再调）
',
 'DeepSeek Harness Web UI',
 JSON_ARRAY('Plugin', 'UI', 'DSH'), 36000, 3000, 'PUBLISHED'),
(@alice, 'SKILL: harmony-next.skills', 'HarmonyOS NEXT 开发专家 Skill',
 '# SKILL: harmony-next.skills

HarmonyOS NEXT 开发专家 Skill

来源：https://github.com/linhay/harmony-next.skills

## 用途
API12+ IDE、性能、HAP/HAR/HSP、自动化测试指导。

## 模型适配
Claude / 通用 Skill 宿主

## 社区点赞参考
演示 like_count≈1800（可按真实热度再调）
',
 'Claude / 通用 Skill 宿主',
 JSON_ARRAY('Skill', 'HarmonyOS', 'Mobile'), 21600, 1800, 'PUBLISHED');

-- ---------- 6) 评论 ----------
SET @p_r1 := (SELECT id FROM post WHERE title = '[REAL] 读 DeepSeek-R1：用纯 RL 激励推理能力' LIMIT 1);
SET @p_dsh := (SELECT id FROM post WHERE title LIKE '[REAL] DSH 生态热榜快照%' LIMIT 1);
SET @s_harness := (SELECT id FROM project_share WHERE title LIKE 'DeepSeek Harness%' LIMIT 1);
SET @s_skills := (SELECT id FROM project_share WHERE title LIKE 'anthropics/skills%' LIMIT 1);

INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count) VALUES
('POST', @p_r1, @alice, NULL, '导读帖很清晰，已去 arXiv 看原文摘要。', 12),
('POST', @p_r1, @carol, NULL, '蒸馏小模型那段对本地实验很有用。', 8),
('POST', @p_dsh, @bob, NULL, '仪表盘 stars 和仓库页对得上，种子可信。', 6),
('SHARE', @s_harness, @carol, NULL, '插件市场玩法很像 Everything is a Plugin。', 15),
('SHARE', @s_skills, @alice, NULL, '官方 Skill 规范值得当极客栈 Skill 专区标杆。', 11);

SET @c_r1 := (SELECT id FROM comment WHERE content_md LIKE '导读帖很清晰%' LIMIT 1);
INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count) VALUES
('POST', @p_r1, @bob, @c_r1, '楼中楼：Nature 正式刊出后 DOI 也补进帖子了。', 3);

UPDATE post p
   SET reply_count = (
       SELECT COUNT(*) FROM comment c
        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0),
       last_reply_at = (
       SELECT MAX(created_at) FROM comment c
        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0)
 WHERE p.user_id IN (@alice, @bob, @carol);

INSERT INTO user_like (user_id, target_type, target_id)
SELECT @alice, 'POST', @p_r1 FROM DUAL WHERE @p_r1 IS NOT NULL
UNION ALL SELECT @carol, 'POST', @p_r1 FROM DUAL WHERE @p_r1 IS NOT NULL
UNION ALL SELECT @bob, 'SHARE', @s_harness FROM DUAL WHERE @s_harness IS NOT NULL
UNION ALL SELECT @alice, 'SHARE', @s_skills FROM DUAL WHERE @s_skills IS NOT NULL;

SELECT 'seed-real-hot-data OK' AS msg,
  (SELECT COUNT(*) FROM `user` WHERE username LIKE 'real\\_%' ESCAPE '\\') AS users,
  (SELECT COUNT(*) FROM post p JOIN `user` u ON u.id=p.user_id WHERE u.username LIKE 'real\\_%' ESCAPE '\\') AS posts,
  (SELECT COUNT(*) FROM project_share s JOIN `user` u ON u.id=s.user_id WHERE u.username LIKE 'real\\_%' ESCAPE '\\') AS shares,
  (SELECT COUNT(*) FROM skill sk JOIN `user` u ON u.id=sk.user_id WHERE u.username LIKE 'real\\_%' ESCAPE '\\') AS skills;
