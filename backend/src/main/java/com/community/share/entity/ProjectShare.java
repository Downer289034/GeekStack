package com.community.share.entity;

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
 * 项目分享实体，对应 project_share 表。
 */
@Data
@TableName(value = "project_share", autoResultMap = true)
public class ProjectShare {

    /** 数据库自增主键（不用 MP 默认雪花 ID，避免超出 JS Number 安全整数范围）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    /** 一句话介绍。 */
    private String summary;

    /** 详细说明（Markdown 原文，渲染在前端做并过 DOMPurify）。 */
    private String contentMd;

    /** 可选字段用 IGNORED：编辑时传 null 即清空（P2-5，MP 3.5.12 中 ALWAYS 即旧版 IGNORED 语义），避免 NOT_NULL 默认策略跳过 null。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String coverUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String repoUrl;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String demoUrl;

    /** 技术栈标签，JSON 数组列。 */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> tags;

    private Long viewCount;

    /** PUBLISHED / HIDDEN（下架）。 */
    private String status;

    /** 逻辑删除标记（全局配置 deleted=1 视为已删）。 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
