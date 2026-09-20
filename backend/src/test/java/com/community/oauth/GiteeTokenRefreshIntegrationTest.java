package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.oauth.service.GiteeOAuthService;
import com.community.oauth.service.GiteeRepoProvider;
import com.community.oauth.service.RepoProvider;
import com.community.oauth.service.RepoSyncService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T-OAUTH-GITEE-REFRESH：过期 token 静默 refresh；失败保留旧快照（await @Async）。
 */
@SpringBootTest
class GiteeTokenRefreshIntegrationTest {

    private static final String PREFIX = "s_gtr_";

    @Autowired
    private RepoSyncService repoSyncService;

    @Autowired
    private UserRepoMapper repoMapper;

    @Autowired
    private UserOauthBindingMapper bindingMapper;

    @Autowired
    private AesGcmCipher cipher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private GiteeOAuthService giteeOAuthService;

    @MockBean
    private GiteeRepoProvider giteeRepoProvider;

    private long userId;

    @BeforeEach
    void setUp() {
        when(giteeRepoProvider.provider()).thenReturn("GITEE");
        String username = PREFIX + System.nanoTime();
        jdbcTemplate.update("INSERT INTO user (username, role, status) VALUES (?, 'USER', 0)", username);
        userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void clean() {
        jdbcTemplate.update(
                "DELETE FROM user_repo WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)",
                PREFIX + "%");
        jdbcTemplate.update(
                "DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private void insertExpiredBinding(String accessPlain, String refreshPlain) {
        UserOauthBinding binding = new UserOauthBinding();
        binding.setUserId(userId);
        binding.setProvider("GITEE");
        binding.setOpenid("gtr_" + userId);
        binding.setAccessTokenEnc(cipher.encrypt(accessPlain));
        binding.setRefreshToken(cipher.encrypt(refreshPlain));
        binding.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        bindingMapper.insert(binding);
    }

    private void insertOldSnapshot(String fullName) {
        UserRepo old = new UserRepo();
        old.setUserId(userId);
        old.setProvider("GITEE");
        old.setRepoFullName(fullName);
        old.setRepoUrl("https://gitee.com/" + fullName);
        old.setStars(1L);
        old.setSyncedAt(LocalDateTime.now().minusDays(1));
        repoMapper.insert(old);
    }

    private List<UserRepo> visible() {
        return repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, "GITEE"));
    }

    private void awaitRepos(String expectedFullName) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            List<UserRepo> list = visible();
            if (list.size() == 1 && expectedFullName.equals(list.get(0).getRepoFullName())) {
                return;
            }
            Thread.sleep(250);
        }
        assertThat(visible()).extracting(UserRepo::getRepoFullName).containsExactly(expectedFullName);
    }

    private void awaitOldSnapshotPreserved(String fullName) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            assertThat(visible())
                    .as("async round %d: refresh 失败不得清快照", i)
                    .hasSize(1);
            assertThat(visible().get(0).getRepoFullName()).isEqualTo(fullName);
            Thread.sleep(300);
        }
    }

    @Test
    void expiredTokenRefreshSuccessUpdatesSnapshotAndRewritesToken() throws Exception {
        insertExpiredBinding("old_access", "old_refresh");
        insertOldSnapshot("owner/old-repo");

        when(giteeOAuthService.refreshAccessToken(eq("old_refresh")))
                .thenReturn(new GiteeOAuthService.TokenExchangeResult(
                        "new_access", "new_refresh", LocalDateTime.now().plusHours(2)));
        when(giteeRepoProvider.fetchPublicRepos(eq("new_access")))
                .thenReturn(List.of(new RepoProvider.RemoteRepo(
                        "owner/new-repo", "https://gitee.com/owner/new-repo", "n", 3L)));

        repoSyncService.syncRepos(userId, "GITEE");
        awaitRepos("owner/new-repo");

        verify(giteeOAuthService).refreshAccessToken("old_refresh");
        verify(giteeRepoProvider).fetchPublicRepos("new_access");

        UserOauthBinding updated = bindingMapper.selectOne(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, userId)
                .eq(UserOauthBinding::getProvider, "GITEE"));
        assertThat(cipher.decrypt(updated.getAccessTokenEnc())).isEqualTo("new_access");
        assertThat(cipher.decrypt(updated.getRefreshToken())).isEqualTo("new_refresh");
        assertThat(updated.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(30));
        assertThat(updated.getAccessTokenEnc()).doesNotContain("new_access");
        assertThat(updated.getRefreshToken()).doesNotContain("new_refresh");
    }

    @Test
    void refreshFailurePreservesOldSnapshot() throws Exception {
        insertExpiredBinding("old_access", "bad_refresh");
        insertOldSnapshot("owner/keep-me");

        when(giteeOAuthService.refreshAccessToken(anyString()))
                .thenThrow(new IllegalStateException("invalid_grant"));

        repoSyncService.syncRepos(userId, "GITEE");
        awaitOldSnapshotPreserved("owner/keep-me");

        UserOauthBinding unchanged = bindingMapper.selectOne(Wrappers.<UserOauthBinding>lambdaQuery()
                .eq(UserOauthBinding::getUserId, userId)
                .eq(UserOauthBinding::getProvider, "GITEE"));
        assertThat(cipher.decrypt(unchanged.getAccessTokenEnc())).isEqualTo("old_access");
    }
}
