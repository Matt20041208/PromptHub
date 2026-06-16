CREATE DATABASE IF NOT EXISTS `prompt_trade` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `prompt_trade`;

CREATE TABLE `order_info` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `order_no`    VARCHAR(32)  NOT NULL UNIQUE,
    `buyer_id`    BIGINT       NOT NULL,
    `seller_id`   BIGINT       NOT NULL,
    `prompt_id`   BIGINT       NOT NULL,
    `amount`      DECIMAL(10,2) NOT NULL,
    `status`      VARCHAR(20)  DEFAULT 'UNPAID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `pay_time`    DATETIME,
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_prompt (prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `purchase_record` (
    `id`          BIGINT    PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT    NOT NULL,
    `prompt_id`   BIGINT    NOT NULL,
    `order_id`    BIGINT    NOT NULL,
    `create_time` DATETIME  DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_prompt (user_id, prompt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_balance` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL UNIQUE,
    `balance`     DECIMAL(10,2) DEFAULT 0,
    `freeze`      DECIMAL(10,2) DEFAULT 0,
    `version`     INT           DEFAULT 1,
    `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `transaction_log` (
    `id`             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `order_id`       BIGINT,
    `type`           VARCHAR(20)  NOT NULL,
    `amount`         DECIMAL(10,2) NOT NULL,
    `balance_before` DECIMAL(10,2),
    `balance_after`  DECIMAL(10,2),
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
