-- 初始化系统角色
INSERT INTO `sys_role`
  (`role_code`, `role_name`, `description`, `status`, `data_scope`)
VALUES
  ('USER', '普通用户', '微信首次登录后自动绑定的基础角色', 1, 'SELF'),
  ('ADMIN', '平台管理员', '拥有平台全部管理权限的系统角色', 1, 'ALL')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `data_scope` = VALUES(`data_scope`);

-- 初始化库存领域权限
INSERT INTO `sys_permission`
  (`parent_id`, `permission_code`, `permission_name`, `permission_type`,
   `resource_path`, `http_method`, `sort_order`, `status`)
VALUES
  (0, 'inventory:item:read', '查看库存物品', 'API', '/api/items/**', 'GET', 10, 1),
  (0, 'inventory:item:create', '创建库存物品', 'API', '/api/items', 'POST', 20, 1),
  (0, 'inventory:item:update', '修改库存物品', 'API', '/api/items/**', 'PUT', 30, 1),
  (0, 'inventory:item:delete', '删除库存物品', 'API', '/api/items/**', 'DELETE', 40, 1),
  (0, 'inventory:space:manage', '管理存储空间', 'BUTTON', NULL, NULL, 50, 1),
  (0, 'system:user:manage', '管理系统用户', 'API', '/api/admin/users/**', NULL, 100, 1),
  (0, 'system:role:manage', '管理角色权限', 'API', '/api/admin/roles/**', NULL, 110, 1)
ON DUPLICATE KEY UPDATE
  `permission_name` = VALUES(`permission_name`),
  `permission_type` = VALUES(`permission_type`),
  `resource_path` = VALUES(`resource_path`),
  `http_method` = VALUES(`http_method`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`);

-- 普通用户拥有库存日常操作权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role_table.id, permission_table.id
FROM `sys_role` role_table
JOIN `sys_permission` permission_table
  ON permission_table.permission_code IN (
    'inventory:item:read',
    'inventory:item:create',
    'inventory:item:update',
    'inventory:space:manage'
  )
WHERE role_table.role_code = 'USER';

-- 平台管理员拥有全部有效权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT role_table.id, permission_table.id
FROM `sys_role` role_table
CROSS JOIN `sys_permission` permission_table
WHERE role_table.role_code = 'ADMIN'
  AND permission_table.status = 1
  AND permission_table.deleted = 0;
