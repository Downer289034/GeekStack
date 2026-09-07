package com.community.oauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 仓库快照实体，对应 user_repo 表。
 * 唯一键 (user_id, provider, repo_full_name) — upsert 按此键匹配。
 */
@Data
@TableName("user_repo")
public class UserRepo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** GITHUB / GITEE。 */
    private String provider;

    /** owner/name */
    private String repoFullName;

    private String repoUrl;

    private String description;

    private Long stars;

    private LocalDateTime pushedAt;

    private LocalDateTime syncedAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
