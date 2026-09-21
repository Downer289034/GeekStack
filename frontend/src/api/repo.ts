import request, { unwrap } from '@/api/request'

/** 用户仓库快照（与后端 UserRepo 对应） */
export interface UserRepo {
  id: number
  userId: number
  provider: string
  repoFullName: string
  repoUrl: string
  description: string | null
  stars: number
  pushedAt: string | null
  syncedAt: string
  hidden: number
}

/** 获取用户仓库列表（includeHidden=true 时返回全量，供导入使用） */
export function fetchRepos(username: string, includeHidden = false): Promise<UserRepo[]> {
  return unwrap<UserRepo[]>(
    request.get(`/v1/users/${encodeURIComponent(username)}/repos`, {
      params: { includeHidden: includeHidden || undefined }
    })
  )
}

/** 切换仓库隐藏状态：返回 true=已隐藏，false=已显示 */
export function toggleRepoHide(repoId: number): Promise<boolean> {
  return unwrap<boolean>(request.post(`/v1/repos/${repoId}/toggle-hide`))
}

/** 手动同步平台（对接 T-REPO-MANUAL-SYNC-API） */
export type RepoSyncProvider = 'GITHUB' | 'GITEE'

export interface ManualRepoSyncResult {
  provider: string
  syncedCount: number
}

/**
 * 本人手动同步指定 provider 仓库快照。
 * `POST /api/v1/me/repos/sync?provider=`（query 优先；未绑定 404 / 限流 429）。
 */
export function syncMyRepos(provider: RepoSyncProvider): Promise<ManualRepoSyncResult> {
  return unwrap<ManualRepoSyncResult>(
    request.post(
      '/v1/me/repos/sync',
      {},
      {
        params: { provider },
        timeout: 60000
      }
    )
  )
}
