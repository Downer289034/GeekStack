package com.community.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 论坛帖子实体，对应 post 表。
 */
@Data
@TableName(value = "post", autoResultMap = true)
public class Post {

    /** 数据库自增主键（决策见 docs/开发进度.md 关键决策 8）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    /** 正文（Markdown 原文，前端渲染必须走 utils/markdown.ts 唯一出口）。 */
    private String contentMd;

    /** 标签数组（T-TAG-BE；存量默认可为空数组）。 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> tags;

    private Long viewCount;

    private Long replyCount;

    private Long likeCount;

    private LocalDateTime lastReplyAt;

    /** 置顶：0 否 1 是。 */
    private Integer isPinned;

    /** 加精：0 否 1 是（T-ADMIN-FEATURE）。 */
    private Integer isFeatured;

    /** 热门：0 否 1 是。 */
    private Integer isHot;

    /** 逻辑删除标记（全局配置 deleted=1 视为已删）。 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
