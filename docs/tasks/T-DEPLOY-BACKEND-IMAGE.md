# T-DEPLOY-BACKEND-IMAGE · backend 镜像追上 main 能力

> 负责人：**开发 A（极客栈后端）**  
> 优先级：P1 · 联调/演示防漂移  
> 派发：2026-09-21（T-SEED-1 关单后）

## 背景

AD2 曾因旧 `geekstack/backend:latest` 缺 `AdminController` 靠热更 jar 过关。此后 main 又落地：加精/置顶（T-ADMIN-FEATURE）、Gitee refresh、标签云含帖（T-BE-4.2）等。工作区仍有未提交的 deploy 纪律补记（`deploy/prod/deploy.sh`、`docker-compose.yml`、交接指引），需与镜像重建一并收口。

## 范围

1. **提交**此前 T-M4-1 纪律补记（仅 deploy/docs 相关脏文件；**勿**混 FE 脚本、其它未关单改动）。  
2. **重建并滚动** `geekstack/backend:latest`（或你们现行 tag），构建点对齐当前含上述能力的 commit；`DEPLOY_COMMIT` 写清。  
3. **冒烟（脱敏）**：  
   - `/actuator/health` → UP  
   - 未登录 `DELETE /api/v1/admin/posts/1` → **401**（路由在镜像内，非 40400）  
   - 未登录 `PUT .../featured` 或 `/pinned` → **401**（加精路由在）  
4. 一句写入：禁止 `docker cp` 热更 jar（若纪律文案已有则确认仍在）。

## 非范围

- 不改业务功能代码  
- 不强制重建 frontend / ai-service（除非你发现 compose 连带必须）  
- 不把 prod MySQL 当实验场；seed 仍走 community-dev

## 验收

1. 纪律补记已 commit + push  
2. 新镜像冒烟三句成立；汇报含 image id / `DEPLOY_COMMIT`  
3. 三句话汇报

## 完成说明（开发 A，2026-09-21）

- 纪律补记入库；`geekstack/backend:latest` 按本 commit 重建滚动
- 冒烟：health UP；admin delete/featured 未登录 → 401
