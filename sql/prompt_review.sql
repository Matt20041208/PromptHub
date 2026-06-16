CREATE DATABASE IF NOT EXISTS `prompt_review` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_review`;

CREATE TABLE `review` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `prompt_id`   BIGINT       NOT NULL,
    `user_id`     BIGINT       NOT NULL,
    `order_id`    BIGINT,
    `rating`      TINYINT      NOT NULL,
    `content`     VARCHAR(1000),
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_prompt (prompt_id),
    UNIQUE KEY uk_user_prompt_review (user_id, prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `favorite` (
    `id`          BIGINT   PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT   NOT NULL,
    `prompt_id`   BIGINT   NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_prompt_fav (user_id, prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
