package com.community.like.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 点赞明细实体（多态，target_type = POST/SHARE/COMMENT）。
 * 唯一键 (user_id, target_type, target_id) 兜底防重。
 */
@Data
@TableName("user_like")
public class UserLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** POST / SHARE / COMMENT（V3 CHECK 约束）。 */
    private String targetType;

    private Long targetId;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
