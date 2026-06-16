CREATE DATABASE IF NOT EXISTS `prompt_search` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_search`;

CREATE TABLE `search_log` (
    `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`      BIGINT,
    `keyword`      VARCHAR(200) NOT NULL,
    `result_count` INT          DEFAULT 0,
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_keyword (keyword),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
