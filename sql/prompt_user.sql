-- ============================================
-- 用户服务数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS `prompt_user` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_user`;

CREATE TABLE `user` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL UNIQUE,
    `nickname`    VARCHAR(50),
    `password`    VARCHAR(255) NOT NULL,
    `email`       VARCHAR(100),
    `phone`       VARCHAR(20),
    `avatar`      VARCHAR(500),
    `bio`         VARCHAR(500),
    `role`        VARCHAR(20)  DEFAULT 'USER',
    `status`      TINYINT      DEFAULT 1,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_auth` (
    `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `auth_type`    VARCHAR(20)  NOT NULL,
    `open_id`      VARCHAR(100),
    `access_token` VARCHAR(500),
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
