package com.community.like.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.event.ContentLikedEvent;
import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import com.community.like.entity.UserLike;
import com.community.like.mapper.UserLikeMapper;
import com.community.share.entity.ProjectShare;
import com.community.share.mapper.ProjectShareMapper;
import com.community.skill.entity.Skill;
import com.community.skill.mapper.SkillMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞服务（T19）：
 * - toggle 语义：未赞→点赞、已赞→取消；重复调用幂等，返回当前状态
 * - 原子计数：setSql("like_count = like_count + 1 / GREATEST(like_count - 1, 0)")
 * - 事件预埋：点赞成功发布 ContentLikedEvent（T20 notify 消费）
 * - 批量查询：首页/详情批量返回"我是否已赞"
 */
@Service
public class LikeService {

    public static final String TYPE_POST = "POST";
    public static final String TYPE_SHARE = "SHARE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_SKILL = "SKILL";

    private final UserLikeMapper likeMapper;
    private final PostMapper postMapper;
    private final ProjectShareMapper shareMapper;
    private final SkillMapper skillMapper;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public LikeService(UserLikeMapper likeMapper, PostMapper postMapper,
                       ProjectShareMapper shareMapper, SkillMapper skillMapper,
                       UserService userService, ApplicationEventPublisher eventPublisher) {
        this.likeMapper = likeMapper;
        this.postMapper = postMapper;
        this.shareMapper = shareMapper;
        this.skillMapper = skillMapper;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * toggle 点赞：未赞→点赞（+1），已赞→取消（-1，GREATEST 兜底 0）。
     * 返回 true=已赞，false=已取消。
     * 认证必需（由 Controller 传入 username）。
     */
    @Transactional
    public boolean toggle(String username, String targetType, long targetId) {
        long userId = requireUserId(username);
        requireTargetExists(targetType, targetId);

        UserLike existing = likeMapper.selectOne(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId));

        if (existing != null) {
            // 已赞→取消
            likeMapper.deleteById(existing.getId());
            atomicUpdateCount(targetType, targetId, -1);
            return false;
        } else {
            // 未赞→点赞
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            try {
                likeMapper.insert(like);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // P2-13：并发双击竞态——第二个事务撞唯一键，返回"已赞"幂等
                return true;
            }
            atomicUpdateCount(targetType, targetId, 1);
            publishLikeEvent(targetType, targetId, userId);
            return true;
        }
    }

    /** 批量查询用户对帖子列表的点赞状态：返回 targetId 集合（已点赞的）。 */
    public Set<Long> getLikedPostIds(long userId, List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Set.of();
        }
        return likeMapper.selectList(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, TYPE_POST)
                .in(UserLike::getTargetId, postIds))
                .stream()
                .map(UserLike::getTargetId)
                .collect(Collectors.toSet());
    }

    /** 单条帖子是否已赞（详情页用）。 */
    public boolean isLiked(long userId, String targetType, long targetId) {
        return likeMapper.selectCount(Wrappers.<UserLike>lambdaQuery()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, targetType)
                .eq(UserLike::getTargetId, targetId)) > 0;
    }

    /** 原子计数：setSql("like_count = like_count + delta")，GREATEST 兜底 0。 */
    private void atomicUpdateCount(String targetType, long targetId, int delta) {
        String sql = "like_count = GREATEST(like_count + " + delta + ", 0)";
        switch (targetType) {
            case TYPE_POST -> postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getId, targetId).setSql(sql));
            case TYPE_SHARE -> shareMapper.update(null, Wrappers.<ProjectShare>lambdaUpdate()
                    .eq(ProjectShare::getId, targetId).setSql(sql));
            case TYPE_SKILL -> skillMapper.update(null, Wrappers.<Skill>lambdaUpdate()
                    .eq(Skill::getId, targetId).setSql(sql));
            // COMMENT: like_count 在 comment 表上，后续接入时加
            default -> {
            }
        }
    }

    private void publishLikeEvent(String targetType, long targetId, long likerUserId) {
        long authorId = resolveAuthorUserId(targetType, targetId);
        eventPublisher.publishEvent(
                new ContentLikedEvent(this, targetType, targetId, likerUserId, authorId));
    }

    /** 从宿主表查作者 ID（供通知模块判断是否自嗨）。 */
    private long resolveAuthorUserId(String targetType, long targetId) {
        if (TYPE_POST.equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            return post != null ? post.getUserId() : -1;
        }
        if (TYPE_SHARE.equals(targetType)) {
            ProjectShare share = shareMapper.selectById(targetId);
            return share != null ? share.getUserId() : -1;
        }
        if (TYPE_SKILL.equals(targetType)) {
            Skill skill = skillMapper.selectById(targetId);
            return skill != null ? skill.getUserId() : -1;
        }
        return -1;
    }

    private void requireTargetExists(String targetType, long targetId) {
        switch (targetType) {
            case TYPE_POST -> {
                if (postMapper.selectById(targetId) == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
                }
            }
            case TYPE_SHARE -> {
                if (shareMapper.selectById(targetId) == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "项目分享不存在");
                }
            }
            case TYPE_SKILL -> {
                if (skillMapper.selectById(targetId) == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "Skill 不存在");
                }
            }
            case TYPE_COMMENT -> {
                // TODO: 评论存在性检查
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的点赞目标类型");
        }
    }

    public long requireUserId(String username) {
        UserResponse user = userService.me(username);
        return user.id();
    }
}
