package com.community.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 帖子分类实体，对应 post_category 表（种子数据见 Flyway V1）。
 */
@Data
@TableName("post_category")
public class PostCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    private Integer sortOrder;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
