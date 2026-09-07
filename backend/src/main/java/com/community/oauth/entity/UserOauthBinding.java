package com.community.oauth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 第三方账号绑定实体，对应 user_oauth_binding 表（V1 已建）。
 * access_token_enc 为 AES-GCM 密文（base64），绝不存明文。
 */
@Data
@TableName("user_oauth_binding")
public class UserOauthBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** GITHUB / GITEE。 */
    private String provider;

    /** 平台侧唯一 ID（GitHub 的 numeric id / Gitee 的 id 字符串）。 */
    private String openid;

    private String accessTokenEnc;

    private String refreshToken;

    private LocalDateTime expiresAt;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
