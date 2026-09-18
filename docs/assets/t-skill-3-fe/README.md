# T-SKILL-3-FE 验收证据

## 三断点截图（发布页）
- `01-publish-desktop-1920.png`
- `01-publish-tablet-768.png`
- `01-publish-mobile-375.png`

## SKILL.md 粘贴自动提取
- 样例：`sample-SKILL.md`（frontmatter：`java-exception-analyzer`）
- 结果：`paste-extract.json`（`ok: true`）
- 截图：`02-paste-extract.png`（toast「已从 SKILL.md 提取名称与摘要」）

## 契约对齐（T-SKILL-3-BE）
| 字段 | FE | BE |
|------|----|----|
| `version` | 必填，默认 `1.0.0`，SemVer 核心正则 | `SkillPayload.version`；空→`1.0.0`；`SemVer.isValid` |
| `changelog` | 选填 ≤2000 | `@Size(max=2000)` |
| 展示兜底 | `skillVersionLabel()` → `1.0.0` | Flyway V7 存量回填 |

联调实证：`mvn -Dtest=SkillVersionIntegrationTest,SemVerTest test` 通过（V7 已迁移）。

重跑截图（需本机已装 playwright）：`node frontend/scripts/t-skill-3-fe-capture.mjs`（先 `npm run dev`）。
