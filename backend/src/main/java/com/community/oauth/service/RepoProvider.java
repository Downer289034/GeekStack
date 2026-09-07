package com.community.oauth.service;

import java.util.List;

/**
 * 仓库平台适配器：抽象 GitHub / Gitee 的拉取仓库列表操作。
 * 未来新增 GitLab/Codeberg 等平台时，只需新增实现（决策 10）。
 */
public interface RepoProvider {

    /** 平台标识：GITHUB / GITEE。 */
    String provider();

    /** 按平台 token（解密后）拉取用户的公开仓库列表。失败抛异常，由上层决定是否保留旧快照。 */
    List<RemoteRepo> fetchPublicRepos(String accessToken);

    /** 平台侧仓库信息（归一化）。 */
    record RemoteRepo(
            String fullName,
            String url,
            String description,
            long stars
    ) {}
}
