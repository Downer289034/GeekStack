# T-VERIFY-1 走查记录（2026-09-18）

脚本：`scripts/t-verify-1-walkthrough.mjs` + `scripts/degrade-check.mjs`（Playwright + CDP）  
证据目录：`docs/assets/t-verify-1/`

## ① 粒子层

| # | 项 | 结果 | 证据 |
|---|---|---|---|
| 1 | 暗/亮主题 | 暗 `--particles-rgb: 255,186,100`；亮 `150,118,72`；肉眼几乎不可察觉，无色块 | `01-home-dark.png` / `02-home-light.png` + `walkthrough.json` |
| 2 | `/login` 无粒子 | `canvas.gs-particles` count = **0** | `03-login-no-particles.png` |
| 3 | 页签隐藏停 rAF | `visibilitychange` + `document.hidden` → `stop()`；代码路径确认 | `walkthrough.json`；任务管理器 CPU 归零需人工实机补一眼（headless 无法代签） |
| 4 | CPU 限速降级 | CDP `setCPUThrottlingRate` 16× ≈25s：先 `[GsParticles] 掉帧降级：密度减半至 25`，再 `[GsParticles] 停用：掉帧降级触发两次，粒子层停用` | `degrade.json` |

## ② T-SEC-1-FE

| # | 项 | 结果 | 证据 |
|---|---|---|---|
| 1 | 篡改 pending.state | 文案「登录校验失败（state 不匹配）…」；localStorage/sessionStorage 已清空 | `04-oauth-state-mismatch.png` |
| 2 | 正常 Gitee 登录 / 设置页绑定 | **需有 OAuth App 凭据的人工联调**（本机无真实授权码，自动化不覆盖） | 见 `docs/T-SEC-1-FE-OAuth-state-手动演练.md` 正常路径 |

## ③ 资产治理

| # | 项 | 结果 |
|---|---|---|
| 1 | 2.6MB 脏 mp4 | **还原**至 HEAD（`git checkout`）；工作区禁止直接提交变大版本。CRF28 试压结果与已入库 blob 一致（1,719,645 B） |
| 2 | 双份冗余 | **删除** `docs/assets/login-bg.mp4`；设计稿改引 `../frontend/src/assets/login-bg.mp4` |
| 3 | 尺寸校验 | `git cat-file -s HEAD:frontend/src/assets/login-bg.mp4` = **1719645**（≤1.7MB） |
| 4 | 设计基线入库 | `docs/design-report-2026-09-18.md`、`docs/geekstack-particles-bg.html` |
