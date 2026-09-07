# 极客栈 GeekStack

> 代码江湖的落脚点 —— 极客的客栈，技术的驿站。任何技术方向的人都能落脚交流；
> Java 与 AI 是初始内容聚焦点，但名字不设边界。

极客栈是一个 PC 端技术社区：注册登录、项目分享（Markdown）、技术论坛（分类 / 两级回帖）、
GitHub / Gitee 仓库绑定（规划中）。前后端分离的模块化单体架构。

## 技术栈

- 后端：JDK 17 · Spring Boot 3.3 · MyBatis-Plus · MySQL 8 · Redis 7 · JWT
- 前端：Vue 3 · Vite · TypeScript · Pinia · Naive UI（鸿蒙设计风主题）· markdown-it + DOMPurify
- 基础设施：Docker Compose · Nginx · GitHub Actions CI

## 仓库结构（monorepo）

```
backend/    Spring Boot 服务（com.community，内部代号 community）
frontend/   Vue 3 SPA
docs/       架构规划、开发计划、任务清单、进度交接
deploy/     开发基础设施 Compose 文件
```

## 本地开发

```bash
# 1. 基础设施：服务器侧 docker compose 拉起 MySQL 8 + Redis 7（见 deploy/dev/docker-compose.yml）
# 2. 后端
cp .env.example .env   # 填入数据库 / Redis / JWT 凭据
cd backend && mvn spring-boot:run
# 3. 前端
cd frontend && npm install && npm run dev
```

> 说明：`community` 仅为项目内部代号（包名 / 库名 / 存储键），对外品牌为**极客栈 GeekStack**。

## 文档

- 产品与架构：docs/JavaAI社区-架构与产品规划.md（历史文档，品牌以本文档为准）
- 进度与交接：docs/开发进度.md
