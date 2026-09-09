# T-BE-2 环境验收 · 疑问清单

> 产出日期：2026-09-09 ｜ 验收人：接任后端  
> 通读：`交接指引-新后端.md`、架构规划、执行计划、检查点 B/C/D、开发进度、AGENTS.md

## 验收证据

| 项 | 结果 |
|----|------|
| `mvn.cmd -B verify` | **Tests run: 90, Failures: 0, Errors: 0, Skipped: 0** / **BUILD SUCCESS**（在已灌入 `demo_*` 演示数据后复跑仍绿；共享库隔离断言已收紧） |
| 前端 lint | `npm run lint` 通过 |
| 前端 typecheck | `npx vue-tsc --noEmit` 通过（`TYPECHECK_OK`） |
| 前端 build | 此前同会话 `vite build` 通过（chunk>500KB 告警为 md-editor，与检查点 B 一致） |
| 基础设施 | `172.25.41.137:3306/6379` 通；community-dev healthy |

## 疑问清单（交接反馈闭环）

1. **共享 community-dev 与集成测试隔离**  
   若干集成测曾断言「全站结果第 0 条 / 恰好 N 条」，与 T-BE-3 演示数据冲突（本轮踩中 `HotPostServiceTest`、`PostApiIntegrationTest.latestAndHotSorting`）。  
   **已做最小修复**：改为断言本用例记录的相对序。请 PM 确认可合入；并确认 `demo_*` 与日常 `mvn verify` 允许共存。

2. **测试用户残留**  
   库中大量 `s17other_*` 等非 `t*` 前缀用户未清（疑似历史集成测清理不全）。不影响当前 90 绿，但是否需要定期 `cleanup` 脚本？与 T-BE-3 `demo_*` 清理策略是否统一？

3. **交接指引质量底账仍写「88 个测试」**  
   现网已是 90（含 T-BE-1 隔离测等）。是否由文档责权方改数字，还是新人 PR 顺手改？

4. **Webhook 仍指向可联调 Agent 时的 422 噪声**  
   本机 verify 日志大量 `Webhook 422`（字段命名 camelCase vs Agent snake_case）。Agent 对接已取消但代码保留——本地开发是否应默认 `AGENT_WEBHOOK_URL=http://127.0.0.1:9`（与 CI 一致）写进 `.env.example` 注释，减少误判？

5. **生产栈 `/opt/community` vs 开发栈路径注释**  
   `deploy/dev/docker-compose.yml` 头注释仍写「部署位置：服务器 /opt/community/…」，实物是 `/opt/community-dev`。是否更正注释以免下一任再踩双栈坑？

6. **空项显式声明**  
   密钥交接、OAuth 真机联调、上线 CF Tunnel 决策等不在本次环境验收范围；无额外阻塞疑问。

—— 以上为 T-BE-2 全部产出。
