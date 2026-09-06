package com.community.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户实体，对应 user 表（user_oauth_binding 等关联表在对应模块中定义）。
 */
@Data
@TableName("user")
public class User {

    private Long id;

    private String username;

    private String email;

    /** BCrypt 摘要；第三方登录用户为空。 */
    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String bio;

    /** 角色编码：USER / ADMIN（RBAC 预留）。 */
    private String role;

    /** 状态：0 正常 1 封禁。 */
    private Integer status;

    /** 逻辑删除标记（全局配置 deleted=1 视为已删）。 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
