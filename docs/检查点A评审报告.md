# 检查点 A 骨架评审报告

> 评审人：PM ｜ 日期：2026-09-06 20:35 ｜ 评审对象：T0–T6（commit 08d4838…a0e4342）
> 评审方式：静态代码核对 + 本机复跑构建实测

---

## 一、评审结论：✅ 通过（PASS），准予进入阶段 B

附带 2 项 P2 记录（不阻断），1 项决策确认（批准）。

## 二、实测验证结果（非仅听自测汇报）

| 验证项 | 结果 |
|--------|------|
| 后端 `mvn -B verify`（Checkstyle + 全量测试，连真实 MySQL/Redis） | ✅ BUILD SUCCESS，Tests run: 8, Failures: 0 |
| Flyway 对 172.25.41.137 真实库校验 | ✅ Schema up to date，jar 正常打包 |
| 前端 `npm run lint` | ✅ 0 违规 |
| 前端 `npm run build` | ✅ 6.27s 构建成功，主包 gzip 116KB（正常） |
| `.env` 是否被 git 忽略（git check-ignore 实测） | ✅ 已忽略；`.env.example` 仅有占位符入库 |
| monorepo 结构 / 包结构 | ✅ 与执行计划第三节完全一致（common/config/user/oauth/share/forum/notify/event） |

## 三、逐项核对（对照执行计划检查点 A 范围）

1. **包结构与模块边界** ✅：8 个业务包齐全，预留包均有 package-info 占位，未发现跨模块越界代码。
2. **统一响应体规范** ✅：`Result<T>`（code=0 成功）+ `ErrorCode`（区段=HTTP）+ `BusinessException` + `GlobalExceptionHandler` 完整；BindException/类型不匹配/404/未捕获异常全覆盖，500 不泄漏内部细节（log.error 落盘、响应脱敏）——质量好于验收标准。
3. **建表与规划第五节一致性** ✅：8 张表逐一比对通过——字段、ENUM（GITHUB/GITEE、PUBLISHED/HIDDEN、POST/SHARE）、多态评论（target_type+target_id）、两级评论（parent_id 自引用外键）、关键索引（idx_share_status_created、idx_post_category_created、idx_comment_target、uk_binding_provider_openid）、4 条分类种子，全部吻合。所有表含 created_at/updated_at/deleted ✅。
4. **主题 token 独立性** ✅：`theme/tokens.ts` 集中定义鸿蒙设计风 token（品牌色 #0a59f7、12px 大圆角、HarmonyOS Sans SC 字体栈），注释明确"换肤不碰组件"，符合要求。
5. **请求封装** ✅：JWT Bearer 拦截、401 清 token 跳登录、统一错误提示、`unwrap` 取数便捷方法，TOKEN_KEY 单一出处。
6. **CI** ✅：workflow 用 services 拉起真实 MySQL/Redis 跑测试（非空跑），前端 npm ci + lint + build，触发条件 main push/PR，配置正确。
7. **凭据纪律抽查** ✅：application.yml 全部环境变量引用（密码默认空）、CI 密码为公开测试值属合理、.env 实测被忽略。

## 四、决策确认（进度文档遗留项）

| # | 事项 | PM 决策 |
|---|------|---------|
| 1 | `user.email` 允许 NULL（第三方登录用户可能无邮箱） | **批准**。理由充分且符合规划 M2 口径，唯一索引允许多 NULL 的行为正确。此决策生效，回写规划认知 |
| 2 | post_category.name 未加唯一约束 | **维持现状**。slug 唯一已够用，name 重复属运营层面问题，M4 管理端再做。不加 |
| 3 | Maven 在 Git Bash 下 Launcher 报错 | **记为开发环境注意事项**（非项目缺陷）：后端构建须用 `mvn.cmd` 全路径或 PowerShell/cmd 环境。CI（Linux）不受影响 |

## 五、P2 记录（不阻断，排入后续任务间隙处理）

- **P2-1**：`maven-compiler-plugin:3.13.0` 构建时有 plugin validation 警告，可在 pom 加 `<maven.plugin.validation>DEFAULT</maven.plugin.validation>` 或忽略，建议在 T9 顺手处理。
- **P2-2**：前端主包 350KB（gzip 116KB），MVP 可接受；T10 时检查路由懒加载是否全部生效（当前抽查已是动态 import，预计无问题）。

## 六、下一步指令（阶段 B 启动）

1. **先建 GitHub 远程仓库并推送**（`gh auth login` 或建好给地址），让 T6 的 CI 真实跑起来——首个绿灯截图/链接回报后开始 T7。
2. 按 T7 注册登录 API → T8 登录注册页 → T9 → T10 → T11/T12 → T13 → T14 顺序推进，每任务一分支一 commit，完成后回报。
3. T14 完成即触发**检查点 B：MVP 全量评审 + 上线 Go/No-Go**。提醒：ICP 备案现在就可以开始办理，不要等开发完。
