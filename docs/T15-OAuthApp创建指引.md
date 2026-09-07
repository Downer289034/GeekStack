# T15 OAuth App 创建指引（需 PM 亲手办理）

> 原因：GitHub / Gitee 均无"创建 OAuth App"的 API，必须登录 Web UI 操作；开发者的应用内浏览器无 PM 登录态，不可代登录。
> 创建后**只需回报"已就位"**，Secret 不发任何人——直接写入本机根目录 `.env`（该文件已被 gitignore）。

## 一、GitHub OAuth App

入口：GitHub → Settings → Developer settings → OAuth Apps → **New OAuth App**

| 表单项 | 填写值 |
|--------|--------|
| Application name | `GeekStack dev` |
| Homepage URL | `http://localhost:5173` |
| Authorization callback URL | `http://localhost:8080/api/v1/oauth/callback/github` |
| Enable Device Flow | 不勾 |

创建后：**Generate a new client secret**（只显示一次）。

## 二、Gitee OAuth 应用

入口：Gitee → 设置 → 安全设置 → 第三方应用 → **创建应用**

| 表单项 | 填写值 |
|--------|--------|
| 应用名称 | `GeekStack dev` |
| 应用主页 | `http://localhost:5173` |
| 应用回调地址 | `http://localhost:8080/api/v1/oauth/callback/gitee` |
| 权限 | `user_info`、`projects`（最小化） |

创建后：应用详情页可查看 Client ID 与 Client Secret（可重置）。

## 三、凭据落位（PM 操作）

编辑仓库根目录 `.env`（已有文件，追加 4 行）：

```
GITHUB_CLIENT_ID=<GitHub 的 Client ID>
GITHUB_CLIENT_SECRET=<GitHub 的 Client Secret>
GITEE_CLIENT_ID=<Gitee 的 Client ID>
GITEE_CLIENT_SECRET=<Gitee 的 Client Secret>
```

保存后回报"OAuth App 已就位"即可，开发者随后联调。`.env` 已被 gitignore，绝不入库。

## 四、生产回调（后续，不阻塞 T15）

geekstack.io 域名+备案就绪后，两家 App 各加一个生产回调地址
`https://<域名>/api/v1/oauth/callback/<provider>`（保留 localhost 回调用于本地开发）。
