CREATE DATABASE IF NOT EXISTS `prompt_prompt` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_prompt`;

CREATE TABLE `prompt` (
    `id`              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `category_id`     BIGINT,
    `title`           VARCHAR(200) NOT NULL,
    `description`     VARCHAR(2000),
    `content`         TEXT         NOT NULL,
    `template_schema` JSON,
    `cover`           VARCHAR(500),
    `price`           DECIMAL(10,2) DEFAULT 0,
    `status`          VARCHAR(20)  DEFAULT 'DRAFT',
    `view_count`      INT          DEFAULT 0,
    `download_count`  INT          DEFAULT 0,
    `avg_rating`      DECIMAL(2,1) DEFAULT 0,
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_title_desc (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `prompt_category` (
    `id`        BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `name`      VARCHAR(50)  NOT NULL,
    `parent_id` BIGINT       DEFAULT 0,
    `sort`      INT          DEFAULT 0,
    INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `prompt_tag` (
    `id`   BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(50)  NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `prompt_tag_rel` (
    `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
    `prompt_id` BIGINT NOT NULL,
    `tag_id`    BIGINT NOT NULL,
    UNIQUE KEY uk_prompt_tag (prompt_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `prompt_version` (
    `id`         BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `prompt_id`  BIGINT       NOT NULL,
    `version_no` VARCHAR(20)  NOT NULL,
    `content`    TEXT         NOT NULL,
    `changelog`  VARCHAR(1000),
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_version (prompt_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
