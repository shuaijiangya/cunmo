-- 会员、支付、自动续费和升级申请

ALTER TABLE `inv_vault`
  MODIFY COLUMN `item_limit_per_space_category`
    INT UNSIGNED NULL DEFAULT 10
    COMMENT '兼容字段；会员配额策略以 membership_entitlement 为准';

UPDATE `inv_vault`
SET `item_limit_per_space_category` = 10
WHERE `item_limit_per_space_category` = 20;

CREATE TABLE IF NOT EXISTS `membership_entitlement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `plan_type` VARCHAR(24) NOT NULL DEFAULT 'FREE',
  `effective_at` DATETIME(3) NULL,
  `expires_at` DATETIME(3) NULL,
  `source_reference` VARCHAR(64) NULL,
  `version` INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_entitlement_user` (`user_id`),
  KEY `idx_membership_entitlement_expiry` (`plan_type`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `membership_order` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `product_code` VARCHAR(24) NOT NULL,
  `amount_fen` INT UNSIGNED NOT NULL,
  `auto_renew` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED',
  `wechat_transaction_id` VARCHAR(64) NULL,
  `prepay_id` VARCHAR(128) NULL,
  `paid_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_order_no` (`order_no`),
  UNIQUE KEY `uk_membership_order_wechat_transaction`
    (`wechat_transaction_id`),
  KEY `idx_membership_order_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `membership_renewal_agreement` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `contract_code` VARCHAR(64) NOT NULL,
  `wechat_contract_id` VARCHAR(128) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `next_charge_at` DATETIME(3) NULL,
  `consecutive_failures` INT UNSIGNED NOT NULL DEFAULT 0,
  `terminated_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_renewal_user` (`user_id`),
  UNIQUE KEY `uk_membership_renewal_contract_code` (`contract_code`),
  UNIQUE KEY `uk_membership_renewal_wechat_contract`
    (`wechat_contract_id`),
  KEY `idx_membership_renewal_due` (`status`, `next_charge_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `membership_renewal_attempt` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `attempt_no` VARCHAR(64) NOT NULL,
  `agreement_id` BIGINT UNSIGNED NOT NULL,
  `amount_fen` INT UNSIGNED NOT NULL,
  `scheduled_at` DATETIME(3) NOT NULL,
  `charged_at` DATETIME(3) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED',
  `wechat_transaction_id` VARCHAR(64) NULL,
  `failure_reason` VARCHAR(255) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_renewal_attempt_no` (`attempt_no`),
  UNIQUE KEY `uk_membership_renewal_attempt_wechat`
    (`wechat_transaction_id`),
  KEY `idx_membership_renewal_attempt_agreement`
    (`agreement_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `membership_upgrade_request` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `contact` VARCHAR(128) NOT NULL,
  `remark` VARCHAR(500) NOT NULL DEFAULT '',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `reviewer_user_id` BIGINT UNSIGNED NULL,
  `grant_type` VARCHAR(24) NULL,
  `grant_months` INT UNSIGNED NULL,
  `review_note` VARCHAR(500) NULL,
  `reviewed_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_membership_upgrade_user_status` (`user_id`, `status`),
  KEY `idx_membership_upgrade_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `membership_payment_notification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `notification_id` VARCHAR(64) NOT NULL,
  `notification_type` VARCHAR(32) NOT NULL,
  `business_reference` VARCHAR(64) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'RECEIVED',
  `processed_at` DATETIME(3) NULL,
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_notification_id` (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `sys_permission`
  (`parent_id`, `permission_code`, `permission_name`, `permission_type`,
   `resource_path`, `http_method`, `sort_order`, `status`)
VALUES
  (0, 'membership:upgrade:review', '审批会员升级申请', 'API',
   '/api/admin/membership/**', NULL, 120, 1)
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `resource_path` = VALUES(`resource_path`),
  `status` = VALUES(`status`);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role_table.id, permission_table.id
FROM `sys_role` role_table
JOIN `sys_permission` permission_table
  ON permission_table.permission_code = 'membership:upgrade:review'
WHERE role_table.role_code = 'ADMIN';
