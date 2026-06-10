-- 存魔方库存领域表结构
-- MySQL 8.0+
-- 设计说明：
-- 1. 所有业务数据通过 vault_id 进行魔方域隔离。
-- 2. 表之间仅保留关联字段和必要索引，不创建数据库外键约束。
-- 3. 聚合一致性、关联有效性和级联规则由领域服务与应用事务负责。

CREATE TABLE IF NOT EXISTS `inv_vault` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '魔方域主键',
  `owner_user_id` BIGINT UNSIGNED NOT NULL COMMENT '域所有者用户主键，关联 sys_user.id',
  `vault_name` VARCHAR(64) NOT NULL COMMENT '魔方域名称',
  `item_limit_per_space_category` INT UNSIGNED NULL DEFAULT 10 COMMENT '兼容字段；会员配额策略以 membership_entitlement 为准',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '域状态：1-启用，0-停用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_vault_owner_user` (`owner_user_id`),
  KEY `idx_inv_vault_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='库存魔方域表';

CREATE TABLE IF NOT EXISTS `inv_space` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '空间主键',
  `vault_id` BIGINT UNSIGNED NOT NULL COMMENT '所属魔方域主键，关联 inv_vault.id',
  `space_code` VARCHAR(64) NOT NULL COMMENT '空间稳定编码，用于接口筛选和前端状态',
  `space_name` VARCHAR(64) NOT NULL COMMENT '空间显示名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '空间显示顺序，数值越小越靠前',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '空间状态：1-启用，0-停用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_space_vault_code` (`vault_id`, `space_code`),
  KEY `idx_inv_space_vault_status_sort`
    (`vault_id`, `status`, `deleted`, `sort_order`, `id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='库存空间表';

CREATE TABLE IF NOT EXISTS `inv_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类主键',
  `vault_id` BIGINT UNSIGNED NOT NULL COMMENT '所属魔方域主键，关联 inv_vault.id',
  `category_code` VARCHAR(64) NOT NULL COMMENT '分类稳定编码，用于接口筛选和前端状态',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类显示名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '分类显示顺序，数值越小越靠前',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '分类状态：1-启用，0-停用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_category_vault_code` (`vault_id`, `category_code`),
  KEY `idx_inv_category_vault_status_sort`
    (`vault_id`, `status`, `deleted`, `sort_order`, `id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='库存分类表';

CREATE TABLE IF NOT EXISTS `inv_space_category` (
  `vault_id` BIGINT UNSIGNED NOT NULL COMMENT '所属魔方域主键，关联 inv_vault.id',
  `space_id` BIGINT UNSIGNED NOT NULL COMMENT '空间主键，关联 inv_space.id',
  `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类主键，关联 inv_category.id',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '分类在当前空间内的显示顺序',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '空间与分类绑定时间',
  PRIMARY KEY (`space_id`, `category_id`),
  KEY `idx_inv_space_category_vault_space`
    (`vault_id`, `space_id`, `sort_order`, `category_id`),
  KEY `idx_inv_space_category_category` (`category_id`, `space_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='空间分类关联表';

CREATE TABLE IF NOT EXISTS `inv_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存物品主键',
  `vault_id` BIGINT UNSIGNED NOT NULL COMMENT '所属魔方域主键，关联 inv_vault.id',
  `space_id` BIGINT UNSIGNED NOT NULL COMMENT '所属空间主键，关联 inv_space.id',
  `category_id` BIGINT UNSIGNED NOT NULL COMMENT '所属分类主键，关联 inv_category.id',
  `item_name` VARCHAR(128) NOT NULL COMMENT '物品名称',
  `small_category` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '细分类名称',
  `detail_location` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '空间内精准存放位置',
  `quantity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  `minimum_quantity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最低库存预警线',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '物品状态：1-启用，0-停用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '库存并发更新乐观锁版本号',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_inv_item_vault_updated`
    (`vault_id`, `deleted`, `updated_at`, `id`),
  KEY `idx_inv_item_vault_space_updated`
    (`vault_id`, `space_id`, `deleted`, `updated_at`, `id`),
  KEY `idx_inv_item_vault_category_updated`
    (`vault_id`, `category_id`, `deleted`, `updated_at`, `id`),
  KEY `idx_inv_item_vault_space_category`
    (`vault_id`, `space_id`, `category_id`, `deleted`, `id`),
  KEY `idx_inv_item_vault_name`
    (`vault_id`, `item_name`, `deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='库存物品表';

CREATE TABLE IF NOT EXISTS `inv_stock_transaction` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存流水主键，同时作为游标分页依据',
  `vault_id` BIGINT UNSIGNED NOT NULL COMMENT '所属魔方域主键，关联 inv_vault.id',
  `item_id` BIGINT UNSIGNED NOT NULL COMMENT '库存物品主键，关联 inv_item.id',
  `transaction_type` VARCHAR(16) NOT NULL COMMENT '流水类型：IN-入库，OUT-出库，WARN-预警',
  `quantity_delta` INT NOT NULL COMMENT '本次库存变化量，入库为正数，出库为负数',
  `quantity_before` INT UNSIGNED NOT NULL COMMENT '变更前库存数量',
  `quantity_after` INT UNSIGNED NOT NULL COMMENT '变更后库存数量',
  `description` VARCHAR(255) NOT NULL COMMENT '库存变化业务说明',
  `item_name_snapshot` VARCHAR(128) NOT NULL COMMENT '流水发生时的物品名称快照',
  `space_snapshot` VARCHAR(64) NOT NULL COMMENT '流水发生时的空间名称快照',
  `category_snapshot` VARCHAR(64) NOT NULL COMMENT '流水发生时的分类名称快照',
  `detail_location_snapshot` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '流水发生时的精准位置快照',
  `operator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '执行库存操作的用户主键，关联 sys_user.id',
  `occurred_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '流水发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_inv_transaction_vault_cursor`
    (`vault_id`, `id`),
  KEY `idx_inv_transaction_vault_time`
    (`vault_id`, `occurred_at`, `id`),
  KEY `idx_inv_transaction_item_time`
    (`item_id`, `occurred_at`, `id`),
  KEY `idx_inv_transaction_operator_time`
    (`operator_user_id`, `occurred_at`, `id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='库存流转流水表';
