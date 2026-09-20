package com.community.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.oauth.entity.UserRepo;
import com.community.oauth.mapper.UserRepoMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T-REPO-1：syncSnapshot 物理替换回归（同包访问 package-private 方法）。
 */
@SpringBootTest
class RepoSyncSnapshotTest {

    private static final String PREFIX = "s16snap_";

    @Autowired
    private RepoSyncService repoSyncService;

    @Autowired
    private UserRepoMapper repoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long userId;

    @BeforeEach
    void initUser() {
        String username = PREFIX + System.nanoTime();
        jdbcTemplate.update("INSERT INTO user (username, role, status) VALUES (?, 'USER', 0)", username);
        userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update(
                "DELETE FROM user_repo WHERE user_id IN (SELECT id FROM (SELECT id FROM user WHERE username LIKE ?) t)",
                PREFIX + "%");
        jdbcTemplate.update("DELETE FROM user WHERE username LIKE ?", PREFIX + "%");
    }

    private List<UserRepo> visible() {
        return repoMapper.selectList(Wrappers.<UserRepo>lambdaQuery()
                .eq(UserRepo::getUserId, userId)
                .eq(UserRepo::getProvider, "GITHUB"));
    }

    @Test
    void secondSuccessfulSyncKeepsReposVisible() {
        var remotes = List.of(
                new RepoProvider.RemoteRepo("owner/repo-a", "https://github.com/owner/repo-a", "a", 3L),
                new RepoProvider.RemoteRepo("owner/repo-b", "https://github.com/owner/repo-b", "b", 1L));

        repoSyncService.syncSnapshot(userId, "GITHUB", remotes);
        assertThat(visible()).hasSize(2);

        repoSyncService.syncSnapshot(userId, "GITHUB", remotes);
        assertThat(visible()).hasSize(2);
        assertThat(visible()).extracting(UserRepo::getRepoFullName)
                .containsExactlyInAnyOrder("owner/repo-a", "owner/repo-b");

        Integer ghost = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_repo WHERE user_id = ? AND deleted = 1",
                Integer.class, userId);
        assertThat(ghost).isZero();
    }

    @Test
    void syncRecoversFromLogicDeletedGhostRows() {
        jdbcTemplate.update(
                "INSERT INTO user_repo (user_id, provider, repo_full_name, repo_url, stars, hidden, deleted, synced_at) "
                        + "VALUES (?, 'GITHUB', 'owner/ghost', 'https://github.com/owner/ghost', 0, 0, 1, NOW())",
                userId);
        assertThat(visible()).isEmpty();

        var remotes = List.of(
                new RepoProvider.RemoteRepo("owner/ghost", "https://github.com/owner/ghost", "revived", 9L));
        repoSyncService.syncSnapshot(userId, "GITHUB", remotes);

        assertThat(visible()).hasSize(1);
        assertThat(visible().get(0).getRepoFullName()).isEqualTo("owner/ghost");
        assertThat(visible().get(0).getStars()).isEqualTo(9L);
    }

    @Test
    void successfulResyncPreservesHiddenPreference() {
        var remotes = List.of(
                new RepoProvider.RemoteRepo("user/keep-hidden", "https://github.com/user/keep-hidden", "x", 2L));
        repoSyncService.syncSnapshot(userId, "GITHUB", remotes);
        repoSyncService.toggleHide(visible().get(0).getId(), userId);

        repoSyncService.syncSnapshot(userId, "GITHUB", remotes);
        assertThat(visible()).hasSize(1);
        assertThat(visible().get(0).getHidden()).isEqualTo(1);
    }
}
