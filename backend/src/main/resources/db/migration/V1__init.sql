-- =====================================================================
-- V1: 初始化核心表（依据《架构与产品规划》第五节）+ 预置帖子分类
-- 约定：所有表含 created_at / updated_at / deleted（逻辑删除，0 否 1 是）
-- 注：post_category 须先于 post 建表（外键依赖）
-- =====================================================================

-- 帖子分类（预置：Java / AI / 问答 / 资源分享）
CREATE TABLE `post_category` (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)     NOT NULL COMMENT '分类名',
    slug       VARCHAR(50)     NOT NULL COMMENT 'URL 标识',
    sort_order INT             NOT NULL DEFAULT 0,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子分类';

-- 用户
CREATE TABLE `user` (
    id            BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)          NOT NULL COMMENT '用户名（登录用）',
    email         VARCHAR(100)         NULL COMMENT '邮箱；第三方登录用户可能为空',
    password_hash VARCHAR(100)         NULL COMMENT 'BCrypt 摘要；第三方登录用户为空',
    nickname      VARCHAR(50)          NULL COMMENT '昵称（展示用）',
    avatar_url    VARCHAR(500)         NULL COMMENT '头像地址',
    bio           VARCHAR(500)         NULL COMMENT '个人简介',
    tech_stack    JSON                 NULL COMMENT '技术栈标签数组',
    role          ENUM ('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '角色（RBAC 预留）',
    status        TINYINT              NOT NULL DEFAULT 0 COMMENT '状态：0 正常 1 封禁',
    created_at    DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT              NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';

-- 第三方账号绑定（一个用户可绑多个平台）
CREATE TABLE `user_oauth_binding` (
    id               BIGINT UNSIGNED        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED        NOT NULL COMMENT '所属用户',
    provider         ENUM ('GITHUB', 'GITEE') NOT NULL COMMENT '平台',
    openid           VARCHAR(128)           NOT NULL COMMENT '平台侧唯一 ID',
    access_token_enc VARCHAR(2048)          NOT NULL COMMENT 'AES-GCM 加密的 access token（base64）',
    refresh_token    VARCHAR(512)           NULL COMMENT '刷新令牌（Gitee 需要）',
    expires_at       DATETIME               NULL COMMENT 'token 过期时间',
    created_at       DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT                NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_binding_provider_openid (provider, openid),
    KEY idx_binding_user (user_id),
    CONSTRAINT fk_binding_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '第三方账号绑定';

-- 绑定的仓库快照（用于主页展示，登录/绑定时刷新，失败保留旧快照）
CREATE TABLE `user_repo` (
    id             BIGINT UNSIGNED        NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED        NOT NULL,
    provider       ENUM ('GITHUB', 'GITEE') NOT NULL,
    repo_full_name VARCHAR(255)           NOT NULL COMMENT 'owner/name',
    repo_url       VARCHAR(500)           NOT NULL,
    description    VARCHAR(1000)          NULL,
    stars          INT UNSIGNED           NOT NULL DEFAULT 0,
    pushed_at      DATETIME               NULL COMMENT '仓库最近推送时间',
    synced_at      DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本次快照同步时间',
    created_at     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT                NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repo_user_provider_name (user_id, provider, repo_full_name),
    CONSTRAINT fk_repo_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '仓库快照';

-- 项目分享
CREATE TABLE `project_share` (
    id         BIGINT UNSIGNED              NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED              NOT NULL,
    title      VARCHAR(200)                 NOT NULL,
    summary    VARCHAR(500)                 NOT NULL COMMENT '一句话介绍',
    content_md MEDIUMTEXT                   NOT NULL COMMENT '详细说明（Markdown 原文）',
    cover_url  VARCHAR(500)                 NULL,
    repo_url   VARCHAR(500)                 NULL,
    demo_url   VARCHAR(500)                 NULL COMMENT '在线 Demo 地址',
    tags       JSON                         NULL COMMENT '技术栈标签数组',
    view_count INT UNSIGNED                 NOT NULL DEFAULT 0,
    status     ENUM ('PUBLISHED', 'HIDDEN') NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT                      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_share_status_created (status, created_at),
    CONSTRAINT fk_share_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目分享';

-- 论坛帖子
CREATE TABLE `post` (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id       BIGINT UNSIGNED NOT NULL,
    category_id   BIGINT UNSIGNED NOT NULL,
    title         VARCHAR(200)    NOT NULL,
    content_md    MEDIUMTEXT      NOT NULL COMMENT '正文（Markdown 原文）',
    view_count    INT UNSIGNED    NOT NULL DEFAULT 0,
    reply_count   INT UNSIGNED    NOT NULL DEFAULT 0,
    last_reply_at DATETIME        NULL,
    is_pinned     TINYINT         NOT NULL DEFAULT 0 COMMENT '置顶：0 否 1 是',
    is_hot        TINYINT         NOT NULL DEFAULT 0 COMMENT '热门：0 否 1 是',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_post_category_created (category_id, created_at),
    CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_post_category FOREIGN KEY (category_id) REFERENCES `post_category` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '论坛帖子';

-- 评论（多态挂载：帖子或项目分享；两级结构 parent_id）
CREATE TABLE `comment` (
    id          BIGINT UNSIGNED      NOT NULL AUTO_INCREMENT,
    target_type ENUM ('POST', 'SHARE') NOT NULL COMMENT '宿主类型',
    target_id   BIGINT UNSIGNED      NOT NULL COMMENT '宿主 ID（post 或 project_share）',
    user_id     BIGINT UNSIGNED      NOT NULL,
    parent_id   BIGINT UNSIGNED      NULL COMMENT '楼中楼父评论 ID',
    content_md  TEXT                 NOT NULL,
    like_count  INT UNSIGNED         NOT NULL DEFAULT 0,
    created_at  DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT              NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_comment_target (target_type, target_id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES `comment` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论';

-- 站内通知（M3 事件驱动写入，表先建好）
CREATE TABLE `notification` (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '接收人',
    type       VARCHAR(32)     NOT NULL COMMENT '通知类型（REPLY/LIKE 等）',
    title      VARCHAR(200)    NOT NULL,
    content    VARCHAR(1000)   NULL,
    is_read    TINYINT         NOT NULL DEFAULT 0,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_user_read (user_id, is_read),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知';

-- 预置分类种子数据（ID 固定，便于引用）
INSERT INTO `post_category` (id, name, slug, sort_order)
VALUES (1, 'Java', 'java', 1),
       (2, 'AI', 'ai', 2),
       (3, '问答', 'qa', 3),
       (4, '资源分享', 'resource', 4);
