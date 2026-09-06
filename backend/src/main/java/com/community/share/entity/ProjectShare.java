package com.community.share.entity;

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

    private Long id;

    private Long userId;

    private String title;

    /** 一句话介绍。 */
    private String summary;

    /** 详细说明（Markdown 原文，渲染在前端做并过 DOMPurify）。 */
    private String contentMd;

    private String coverUrl;

    private String repoUrl;

    private String demoUrl;

    /** 技术栈标签，JSON 数组列。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private Long viewCount;

    /** PUBLISHED / HIDDEN（下架）。 */
    private String status;

    /** 逻辑删除标记（全局配置 deleted=1 视为已删）。 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
