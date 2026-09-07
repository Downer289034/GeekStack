package com.community.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 论坛帖子实体，对应 post 表。
 */
@Data
@TableName("post")
public class Post {

    /** 数据库自增主键（决策见 docs/开发进度.md 关键决策 8）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    /** 正文（Markdown 原文，前端渲染必须走 utils/markdown.ts 唯一出口）。 */
    private String contentMd;

    private Long viewCount;

    private Long replyCount;

    private LocalDateTime lastReplyAt;

    /** 置顶：0 否 1 是。 */
    private Integer isPinned;

    /** 热门：0 否 1 是。 */
    private Integer isHot;

    /** 逻辑删除标记（全局配置 deleted=1 视为已删）。 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
