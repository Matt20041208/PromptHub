CREATE DATABASE IF NOT EXISTS `prompt_notify` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_notify`;

CREATE TABLE `notification` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `type`        VARCHAR(20)  NOT NULL,
    `title`       VARCHAR(200) NOT NULL,
    `content`     VARCHAR(2000),
    `is_read`     TINYINT      DEFAULT 0,
    `biz_type`    VARCHAR(20),
    `biz_id`      BIGINT,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `notification_template` (
    `id`                BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `code`              VARCHAR(50)  NOT NULL UNIQUE,
    `title_template`    VARCHAR(200),
    `content_template`  VARCHAR(2000),
    `channel`           VARCHAR(50)  DEFAULT 'IN_APP',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
