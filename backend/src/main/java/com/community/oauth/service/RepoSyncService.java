package com.community.oauth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.mapper.UserRepoMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 仓库快照同步服务（T16 + P2-11 + T-REPO-1）：
 * - 登录/绑定成功后异步触发，拉取用户公开仓库写入 user_repo 快照
 * - 平台 API 失败时保留旧快照（不阻塞登录、不写库）
 * - provider 隔离：每个 (userId, provider) 独立同步
 * - 成功同步：物理 DELETE + INSERT（禁止逻辑删占唯一键）
 *
 * 验收标准：断网/限流时主页仍能展示上次快照；不同平台数据不串；二次 sync 列表仍可见。
 */
@Service
public class RepoSyncService {

    private static final Logger log = LoggerFactory.getLogger(RepoSyncService.class);

    private final UserRepoMapper repoMapper;
    private final UserOauthBindingMapper bindingMapper;
    private final AesGcmCipher cipher;
    private final TransactionTemplate transactionTemplate;

    /** 按 provider 索引的 RepoProvider 实现。 */
    private final Map<String, RepoProvider> providers;

    public RepoSyncService(UserRepoMapper repoMapper, UserOauthBindingMapper bindingMapper,
                            AesGcmCipher cipher, TransactionTemplate transactionTemplate,
                            List<RepoProvider> providerList) {
        this.repoMapper = repoMapper;
        this.bindingMapper = bindingMapper;
        this.cipher = cipher;
        this.transactionTemplate = transactionTemplate;
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
            String accessToken = cipher.decrypt(binding.getAccessTokenEnc());

            RepoProvider repoProvider = providers.get(provider);
            if (repoProvider == null) {
                log.warn("syncRepos: 未找到 {} RepoProvider", provider);
                return;
            }
            // Provider 失败必须抛异常；禁止吞成空列表后当成「成功 0 仓」清快照（T-REPO-1）
            List<RepoProvider.RemoteRepo> remoteRepos = repoProvider.fetchPublicRepos(accessToken);

            transactionTemplate.executeWithoutResult(status -> syncSnapshot(userId, provider, remoteRepos));
            log.info("syncRepos 完成: userId={}, provider={}, count={}", userId, provider, remoteRepos.size());
        } catch (Exception e) {
            log.warn("syncRepos 失败(保留旧快照): userId={}, provider={}, error={}",
                    userId, provider, e.getMessage());
        }
    }

    /**
     * 全量替换快照（在事务中执行，物理 DELETE 失败则回滚，不会丢旧快照）。
     * - 物理 DELETE：仅删该 (user_id, provider) 下所有旧快照，不跨 provider
     * - INSERT：按 fullName 回填用户已有的隐藏偏好；新仓库默认 hidden=0
     */
    void syncSnapshot(long userId, String provider, List<RepoProvider.RemoteRepo> remoteRepos) {
        Map<String, Integer> hiddenPrefs = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, provider))
                .stream()
                .filter(r -> r.getHidden() != null && r.getHidden() == 1)
                .collect(Collectors.toMap(UserRepo::getRepoFullName, r -> 1, (a, b) -> a));

        // T-REPO-1：必须物理删，避免 deleted=1 行占唯一键导致 insert 撞键跳过 → 永久空列表
        repoMapper.physicalDeleteByUserAndProvider(userId, provider);

        LocalDateTime now = LocalDateTime.now();

        for (RepoProvider.RemoteRepo repo : remoteRepos) {
            UserRepo record = new UserRepo();
            record.setUserId(userId);
            record.setProvider(provider);
            record.setRepoFullName(repo.fullName());
            record.setRepoUrl(repo.url());
            record.setDescription(repo.description());
            record.setStars(repo.stars());
            record.setHidden(hiddenPrefs.getOrDefault(repo.fullName(), 0));
            record.setSyncedAt(now);
            repoMapper.insert(record);
        }
    }

    /** 切换仓库隐藏状态：0 ↔ 1。仅限仓库所属用户操作。 */
    public boolean toggleHide(long repoId, long userId) {
        UserRepo repo = repoMapper.selectById(repoId);
        if (repo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "仓库不存在");
        }
        if (!repo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限仓库所属用户操作");
        }
        int newHidden = repo.getHidden() == null || repo.getHidden() == 0 ? 1 : 0;
        repoMapper.update(null, Wrappers.<UserRepo>lambdaUpdate()
                .eq(UserRepo::getId, repoId)
                .set(UserRepo::getHidden, newHidden));
        return newHidden == 1;
    }
}
