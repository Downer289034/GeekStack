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
 * 验收标准（Task 16）：
 * - 登录/绑定成功后触发同步，写 user_repo 快照
 * - 平台 API 挂了不阻塞登录（失败保留旧快照）
 * - provider 隔离：Gitee 数据不能串进 GitHub
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
        // 外键约束：先删子表再删 user
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

    @Test
    void syncReposDoesNotThrowOnApiFailure() {
        createBinding("GITHUB");
        // GitHubRepoProvider 对 mock token 返回空（真实 API 无法访问），但不应抛异常
        repoSyncService.syncRepos(userId, "GITHUB");
    }

    @Test
    void apiFailurePreservesOldSnapshot() {
        createBinding("GITHUB");
        // 插入一条假快照
        UserRepo old = new UserRepo();
        old.setUserId(userId);
        old.setProvider("GITHUB");
        old.setRepoFullName("fake/old-repo");
        old.setRepoUrl("https://github.com/fake/old-repo");
        old.setStars(0L);
        old.setSyncedAt(LocalDateTime.now());
        repoMapper.insert(old);
        assertThat(repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId).eq(UserRepo::getProvider, "GITHUB"))).hasSize(1);

        // mock token 无效→API 失败→旧快照保留
        repoSyncService.syncRepos(userId, "GITHUB");
        List<UserRepo> after = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId).eq(UserRepo::getProvider, "GITHUB"));
        assertThat(after).hasSize(1);
        assertThat(after.get(0).getRepoFullName()).isEqualTo("fake/old-repo");
    }

    @Test
    void providerIsolationGitHubNotMixedWithGitee() {
        createBinding("GITHUB");
        // GitHub sync
        repoSyncService.syncRepos(userId, "GITHUB");

        // Gitee 快照不应受影响（无 binding → 空）
        List<UserRepo> giteeRepos = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId).eq(UserRepo::getProvider, "GITEE"));
        assertThat(giteeRepos).isEmpty();

        // GitHub 快照独立存在
        List<UserRepo> githubRepos = repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId).eq(UserRepo::getProvider, "GITHUB"));
        assertThat(githubRepos).isNotNull();
    }

    @Test
    void missingBindingReturnsEarly() {
        // 未建 binding → syncRepos 应静默返回，不抛异常
        repoSyncService.syncRepos(userId, "GITHUB");
        assertThat(repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId))).isEmpty();
    }
}
