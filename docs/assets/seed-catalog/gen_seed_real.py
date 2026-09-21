# -*- coding: utf-8 -*-
"""Generate seed-real-hot-data.sql + README catalog from curated lists + DSH JSON."""
import json
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[2]  # 2026-09-06-18-49-57
dsh = json.loads((HERE / "dsh-top50.json").read_text(encoding="utf-8"))

shares = [
    ("deepseek-ai/deepseek-harness", "DeepSeek Harness：Everything is a Plugin",
     "TypeScript 插件式 Agent Harness，DSH 生态核心运行时", ["TypeScript", "Agent", "Plugin"], 40606),
    ("titanwings/colleague-skill", "colleague-skill：把告别写成可安装 Skill",
     "数字生命风格的同事 Skill，社区高星代表作", ["Python", "Skill", "Agent"], 21086),
    ("Devin-AXIS/iPolloWork", "iPolloWork：自进化 Agent 工作区",
     "可编辑代码/设计/演示的源码可用 AI Workspace，对接 DSH", ["HTML", "Agent", "Workspace"], 3589),
    ("whiteguo233/OpenBiliClaw", "OpenBiliClaw：本地优先内容发现 Agent",
     "跨 B站/小红书/抖音/YouTube 等平台的私有内容发现 Agent", ["Python", "Agent", "Content"], 1939),
    ("paean-ai/deeptide", "deeptide：为 DeepSeek 打造的 macOS Coding Agent",
     "Swift-native macOS coding agent，Built by DeepSeek for DeepSeek", ["Swift", "macOS", "Agent"], 1018),
    ("liustack/modlens", "modlens：DSH 首个视觉插件",
     "给纯文本 Coding Agent 的视觉桥：粘贴图片→结构化 JSON 证据", ["TypeScript", "Vision", "Plugin"], 711),
    ("zhu1090093659/dsh-web-ui", "dsh-web-ui：DSH Web UI 插件与皮肤集",
     "任务板/Git 图/侧栏/远程移动端/皮肤中心", ["TypeScript", "UI", "Plugin"], 542),
    ("Anionex/agent-vision-toolkit", "agent-vision-toolkit：文本模型看图工具箱",
     "多图理解、OCR、UI 还原、GUI 自动化，可接入主流 Agent", ["Python", "Vision", "Skill"], 531),
    ("AdamPlatin123/awesome-dsh-plugins", "awesome-dsh-plugins：DSH 插件雷达索引",
     "自动扫描 dsh-plugin 候选，合格后进入精选目录", ["Shell", "Catalog", "Plugin"], 314),
    ("nutshellai-tech/mobius", "mobius：自进化开源 Agent OS",
     "连接团队、AI agents、设备与算力的 Agent OS", ["TypeScript", "OS", "Agent"], 917),
    ("anthropics/skills", "anthropics/skills：官方 Agent Skills 样例库",
     "Anthropic 公开 Agent Skills 规范与文档/创意/企业工作流样例", ["Skill", "Claude", "Spec"], 172000),
    ("langchain-ai/langchain", "LangChain：LLM 应用编排框架",
     "链式调用、Agent、RAG 工具生态的事实标准之一", ["Python", "LLM", "RAG"], 110000),
    ("vllm-project/vllm", "vLLM：高吞吐 LLM 推理引擎",
     "PagedAttention 驱动的开源推理服务，生产部署常用", ["Python", "Inference", "CUDA"], 55000),
    ("qdrant/qdrant", "Qdrant：向量数据库",
     "Rust 实现的高性能向量检索，极客栈 RAG 同款组件", ["Rust", "Vector", "RAG"], 25000),
    ("deepseek-ai/DeepSeek-R1", "DeepSeek-R1：推理模型开源仓",
     "R1/R1-Zero 与蒸馏模型发布仓，配套 arXiv:2501.12948", ["LLM", "RL", "Reasoning"], 90000),
]

posts = [
    ("ai", True, True, "[REAL] 读 DeepSeek-R1：用纯 RL 激励推理能力", "arxiv",
     "https://arxiv.org/abs/2501.12948",
     "DeepSeek-R1 / R1-Zero 通过大规模强化学习涌现反思与验证行为，并开源多档蒸馏模型。本文是社区导读帖，正文为摘要+要点，原文见 arXiv。"),
    ("ai", False, True, "[REAL] Attention Is All You Need（Transformer 原点）", "arxiv",
     "https://arxiv.org/abs/1706.03762",
     "Vaswani et al. 2017。自注意力替代 RNN/CNN 的序列建模范式，是当今几乎所有 LLM 的结构祖先。"),
    ("ai", False, True, "[REAL] RAG 实践札记：检索增强生成怎么落地", "blog",
     "https://www.pinecone.io/learn/retrieval-augmented-generation/",
     "从向量库、切分、重排到评测的工程向导读；配合 Qdrant/LangChain 做本地实验。"),
    ("ai", False, True, "[REAL] Hugging Face：DeepSeek Moment 一周年观察", "blog",
     "https://huggingface.co/blog/huggingface/one-year-since-the-deepseek-moment-blog-3",
     "HF 对 2025 DeepSeek 时刻后开源生态的回顾：论文热度、组织关注度与后续开源模型浪潮。"),
    ("resource", False, True, "[REAL] Agent Skills 规范与 anthropics/skills 速览", "github",
     "https://github.com/anthropics/skills",
     "Skill = 可动态加载的说明/脚本/资源包。官方仓含文档技能、创意技能与 agentskills.io 规范入口。"),
    ("resource", False, True, "[REAL] DSH 生态热榜快照（来自 deepseek-harness-dashboard）", "local",
     "file:///Desktop/deepseek-harness-dashboard.html",
     "桌面 DSH 仪表盘导出的 dsh-plugin 主题仓库星标快照：harness 4万+、colleague-skill 2万+，可作为广场种子来源。"),
    ("java", False, False, "[REAL] Spring AI 参考：把 LLM 接入 Spring Boot", "docs",
     "https://docs.spring.io/spring-ai/reference/",
     "官方文档导读：ChatClient、Embedding、VectorStore 与 Advisor。适合 Java 栈同学对照极客栈 AI 模块。"),
    ("ai", False, True, "[REAL] Karpathy：A Recipe for Training Neural Networks", "blog",
     "https://karpathy.github.io/2019/04/25/recipe/",
     "经典训练菜谱：先 overfit 再正则、可视化、学习率与数据管道检查清单。"),
    ("qa", False, False, "[REAL] 问：Agent Skill 和 MCP Server 怎么分工？", "discuss",
     "https://modelcontextprotocol.io/",
     "Skill 偏「教 Agent 怎么做某类任务」；MCP 偏「给 Agent 标准化工具/资源接口」。社区讨论帖，欢迎补案例。"),
    ("resource", False, True, "[REAL] Composio awesome-claude-skills 精选入口", "github",
     "https://github.com/ComposioHQ/awesome-claude-skills",
     "社区 awesome 列表：Claude Skills / 资源 / 工具合集，适合当 Skill 广场冷启动目录。"),
]

skills = [
    ("SKILL: Document Skills（anthropics）",
     "官方文档类 Skill：docx/pdf/pptx/xlsx 创建与编辑工作流样例",
     "来源：https://github.com/anthropics/skills\n\n## 用途\n教 Agent 按可重复流程处理办公文档。\n\n## 安装提示\n在 Claude Code / 兼容运行时注册 marketplace 后按官方 README 启用。",
     "Claude / 兼容 Agent Skills 运行时", ["Skill", "Document", "Official"], 9800),
    ("SKILL: colleague-skill 数字同事",
     "把「同事」封装为可安装 Skill，社区热度极高",
     "来源：https://github.com/titanwings/colleague-skill\n\n## 用途\n人格化协作、告别仪式感、可复用提示与脚本。\n\n## 测试点\n安装 → 触发问候/协作指令 → 观察 Skill 加载与工具调用。",
     "Claude / DSH / 通用 Skill 宿主", ["Skill", "Persona", "Python"], 8500),
    ("SKILL: UI/UX Pro Max",
     "为专业 UI/UX 提供设计智能的 Agent Skill",
     "来源：https://github.com/nextlevelbuilder/ui-ux-pro-max-skill\n\n## 用途\n落地页/管理端布局、对比度与组件层级建议。\n\n## 测试点\n给一张需求 → 产出信息架构与视觉约束清单。",
     "Claude Code / Cursor", ["Skill", "UI", "Design"], 7200),
    ("SKILL: Andrej Karpathy Skills（CLAUDE.md）",
     "单文件 CLAUDE.md 改进 Coding Agent 行为",
     "来源：https://github.com/multica-ai/andrej-karpathy-skills\n\n## 用途\n针对 LLM 编码常见坑（幻觉 API、过度重构）给出约束。",
     "Claude Code", ["Skill", "Coding", "Prompt"], 6900),
    ("SKILL: Agent Vision Toolkit",
     "纯文本模型看图：OCR / UI 还原 / GUI 自动化",
     "来源：https://github.com/Anionex/agent-vision-toolkit\n\n## 用途\n粘贴截图 → 结构化证据 JSON。\n\n## 测试点\n前端还原、长截图 OCR、多图问答。",
     "DSH / Claude / Cursor", ["Skill", "Vision", "OCR"], 5300),
    ("SKILL: modlens（DSH Vision Plugin）",
     "DSH 原生视觉插件：图片→结构化证据",
     "来源：https://github.com/liustack/modlens\n\n## 用途\n作为 dsh-plugin 安装，给文本-only agent 补视觉。",
     "DeepSeek Harness", ["Plugin", "Vision", "DSH"], 4800),
    ("SKILL: caveman token 压缩",
     "用极少 token 说话，降低上下文消耗的 Skill",
     "来源：https://github.com/JuliusBrussee/caveman\n\n## 用途\n长会话成本控制；输出仍可被 Agent 理解。",
     "Claude Code", ["Skill", "Token", "Efficiency"], 4500),
    ("SKILL: addyosmani/agent-skills",
     "生产级工程 Skill 合集（前端/性能/可访问性）",
     "来源：https://github.com/addyosmani/agent-skills\n\n## 用途\nWeb 性能、工程实践检查清单式 Skill。",
     "Claude / Antigravity", ["Skill", "Engineering", "Web"], 4200),
    ("SKILL: ProMentor 导师模式",
     "扫描架构、生成阶梯 Chapter、判题与 Code Review",
     "来源：https://github.com/Lyn-77/ProMentor\n\n## 用途\n把 Coding Agent 变成带练习的导师。",
     "Claude / Cursor / Codex", ["Skill", "Education", "Review"], 2100),
    ("SKILL: graphify 知识图谱化仓库",
     "把代码/SQL/文档变成可查询知识图谱",
     "来源：https://github.com/safishamsi/graphify\n\n## 用途\n跨文件依赖理解、架构问答。",
     "Claude Code / Cursor / Gemini CLI", ["Skill", "Graph", "Codebase"], 3900),
    ("SKILL: DSH Web UI 工作台扩展",
     "任务板、Git 图、侧栏 Tab、皮肤中心",
     "来源：https://github.com/zhu1090093659/dsh-web-ui\n\n## 用途\n提升 DSH Web 交互面；可测插件安装与皮肤切换。",
     "DeepSeek Harness Web UI", ["Plugin", "UI", "DSH"], 3000),
    ("SKILL: harmony-next.skills",
     "HarmonyOS NEXT 开发专家 Skill",
     "来源：https://github.com/linhay/harmony-next.skills\n\n## 用途\nAPI12+ IDE、性能、HAP/HAR/HSP、自动化测试指导。",
     "Claude / 通用 Skill 宿主", ["Skill", "HarmonyOS", "Mobile"], 1800),
]


def esc(s: str) -> str:
    return s.replace("\\", "\\\\").replace("'", "''")


def md_share(full, title, summary, lang, stars):
    return f"""## {title}

> 来源快照：GitHub / DSH 仪表盘 · ★≈{stars:,}

**仓库**：[{full}](https://github.com/{full})

**一句话**：{summary}

**主语言**：{lang or 'n/a'}

### 社区测试建议
1. 打开 README，确认安装/插件入口
2. 若为 dsh-plugin：按仪表盘链接一键查看 stars/forks
3. 在极客栈广场核对标签与外链可点

### 免责声明
正文为社区导读摘要，非官方镜像；以 GitHub 原文为准。"""


def md_post(title, kind, url, blurb):
    return f"""## {title}

**类型**：{kind}
**原文**：{url}

### 导读
{blurb}

### 可测点（极客栈）
- 帖子列表卡片标题/热门徽标
- Markdown 外链渲染
- 双检索：标题关键词 + 正文摘要命中
- Steward 事件：发帖/点赞/评论（若已开 webhook）

### 版权说明
仅保留导读与外链，不粘贴论文/博客全文。"""


def md_skill(title, summary, body, model, likes):
    return f"""# {title}

{summary}

{body}

## 模型适配
{model}

## 社区点赞参考
演示 like_count≈{likes}（可按真实热度再调）
"""


# 与 seed-demo-data.sql 一致：文件里写 demo\\_% / ESCAPE '\\'，MySQL 解析后为 \_ + ESCAPE \
CLEANUP = """
DELETE ul FROM user_like ul
 INNER JOIN `user` u ON u.id = ul.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE ul FROM user_like ul
 INNER JOIN post p ON ul.target_type = 'POST' AND ul.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE ul FROM user_like ul
 INNER JOIN project_share s ON ul.target_type = 'SHARE' AND ul.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE ul FROM user_like ul
 INNER JOIN skill sk ON ul.target_type = 'SKILL' AND ul.target_id = sk.id
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE ul FROM user_like ul
 INNER JOIN comment c ON ul.target_type = 'COMMENT' AND ul.target_id = c.id
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE uf FROM user_favorite uf
 INNER JOIN `user` u ON u.id = uf.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE uf FROM user_favorite uf
 INNER JOIN post p ON uf.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE n FROM notification n
 INNER JOIN `user` u ON u.id = n.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\' AND c.parent_id IS NOT NULL;

DELETE c FROM comment c
 INNER JOIN `user` u ON u.id = c.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE c FROM comment c
 INNER JOIN post p ON c.target_type = 'POST' AND c.target_id = p.id
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE c FROM comment c
 INNER JOIN project_share s ON c.target_type = 'SHARE' AND c.target_id = s.id
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE sk FROM skill sk
 INNER JOIN `user` u ON u.id = sk.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE p FROM post p
 INNER JOIN `user` u ON u.id = p.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE s FROM project_share s
 INNER JOIN `user` u ON u.id = s.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE r FROM user_repo r
 INNER JOIN `user` u ON u.id = r.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE b FROM user_oauth_binding b
 INNER JOIN `user` u ON u.id = b.user_id
 WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\';

DELETE FROM `user` WHERE username LIKE 'real\\\\_%' ESCAPE '\\\\';
""".strip()


def main():
    lines = [
        "-- =====================================================================",
        "-- REAL 热门种子（幂等）：GitHub / 论文博客导读 / Skill 社区",
        "-- 账号：real_alice / real_bob / real_carol ，密码均为 password123",
        "-- 数据来源：",
        "--   1) Desktop deepseek-harness-dashboard.html（dsh-plugin 星标快照）",
        "--   2) 公开 GitHub 热门仓 + Agent Skills 社区仓",
        "--   3) arXiv / HF / 经典博客（仅导读+外链，无全文）",
        "-- 用法：bash seed-real-hot-data.sh",
        "-- =====================================================================",
        "",
        "SET NAMES utf8mb4;",
        "SET @demo_pw := '$2a$10$Uyvz95pTPx/qOjTmn7vwXeI.lFVGMTtwCaN4qkGTcU.1jjSYmw3yG';",
        "",
        "-- ---------- 1) 清理 real_* ----------",
        CLEANUP,
        "",
        "-- ---------- 2) 用户 ----------",
        "INSERT INTO `user` (username, email, password_hash, nickname, bio, tech_stack, role, status) VALUES",
        "('real_alice', 'real_alice@geekstack.local', @demo_pw, 'Alice·开源雷达',",
        " '收录 GitHub / DSH 真实热门项目', JSON_ARRAY('TypeScript', 'Agent', 'GitHub'), 'USER', 0),",
        "('real_bob', 'real_bob@geekstack.local', @demo_pw, 'Bob·论文速读',",
        " 'arXiv / HF / 博客导读，不搬运全文', JSON_ARRAY('AI', 'RAG', 'Python'), 'USER', 0),",
        "('real_carol', 'real_carol@geekstack.local', @demo_pw, 'Carol·Skill 策展',",
        " 'Agent Skills / DSH 插件社区策展人', JSON_ARRAY('Skill', 'Claude', 'DSH'), 'USER', 0);",
        "",
        "SET @alice := (SELECT id FROM `user` WHERE username = 'real_alice');",
        "SET @bob   := (SELECT id FROM `user` WHERE username = 'real_bob');",
        "SET @carol := (SELECT id FROM `user` WHERE username = 'real_carol');",
        "SET @cat_java := (SELECT id FROM post_category WHERE slug = 'java');",
        "SET @cat_ai   := (SELECT id FROM post_category WHERE slug = 'ai');",
        "SET @cat_qa   := (SELECT id FROM post_category WHERE slug = 'qa');",
        "SET @cat_res  := (SELECT id FROM post_category WHERE slug = 'resource');",
        "",
        "-- ---------- 3) 论坛帖（论文/博客导读）----------",
        "INSERT INTO post (user_id, category_id, title, content_md, view_count, reply_count, like_count, is_pinned, is_hot) VALUES",
    ]

    authors = ["@bob", "@bob", "@bob", "@bob", "@carol", "@alice", "@alice", "@bob", "@carol", "@carol"]
    views = [9200, 8800, 6100, 5400, 7200, 6800, 3100, 4900, 2700, 4500]
    likes = [180, 160, 95, 88, 120, 110, 40, 70, 35, 75]
    post_vals = []
    for i, (cat, pin, hot, title, kind, url, blurb) in enumerate(posts):
        catv = {"java": "@cat_java", "ai": "@cat_ai", "qa": "@cat_qa", "resource": "@cat_res"}[cat]
        body = esc(md_post(title, kind, url, blurb))
        post_vals.append(
            f"({authors[i]}, {catv}, '{esc(title)}',\n '{body}',\n {views[i]}, 0, {likes[i]}, {1 if pin else 0}, {1 if hot else 0})"
        )
    lines.append(",\n".join(post_vals) + ";")
    lines.append("")

    lines += [
        "-- ---------- 4) 项目分享（真实 GitHub / DSH）----------",
        "INSERT INTO project_share",
        "  (user_id, title, summary, content_md, cover_url, repo_url, demo_url, tags, view_count, like_count, status)",
        "VALUES",
    ]
    share_authors = [
        "@alice", "@carol", "@alice", "@alice", "@alice", "@carol", "@carol", "@carol",
        "@alice", "@alice", "@carol", "@bob", "@bob", "@bob", "@bob",
    ]
    lang_map = {r["n"]: r.get("l") for r in dsh}
    share_vals = []
    for i, (full, title, summary, tags, stars) in enumerate(shares):
        lang = lang_map.get(full) or tags[0]
        cover = f"https://opengraph.githubassets.com/1/{full}"
        body = esc(md_share(full, title, summary, lang, stars))
        tag_sql = ", ".join(f"'{t}'" for t in tags)
        v = min(28000, max(100, stars // 8))
        lk = min(2500, max(5, stars // 80))
        share_vals.append(
            f"({share_authors[i]}, '{esc(title)}', '{esc(summary)[:500]}',\n '{body}',\n '{cover}',\n 'https://github.com/{full}', NULL,\n JSON_ARRAY({tag_sql}), {v}, {lk}, 'PUBLISHED')"
        )
    lines.append(",\n".join(share_vals) + ";")
    lines.append("")

    lines += [
        "-- ---------- 5) Skill 专区（社区热门）----------",
        "INSERT INTO skill",
        "  (user_id, title, summary, content_md, model_adaptation_md, tags, view_count, like_count, status)",
        "VALUES",
    ]
    skill_vals = []
    for i, (title, summary, body, model, tags, lk) in enumerate(skills):
        author = "@carol" if i % 2 == 0 else "@alice"
        content = esc(md_skill(title, summary, body, model, lk))
        tag_sql = ", ".join(f"'{t}'" for t in tags)
        skill_vals.append(
            f"({author}, '{esc(title)}', '{esc(summary)[:500]}',\n '{content}',\n '{esc(model)}',\n JSON_ARRAY({tag_sql}), {lk * 12}, {lk}, 'PUBLISHED')"
        )
    lines.append(",\n".join(skill_vals) + ";")
    lines.append("")

    lines += [
        "-- ---------- 6) 评论 ----------",
        "SET @p_r1 := (SELECT id FROM post WHERE title = '[REAL] 读 DeepSeek-R1：用纯 RL 激励推理能力' LIMIT 1);",
        "SET @p_dsh := (SELECT id FROM post WHERE title LIKE '[REAL] DSH 生态热榜快照%' LIMIT 1);",
        "SET @s_harness := (SELECT id FROM project_share WHERE title LIKE 'DeepSeek Harness%' LIMIT 1);",
        "SET @s_skills := (SELECT id FROM project_share WHERE title LIKE 'anthropics/skills%' LIMIT 1);",
        "",
        "INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count) VALUES",
        "('POST', @p_r1, @alice, NULL, '导读帖很清晰，已去 arXiv 看原文摘要。', 12),",
        "('POST', @p_r1, @carol, NULL, '蒸馏小模型那段对本地实验很有用。', 8),",
        "('POST', @p_dsh, @bob, NULL, '仪表盘 stars 和仓库页对得上，种子可信。', 6),",
        "('SHARE', @s_harness, @carol, NULL, '插件市场玩法很像 Everything is a Plugin。', 15),",
        "('SHARE', @s_skills, @alice, NULL, '官方 Skill 规范值得当极客栈 Skill 专区标杆。', 11);",
        "",
        "SET @c_r1 := (SELECT id FROM comment WHERE content_md LIKE '导读帖很清晰%' LIMIT 1);",
        "INSERT INTO comment (target_type, target_id, user_id, parent_id, content_md, like_count) VALUES",
        "('POST', @p_r1, @bob, @c_r1, '楼中楼：Nature 正式刊出后 DOI 也补进帖子了。', 3);",
        "",
        "UPDATE post p",
        "   SET reply_count = (",
        "       SELECT COUNT(*) FROM comment c",
        "        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0),",
        "       last_reply_at = (",
        "       SELECT MAX(created_at) FROM comment c",
        "        WHERE c.target_type = 'POST' AND c.target_id = p.id AND c.deleted = 0)",
        " WHERE p.user_id IN (@alice, @bob, @carol);",
        "",
        "INSERT INTO user_like (user_id, target_type, target_id)",
        "SELECT @alice, 'POST', @p_r1 FROM DUAL WHERE @p_r1 IS NOT NULL",
        "UNION ALL SELECT @carol, 'POST', @p_r1 FROM DUAL WHERE @p_r1 IS NOT NULL",
        "UNION ALL SELECT @bob, 'SHARE', @s_harness FROM DUAL WHERE @s_harness IS NOT NULL",
        "UNION ALL SELECT @alice, 'SHARE', @s_skills FROM DUAL WHERE @s_skills IS NOT NULL;",
        "",
        "SELECT 'seed-real-hot-data OK' AS msg,",
        "  (SELECT COUNT(*) FROM `user` WHERE username LIKE 'real\\\\_%' ESCAPE '\\\\') AS users,",
        "  (SELECT COUNT(*) FROM post p JOIN `user` u ON u.id=p.user_id WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\') AS posts,",
        "  (SELECT COUNT(*) FROM project_share s JOIN `user` u ON u.id=s.user_id WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\') AS shares,",
        "  (SELECT COUNT(*) FROM skill sk JOIN `user` u ON u.id=sk.user_id WHERE u.username LIKE 'real\\\\_%' ESCAPE '\\\\') AS skills;",
    ]

    sql_path = ROOT / "deploy" / "dev" / "seed-real-hot-data.sql"
    sql_path.parent.mkdir(parents=True, exist_ok=True)
    sql_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("SQL", sql_path, sql_path.stat().st_size)

    cat = [
        "# 极客栈真实热门种子目录",
        "",
        "密码统一：`password123` · 账号：`real_alice` / `real_bob` / `real_carol`",
        "",
        "灌库：`bash deploy/dev/seed-real-hot-data.sh`",
        "",
        "## 1. 项目分享（GitHub / DSH）",
        "",
    ]
    for full, title, summary, tags, stars in shares:
        cat.append(f"- **{full}** ★≈{stars:,} — {summary} — https://github.com/{full}")
    cat += [
        "",
        "### DSH 仪表盘 Top（快照 JSON）",
        "见同目录 `dsh-top50.json`（从桌面 `deepseek-harness-dashboard.html` 解析）。",
        "",
        "## 2. 论坛帖（论文 / 博客导读）",
        "",
    ]
    for cat_slug, pin, hot, title, kind, url, blurb in posts:
        flags = []
        if pin:
            flags.append("置顶")
        if hot:
            flags.append("热门")
        flag = (" [" + "/".join(flags) + "]") if flags else ""
        cat.append(f"- {title}{flag} · {kind} · {url}")
    cat += ["", "## 3. Skill 专区", ""]
    for title, summary, body, model, tags, lk in skills:
        src = next((ln.replace("来源：", "") for ln in body.splitlines() if ln.startswith("来源：")), "")
        cat.append(f"- **{title}** — {summary} — {src}")
    cat += [
        "",
        "## 版权与真实性",
        "- 星标数字为公开页面/仪表盘快照，会随时间漂移。",
        "- 帖子仅导读+外链，不粘贴论文/博客全文。",
        "- Skill 正文为安装与测试说明，指向原仓库。",
        "",
    ]
    readme = HERE / "README.md"
    readme.write_text("\n".join(cat), encoding="utf-8")
    print("README", readme)


if __name__ == "__main__":
    main()
