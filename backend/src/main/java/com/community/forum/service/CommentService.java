package com.community.forum.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.event.ContentCreatedEvent;
import com.community.event.ContentRepliedEvent;
import com.community.forum.dto.CommentResponse;
import com.community.forum.dto.CreateCommentRequest;
import com.community.forum.entity.Comment;
import com.community.forum.mapper.CommentMapper;
import com.community.user.dto.UserResponse;
import com.community.user.service.UserService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论（多态）：宿主为帖子（POST）或项目分享（SHARE），target_id 为宿主 ID。
 * 计数同步口径（仅在帖子宿主时维护 post.reply_count / last_reply_at）：
 * - 新回帖：reply_count +1，last_reply_at 推进为回帖时间；
 * - 回帖删除（逻辑删除）：reply_count -1；last_reply_at 不回退（历史口径，删除不追溯）；
 * - 回帖编辑不影响两者；宿主帖删除时其计数不再有意义，不做回退（防口径混乱）。
 * 楼中楼仅允许一级嵌套：parent 必须是顶级回帖（parent.parent_id == null）。
 */
@Service
public class CommentService {

    public static final String TARGET_POST = "POST";
    public static final String TARGET_SHARE = "SHARE";

    private final CommentMapper commentMapper;
    private final UserService userService;
    private final PostMutationSupport postMutationSupport;
    private final ShareExistenceChecker shareExistenceChecker;
    private final ApplicationEventPublisher eventPublisher;

    public CommentService(CommentMapper commentMapper, UserService userService,
                          PostMutationSupport postMutationSupport,
                          ShareExistenceChecker shareExistenceChecker,
                          ApplicationEventPublisher eventPublisher) {
        this.commentMapper = commentMapper;
        this.userService = userService;
        this.postMutationSupport = postMutationSupport;
        this.shareExistenceChecker = shareExistenceChecker;
        this.eventPublisher = eventPublisher;
    }

    /** 某宿主下的回帖树（顶级按时间正序，楼中楼按时间正序）。 */
    public List<CommentResponse> listTree(String targetType, long targetId) {
        requireTargetType(targetType);
        List<Comment> comments = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId));
        Map<Long, List<Comment>> childrenByParent = new LinkedHashMap<>();
        List<Comment> topLevel = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParentId() == null) {
                topLevel.add(comment);
            } else {
                childrenByParent.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>()).add(comment);
            }
        }
        return topLevel.stream()
                .map(c -> toResponse(c, childrenByParent.get(c.getId())))
                .toList();
    }

    /**
     * 发表回帖：校验宿主存在（POST 走帖子表，SHARE 走项目分享表）；
     * 楼中楼校验父评论归属同一宿主且自身为顶级。
     * 帖子宿主时同事务维护 reply_count / last_reply_at。
     */
    @Transactional
    public CommentResponse create(String targetType, long targetId, String username,
                                  CreateCommentRequest request) {
        requireTargetType(targetType);
        long userId = requireUserId(username);
        if (TARGET_POST.equals(targetType)) {
            postMutationSupport.requirePostExists(targetId);
        } else {
            shareExistenceChecker.requireShareExists(targetId);
        }

        Comment comment = new Comment();
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setUserId(userId);
        comment.setContentMd(request.contentMd());
        comment.setLikeCount(0L);
        if (request.parentId() != null) {
            Comment parent = commentMapper.selectById(request.parentId());
            if (parent == null || parent.getTargetId() != targetId
                    || !targetType.equals(parent.getTargetType())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "父评论不存在");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "楼中楼仅支持一级回复");
            }
            comment.setParentId(parent.getId());
        }
        commentMapper.insert(comment);

        if (TARGET_POST.equals(comment.getTargetType())) {
            postMutationSupport.bumpReplyCount(targetId);
        }
        // T14.6：发布 comment.created 事件（content_text = 评论自身文本，不推父链）
        eventPublisher.publishEvent(new ContentCreatedEvent.CommentCreated(
                this, comment.getId(), comment.getContentMd(), userId, comment.getTargetType().toLowerCase()));
        // T20：发布 ContentRepliedEvent（通知模块消费，防自嗨由 NotifyService 判断）
        long authorId = resolveTargetAuthorId(comment.getTargetType(), comment.getTargetId());
        if (authorId > 0) {
            eventPublisher.publishEvent(new ContentRepliedEvent(
                    this, comment.getTargetType(), comment.getTargetId(),
                    userId, authorId, comment.getContentMd()));
        }
        return toResponse(comment, null);
    }

    /** 删除回帖（逻辑删除，仅限作者本人）；帖子宿主时 reply_count -1（last_reply_at 不回退）。 */
    @Transactional
    public void delete(String targetType, long commentId, String username) {
        requireTargetType(targetType);
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "回帖不存在");
        }
        if (!comment.getUserId().equals(requireUserId(username))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅限作者本人操作");
        }
        commentMapper.deleteById(comment.getId());
        if (TARGET_POST.equals(comment.getTargetType())) {
            postMutationSupport.decreaseReplyCount(comment.getTargetId());
        }
    }

    private CommentResponse toResponse(Comment comment, List<Comment> children) {
        List<CommentResponse> childResponses = children == null ? List.of()
                : children.stream().map(c -> toResponse(c, null)).toList();
        return new CommentResponse(comment.getId(), comment.getUserId(), comment.getParentId(),
                comment.getContentMd(), comment.getLikeCount(), comment.getCreatedAt(), childResponses);
    }

    private void requireTargetType(String targetType) {
        if (!TARGET_POST.equals(targetType) && !TARGET_SHARE.equals(targetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的评论宿主类型");
        }
    }

    private long requireUserId(String username) {
        UserResponse user = userService.me(username);
        return user.id();
    }

    /** T20：从宿主表查作者 ID（供通知模块判断是否自嗨）。 */
    private long resolveTargetAuthorId(String targetType, long targetId) {
        if ("POST".equals(targetType)) {
            return postMutationSupport.getPostAuthorId(targetId);
        }
        // SHARE：通过 shareService 的存在性检查间接获取作者（模块间只调 Service 接口）
        try {
            shareExistenceChecker.requireShareExists(targetId);
            return -1; // SHARE 作者 ID 在 CommentService 中不直接可达，通知模块单独处理
        } catch (Exception e) {
            return -1;
        }
    }
}
