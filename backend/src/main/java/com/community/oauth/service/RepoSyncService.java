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
 * 仓库快照同步服务（T16 + P2-11 + T-REPO-1 + T-OAUTH-GITEE-REFRESH）：
 * - 登录/绑定成功后异步触发，拉取用户公开仓库写入 user_repo 快照
 * - Gitee：expires_at 临近/已过期时用 refresh_token 静默续期并回写
 * - 平台 API / refresh 失败时保留旧快照（不阻塞登录、不写空快照）
 * - 成功同步：物理 DELETE + INSERT
 */
@Service
public class RepoSyncService {

    private static final Logger log = LoggerFactory.getLogger(RepoSyncService.class);

    /** 提前刷新缓冲，避免边界时刻同步踩过期。 */
    static final long REFRESH_SKEW_SECONDS = 60;

    private final UserRepoMapper repoMapper;
    private final UserOauthBindingMapper bindingMapper;
    private final AesGcmCipher cipher;
    private final TransactionTemplate transactionTemplate;
    private final GiteeOAuthService giteeOAuthService;
    private final List<RepoProvider> providerList;

    public RepoSyncService(UserRepoMapper repoMapper, UserOauthBindingMapper bindingMapper,
                            AesGcmCipher cipher, TransactionTemplate transactionTemplate,
                            GiteeOAuthService giteeOAuthService,
                            List<RepoProvider> providerList) {
        this.repoMapper = repoMapper;
        this.bindingMapper = bindingMapper;
        this.cipher = cipher;
        this.transactionTemplate = transactionTemplate;
        this.giteeOAuthService = giteeOAuthService;
        this.providerList = List.copyOf(providerList);
    }

    /**
     * 同步用户指定 provider 的仓库快照（异步，失败保留旧快照）。
     * 触发时机：登录成功 / 绑定成功（由 OAuthService 调用）。
     */
    @Async
    public void syncRepos(long userId, String provider) {
        try {
            syncReposBlocking(userId, provider);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.NOT_FOUND) {
                log.warn("syncRepos: 未找到 {} 绑定记录 userId={}", provider, userId);
            }
            // 远端失败已在 syncReposBlocking 打 WARN，此处不再重复
        } catch (Exception e) {
            log.warn("syncRepos 失败(保留旧快照): userId={}, provider={}, error={}",
                    userId, provider, e.getMessage());
        }
    }

    /**
     * 手动同步（阻塞）。未绑定 → {@link ErrorCode#NOT_FOUND}；
     * 远端/refresh 失败 → {@link ErrorCode#SERVICE_UNAVAILABLE}（旧快照保留）。
     *
     * @return 本次写入条数
     */
    public int syncReposBlocking(long userId, String provider) {
        String normalized = normalizeProvider(provider);
        UserOauthBinding binding = bindingMapper.selectOne(
                Wrappers.<UserOauthBinding>lambdaQuery()
                        .eq(UserOauthBinding::getUserId, userId)
                        .eq(UserOauthBinding::getProvider, normalized)
                        .last("LIMIT 1"));
        if (binding == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "未绑定 " + normalized + " 账号，请先完成 OAuth 授权");
        }

        RepoProvider repoProvider = findProvider(normalized);
        if (repoProvider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的仓库平台: " + normalized);
        }

        try {
            String accessToken = resolveAccessToken(binding, normalized);
            List<RepoProvider.RemoteRepo> remoteRepos = repoProvider.fetchPublicRepos(accessToken);
            transactionTemplate.executeWithoutResult(
                    status -> syncSnapshot(userId, normalized, remoteRepos));
            log.info("syncRepos 完成: userId={}, provider={}, count={}",
                    userId, normalized, remoteRepos.size());
            return remoteRepos.size();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("syncRepos 失败(保留旧快照): userId={}, provider={}, error={}",
                    userId, normalized, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "同步失败，已保留上次仓库快照");
        }
    }

    /** 校验并归一化 provider（GITHUB / GITEE）。 */
    public static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "provider 必填，取值 GITHUB 或 GITEE");
        }
        String p = provider.trim().toUpperCase();
        if (!"GITHUB".equals(p) && !"GITEE".equals(p)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "provider 仅支持 GITHUB 或 GITEE");
        }
        return p;
    }

    private RepoProvider findProvider(String provider) {
        return providerList.stream()
                .filter(p -> provider.equals(p.provider()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解密可用 access_token；Gitee 临近过期则 refresh 并 AES 回写（不打日志明文）。
     */
    String resolveAccessToken(UserOauthBinding binding, String provider) {
        if ("GITEE".equals(provider) && needsGiteeRefresh(binding)) {
            return refreshGiteeAccessToken(binding);
        }
        return cipher.decrypt(binding.getAccessTokenEnc());
    }

    static boolean needsGiteeRefresh(UserOauthBinding binding) {
        LocalDateTime expiresAt = binding.getExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        return !expiresAt.isAfter(LocalDateTime.now().plusSeconds(REFRESH_SKEW_SECONDS));
    }

    private String refreshGiteeAccessToken(UserOauthBinding binding) {
        String refreshEnc = binding.getRefreshToken();
        if (refreshEnc == null || refreshEnc.isBlank()) {
            throw new IllegalStateException("Gitee token expired but refresh_token missing");
        }
        String refreshPlain = cipher.decrypt(refreshEnc);
        try {
            GiteeOAuthService.TokenExchangeResult result = giteeOAuthService.refreshAccessToken(refreshPlain);
            binding.setAccessTokenEnc(cipher.encrypt(result.accessToken()));
            if (result.refreshToken() != null && !result.refreshToken().isBlank()) {
                binding.setRefreshToken(cipher.encrypt(result.refreshToken()));
            }
            binding.setExpiresAt(result.expiresAt());
            bindingMapper.updateById(binding);
            log.info("Gitee token refreshed: userId={}, bindingId={}", binding.getUserId(), binding.getId());
            return result.accessToken();
        } catch (Exception e) {
            log.warn("Gitee token refresh failed: userId={}, bindingId={}, error={}",
                    binding.getUserId(), binding.getId(), e.getMessage());
            throw new IllegalStateException("Gitee token refresh failed: " + e.getMessage(), e);
        }
    }

    /**
     * 全量替换快照（在事务中执行，物理 DELETE 失败则回滚，不会丢旧快照）。
     */
    void syncSnapshot(long userId, String provider, List<RepoProvider.RemoteRepo> remoteRepos) {
        Map<String, Integer> hiddenPrefs = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, provider))
                .stream()
                .filter(r -> r.getHidden() != null && r.getHidden() == 1)
                .collect(Collectors.toMap(UserRepo::getRepoFullName, r -> 1, (a, b) -> a));

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
