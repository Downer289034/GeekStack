package com.community.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评论实体，对应 comment 表。多态挂载：target_type = POST（帖子）或 SHARE（项目分享），
 * target_id 为对应宿主 ID；parent_id 非空时为楼中楼回复。
 * 回帖业务逻辑（reply_count / last_reply_at 维护）在 Task 12 实现，本任务仅提供实体与 Mapper。
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** POST / SHARE。 */
    private String targetType;

    private Long targetId;

    private Long userId;

    /** 楼中楼父评论 ID；顶级回帖为空。 */
    private Long parentId;

    private String contentMd;

    private Long likeCount;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
