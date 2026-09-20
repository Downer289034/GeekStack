package com.community.steward;

import com.community.common.PageResponse;
import com.community.forum.entity.Comment;
import com.community.forum.entity.Post;
import com.community.forum.service.CommentService;
import com.community.forum.service.PostService;
import com.community.share.service.ShareService;
import com.community.skill.service.SkillService;
import com.community.steward.dto.CommunityStatsResponse;
import com.community.steward.dto.PenaltyPageResponse;
import com.community.steward.dto.StewardContentItem;
import com.community.user.entity.User;
import com.community.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 管家只读数据编排：脱敏历史 + 聚合统计 + 判罚占位。
 */
@Service
public class StewardDataService {

    private static final int SNIPPET_MAX = 500;
    private static final String PENALTY_NOTE =
            "判罚回流一期占位：暂无入库与回流，待契约 v2 字段定稿后实装";

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final ShareService shareService;
    private final SkillService skillService;

    public StewardDataService(UserService userService, PostService postService,
                              CommentService commentService, ShareService shareService,
                              SkillService skillService) {
        this.userService = userService;
        this.postService = postService;
        this.commentService = commentService;
        this.shareService = shareService;
        this.skillService = skillService;
    }

    /** 用户发帖历史（脱敏）。 */
    public PageResponse<StewardContentItem> pageUserPosts(String userKey, long current, long size) {
        User user = userService.requireById(StewardIds.parseUserKey(userKey));
        PageResponse<Post> page = postService.pageByUser(user.getId(), current, size);
        List<StewardContentItem> records = page.records().stream()
                .map(p -> toPostItem(p, user.getUsername()))
                .toList();
        return new PageResponse<>(page.total(), page.current(), page.size(), records);
    }

    /** 用户评论历史（脱敏）。 */
    public PageResponse<StewardContentItem> pageUserComments(String userKey, long current, long size) {
        User user = userService.requireById(StewardIds.parseUserKey(userKey));
        PageResponse<Comment> page = commentService.pageByUser(user.getId(), current, size);
        List<StewardContentItem> records = page.records().stream()
                .map(c -> toCommentItem(c, user.getUsername()))
                .toList();
        return new PageResponse<>(page.total(), page.current(), page.size(), records);
    }

    /** 社区聚合统计。 */
    public CommunityStatsResponse stats() {
        return new CommunityStatsResponse(
                userService.countAll(),
                userService.countBanned(),
                postService.countAll(),
                commentService.countAll(),
                shareService.countAll(),
                skillService.countAll(),
                0L,
                PENALTY_NOTE,
                Instant.now());
    }

    /** 判罚回流占位（空列表）。 */
    public PenaltyPageResponse penalties(long current, long size) {
        long cur = current <= 0 ? 1 : current;
        long sz = size <= 0 ? 20 : size;
        return new PenaltyPageResponse(0, cur, sz, List.of(), true, PENALTY_NOTE);
    }

    private StewardContentItem toPostItem(Post post, String username) {
        return new StewardContentItem(
                StewardIds.post(post.getId()),
                StewardIds.user(post.getUserId()),
                username,
                "POST",
                post.getTitle(),
                StewardIds.snippet(post.getContentMd(), SNIPPET_MAX),
                null,
                post.getCreatedAt());
    }

    private StewardContentItem toCommentItem(Comment comment, String username) {
        String targetId = CommentService.TARGET_SHARE.equals(comment.getTargetType())
                ? StewardIds.share(comment.getTargetId())
                : StewardIds.post(comment.getTargetId());
        return new StewardContentItem(
                StewardIds.comment(comment.getId()),
                StewardIds.user(comment.getUserId()),
                username,
                "COMMENT",
                null,
                StewardIds.snippet(comment.getContentMd(), SNIPPET_MAX),
                targetId,
                comment.getCreatedAt());
    }
}
