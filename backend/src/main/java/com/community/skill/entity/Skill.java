package com.community.skill.entity;

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
 * Skill 实体，对应 skill 表。
 */
@Data
@TableName(value = "skill", autoResultMap = true)
public class Skill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String summary;

    /** 提示词正文（Markdown）。 */
    private String contentMd;

    /** 模型适配说明；ALWAYS 以便清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String modelAdaptationMd;

    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> tags;

    private Long viewCount;

    private Long likeCount;

    /** PUBLISHED / HIDDEN。 */
    private String status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
