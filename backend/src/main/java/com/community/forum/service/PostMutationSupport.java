package com.community.forum.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.community.common.BusinessException;
import com.community.common.ErrorCode;
import com.community.forum.entity.Post;
import com.community.forum.mapper.PostMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * 帖子计数变更支撑（forum 模块内部）：回帖联动 reply_count / last_reply_at 的唯一写入点，
 * 供 CommentService 在同事务中调用，避免多处手写 UPDATE 造成口径漂移。
 */
@Service
public class PostMutationSupport {

    private final PostMapper postMapper;

    public PostMutationSupport(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    /** 断言帖子存在（未逻辑删除）。 */
    public void requirePostExists(long postId) {
        if (postMapper.selectById(postId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "帖子不存在");
        }
    }

    /** 查帖子作者 ID（供通知/事件使用）。返回 -1 表示不存在。 */
    public long getPostAuthorId(long postId) {
        Post post = postMapper.selectById(postId);
        return post != null ? post.getUserId() : -1;
    }

    /** 回帖后：reply_count +1，last_reply_at 推进为当前时间。 */
    public void bumpReplyCount(long postId) {
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("reply_count = reply_count + 1")
                .set(Post::getLastReplyAt, LocalDateTime.now()));
    }

    /** 回帖删除后：reply_count -1（下限 0）；last_reply_at 不回退（历史口径，见 CommentService 注释）。 */
    public void decreaseReplyCount(long postId) {
        postMapper.update(null, Wrappers.<Post>lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("reply_count = GREATEST(reply_count - 1, 0)"));
    }
}
