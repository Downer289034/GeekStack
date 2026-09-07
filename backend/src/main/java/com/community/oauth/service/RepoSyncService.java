package com.community.oauth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.mapper.UserRepoMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 仓库快照同步服务（T16）：
 * - 登录/绑定成功后异步触发，拉取用户公开仓库写入 user_repo 快照
 * - 平台 API 失败时保留旧快照（不阻塞登录）
 * - provider 隔离：每个 (userId, provider) 独立同步
 * - upsert 基于唯一键 (user_id, provider, repo_full_name)
 *
 * 验收标准：断网/限流时主页仍能展示上次快照；不同平台数据不串。
 */
@Service
public class RepoSyncService {

    private static final Logger log = LoggerFactory.getLogger(RepoSyncService.class);

    private final UserRepoMapper repoMapper;
    private final UserOauthBindingMapper bindingMapper;
    private final AesGcmCipher cipher;

    /** 按 provider 索引的 RepoProvider 实现。 */
    private final Map<String, RepoProvider> providers;

    public RepoSyncService(UserRepoMapper repoMapper, UserOauthBindingMapper bindingMapper,
                            AesGcmCipher cipher, List<RepoProvider> providerList) {
        this.repoMapper = repoMapper;
        this.bindingMapper = bindingMapper;
        this.cipher = cipher;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(RepoProvider::provider, p -> p));
    }

    /**
     * 同步用户指定 provider 的仓库快照（异步，失败保留旧快照）。
     * 触发时机：登录成功 / 绑定成功（由 OAuthService 调用）。
     */
    @Async
    public void syncRepos(long userId, String provider) {
        try {
            UserOauthBinding binding = bindingMapper.selectOne(
                    Wrappers.<UserOauthBinding>lambdaQuery()
                            .eq(UserOauthBinding::getUserId, userId)
                            .eq(UserOauthBinding::getProvider, provider)
                            .last("LIMIT 1"));
            if (binding == null) {
                log.warn("syncRepos: 未找到 {} 绑定记录 userId={}", provider, userId);
                return;
            }
            // 解密 token
            String accessToken = cipher.decrypt(binding.getAccessTokenEnc());

            // 拉取平台仓库
            RepoProvider repoProvider = providers.get(provider);
            if (repoProvider == null) {
                log.warn("syncRepos: 未找到 {} RepoProvider", provider);
                return;
            }
            List<RepoProvider.RemoteRepo> remoteRepos = repoProvider.fetchPublicRepos(accessToken);

            // 无条件全量替换该 provider 的快照
            syncSnapshot(userId, provider, remoteRepos);
            log.info("syncRepos 完成: userId={}, provider={}, count={}", userId, provider, remoteRepos.size());
        } catch (Exception e) {
            // 平台 API 挂了不阻塞登录——保留旧快照，仅记 WARN
            log.warn("syncRepos 失败(保留旧快照): userId={}, provider={}, error={}",
                    userId, provider, e.getMessage());
        }
    }

    /**
     * 全量替换快照：删除该 provider 的所有旧记录 → 插入新记录。
     * 唯一键 (user_id, provider, repo_full_name) 兜底防止重复。
     * provider 隔离：DELETE WHERE user_id AND provider 保证不跨平台删数据。
     */
    @Transactional
    void syncSnapshot(long userId, String provider, List<RepoProvider.RemoteRepo> remoteRepos) {
        // 删除该 provider 下所有旧快照
        repoMapper.delete(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, provider));

        LocalDateTime now = LocalDateTime.now();

        // 插入新快照
        for (RepoProvider.RemoteRepo repo : remoteRepos) {
            UserRepo record = new UserRepo();
            record.setUserId(userId);
            record.setProvider(provider);
            record.setRepoFullName(repo.fullName());
            record.setRepoUrl(repo.url());
            record.setDescription(repo.description());
            record.setStars(repo.stars());
            record.setSyncedAt(now);
            try {
                repoMapper.insert(record);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 唯一键兜底：跳过重复记录（正常路径不应出现）
                log.debug("syncSnapshot: 跳过重复记录 userId={}, provider={}, fullName={}",
                        userId, provider, repo.fullName());
            }
        }
    }
}
