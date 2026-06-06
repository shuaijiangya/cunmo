-- 存魔用户与 RBAC 基础表结构
-- MySQL 8.0+

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` VARCHAR(64) NULL COMMENT '业务用户名，微信注册用户可为空',
  `nickname` VARCHAR(64) NULL COMMENT '用户昵称',
  `avatar_url` VARCHAR(512) NULL COMMENT '用户头像业务访问地址',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '用户状态：1-启用，0-禁用',
  `last_login_at` DATETIME(3) NULL COMMENT '最近一次成功登录时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `sys_wechat_identity` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '微信身份主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id 的系统用户主键',
  `appid` VARCHAR(64) NOT NULL COMMENT '微信小程序 AppID',
  `openid` VARCHAR(128) NOT NULL COMMENT '用户在当前小程序下的唯一标识',
  `unionid` VARCHAR(128) NULL COMMENT '微信开放平台统一用户标识',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_identity_appid_openid` (`appid`, `openid`),
  KEY `idx_wechat_identity_user_id` (`user_id`),
  KEY `idx_wechat_identity_unionid` (`unionid`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户微信身份表';

CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色唯一编码',
  `role_name` VARCHAR(64) NOT NULL COMMENT '角色显示名称',
  `description` VARCHAR(255) NULL COMMENT '角色用途说明',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '角色状态：1-启用，0-禁用',
  `data_scope` VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围：ALL-全部，DEPARTMENT-部门，SELF-本人',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`),
  KEY `idx_sys_role_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限主键',
  `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父权限主键，根节点为 0',
  `permission_code` VARCHAR(128) NOT NULL COMMENT '权限唯一编码，格式为领域:资源:动作',
  `permission_name` VARCHAR(64) NOT NULL COMMENT '权限显示名称',
  `permission_type` VARCHAR(16) NOT NULL COMMENT '权限类型：MENU、BUTTON、API',
  `resource_path` VARCHAR(255) NULL COMMENT '菜单路由或 API 资源路径',
  `http_method` VARCHAR(16) NULL COMMENT 'API 权限对应的 HTTP 方法',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '同级权限显示顺序',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '权限状态：1-启用，0-禁用',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  `deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`permission_code`),
  KEY `idx_sys_permission_parent_sort` (`parent_id`, `sort_order`),
  KEY `idx_sys_permission_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统权限资源表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_user.id 的系统用户主键',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_role.id 的系统角色主键',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '角色授权时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_role_role_id` (`role_id`)) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_role.id 的系统角色主键',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '关联 sys_permission.id 的系统权限主键',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '权限授权时间',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_permission_permission_id` (`permission_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色权限关联表';
