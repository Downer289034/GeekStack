package com.community.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.AesGcmCipher;
import com.community.oauth.entity.UserOauthBinding;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserOauthBindingMapper;
import com.community.oauth.mapper.UserRepoMapper;
import com.community.oauth.service.RepoSyncService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 异步路径验收（Task 16 + T-REPO-1）：API 失败保留旧快照须真正 await @Async。
 */
@SpringBootTest
class RepoSyncServiceTest {

    private static final String TEST_USER_PREFIX = "s16_";

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

    private long userId;

    @BeforeEach
    void initUser() {
        String username = TEST_USER_PREFIX + System.nanoTime();
        jdbcTemplate.update("INSERT INTO user (username, role, status) VALUES (?, 'USER', 0)", username);
        userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanTestUsers() {
        jdbcTemplate.update("DELETE FROM user_repo WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user_oauth_binding WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)", TEST_USER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", TEST_USER_PREFIX + "%");
    }

    private void createBinding(String provider) {
        UserOauthBinding binding = new UserOauthBinding();
        binding.setUserId(userId);
        binding.setProvider(provider);
        binding.setOpenid("test_openid_" + provider);
        binding.setAccessTokenEnc(cipher.encrypt("mock_token_" + provider));
        binding.setExpiresAt(LocalDateTime.now().plusHours(1));
        bindingMapper.insert(binding);
    }

    private List<UserRepo> visibleRepos(String provider) {
        return repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, provider));
    }

    /**
     * T-REPO-1 / F5：失败路径无正向落库信号时，在窗口内持续断言旧快照仍在
     * （若被误清成空列表会立刻失败）。
     */
    private void awaitOldSnapshotPreserved(String provider, String fullName) throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            List<UserRepo> after = visibleRepos(provider);
            assertThat(after)
                    .as("async round %d: 旧快照不得被清掉", i)
                    .hasSize(1);
            assertThat(after.get(0).getRepoFullName()).isEqualTo(fullName);
            Thread.sleep(300);
        }
    }

    private void awaitNoVisibleAndNoGhost() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            assertThat(visibleRepos("GITHUB")).isEmpty();
            Integer deleted = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user_repo WHERE user_id = ? AND deleted = 1",
                    Integer.class, userId);
            assertThat(deleted).isZero();
            Thread.sleep(200);
        }
    }

    @Test
    void syncReposDoesNotThrowOnApiFailure() throws InterruptedException {
        createBinding("GITHUB");
        repoSyncService.syncRepos(userId, "GITHUB");
        awaitNoVisibleAndNoGhost();
    }

    @Test
    void apiFailurePreservesOldSnapshot() throws InterruptedException {
        createBinding("GITHUB");
        UserRepo old = new UserRepo();
        old.setUserId(userId);
        old.setProvider("GITHUB");
        old.setRepoFullName("fake/old-repo");
        old.setRepoUrl("https://github.com/fake/old-repo");
        old.setStars(0L);
        old.setSyncedAt(LocalDateTime.now());
        repoMapper.insert(old);
        assertThat(visibleRepos("GITHUB")).hasSize(1);

        repoSyncService.syncRepos(userId, "GITHUB");
        awaitOldSnapshotPreserved("GITHUB", "fake/old-repo");
    }

    @Test
    void providerIsolationGitHubNotMixedWithGitee() throws InterruptedException {
        createBinding("GITHUB");
        repoSyncService.syncRepos(userId, "GITHUB");
        awaitNoVisibleAndNoGhost();

        assertThat(visibleRepos("GITEE")).isEmpty();
        assertThat(visibleRepos("GITHUB")).isEmpty();
    }

    @Test
    void missingBindingReturnsEarly() {
        repoSyncService.syncRepos(userId, "GITHUB");
        assertThat(repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId))).isEmpty();
    }

    @Test
    void hiddenPreferencePreservedAfterResync() throws InterruptedException {
        createBinding("GITHUB");

        UserRepo repo = new UserRepo();
        repo.setUserId(userId);
        repo.setProvider("GITHUB");
        repo.setRepoFullName("user/my-hidden-repo");
        repo.setRepoUrl("https://github.com/user/my-hidden-repo");
        repo.setStars(50L);
        repo.setHidden(1);
        repo.setSyncedAt(LocalDateTime.now());
        repoMapper.insert(repo);

        repoSyncService.syncRepos(userId, "GITHUB");
        awaitOldSnapshotPreserved("GITHUB", "user/my-hidden-repo");
        assertThat(visibleRepos("GITHUB").get(0).getHidden()).isEqualTo(1);
    }
}
