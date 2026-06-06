-- 存魔库存开发联调数据
-- 仅用于本地开发环境，请勿在生产环境执行。
-- 执行前必须先登录一次并调用 GET /api/inventory/bootstrap，
-- 确保目标用户已经创建默认魔方域、三个空间和三个分类。

SET @seed_user_id = (
  SELECT MIN(`id`)
  FROM `sys_user`
  WHERE `status` = 1 AND `deleted` = 0
);

SET @seed_vault_id = (
  SELECT `id`
  FROM `inv_vault`
  WHERE `owner_user_id` = @seed_user_id
    AND `status` = 1
    AND `deleted` = 0
  LIMIT 1
);

-- 增加开发分类，使分类透视镜可以验证游标分页。
INSERT INTO `inv_category`
  (`vault_id`, `category_code`, `category_name`, `sort_order`,
   `status`, `version`, `deleted`)
SELECT @seed_vault_id, seed.code, seed.name, seed.sort_order, 1, 0, 0
FROM (
  SELECT 'seed_books' AS code, '图书资料' AS name, 110 AS sort_order
  UNION ALL SELECT 'seed_tools', '工具耗材', 120
  UNION ALL SELECT 'seed_medicine', '常备药品', 130
  UNION ALL SELECT 'seed_kitchen', '厨房用品', 140
  UNION ALL SELECT 'seed_cleaning', '清洁用品', 150
  UNION ALL SELECT 'seed_sports', '运动装备', 160
  UNION ALL SELECT 'seed_travel', '旅行用品', 170
  UNION ALL SELECT 'seed_collectibles', '收藏陈列', 180
  UNION ALL SELECT 'seed_documents', '证件档案', 190
  UNION ALL SELECT 'seed_emergency', '应急物资', 200
  UNION ALL SELECT 'seed_hobby', '兴趣手作', 210
  UNION ALL SELECT 'seed_network', '网络设备', 220
) seed
WHERE @seed_vault_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `category_name` = VALUES(`category_name`),
  `sort_order` = VALUES(`sort_order`),
  `status` = 1,
  `deleted` = 0;

-- 将开发分类轮流绑定到三个默认空间。
INSERT IGNORE INTO `inv_space_category`
  (`vault_id`, `space_id`, `category_id`, `sort_order`)
SELECT @seed_vault_id, space_table.id, category_table.id,
       category_table.sort_order
FROM `inv_category` category_table
JOIN `inv_space` space_table
  ON space_table.vault_id = category_table.vault_id
 AND space_table.space_code = CASE
       WHEN category_table.category_code IN (
         'seed_books', 'seed_medicine', 'seed_collectibles',
         'seed_emergency'
       ) THEN 'bedroom'
       WHEN category_table.category_code IN (
         'seed_kitchen', 'seed_cleaning', 'seed_sports',
         'seed_hobby'
       ) THEN 'living'
       ELSE 'office'
     END
WHERE category_table.vault_id = @seed_vault_id
  AND category_table.category_code LIKE 'seed\_%'
  AND category_table.status = 1
  AND category_table.deleted = 0;

-- 每个开发分类放入一个示例物品，重复执行不会重复建货。
INSERT INTO `inv_item`
  (`vault_id`, `space_id`, `category_id`, `item_name`,
   `small_category`, `detail_location`, `quantity`,
   `minimum_quantity`, `status`, `version`, `deleted`)
SELECT @seed_vault_id, space_table.id, category_table.id,
       seed.item_name, seed.small_category, seed.detail_location,
       seed.quantity, seed.minimum_quantity, 1, 0, 0
FROM (
  SELECT 'seed_books' AS category_code, 'Java 领域驱动设计' AS item_name,
         '技术图书' AS small_category, '书架 A2' AS detail_location,
         2 AS quantity, 1 AS minimum_quantity
  UNION ALL SELECT 'seed_tools', '精密螺丝刀套装', '维修工具', '工具柜上层', 1, 1
  UNION ALL SELECT 'seed_medicine', '创可贴', '外用护理', '床头柜药盒', 18, 5
  UNION ALL SELECT 'seed_kitchen', '保鲜袋', '厨房消耗品', '橱柜抽屉', 24, 8
  UNION ALL SELECT 'seed_cleaning', '除尘湿巾', '清洁消耗品', '电视柜侧格', 10, 3
  UNION ALL SELECT 'seed_sports', '瑜伽弹力带', '训练器材', '客厅收纳箱', 3, 1
  UNION ALL SELECT 'seed_travel', '旅行转换插头', '出行配件', '办公室抽屉', 2, 1
  UNION ALL SELECT 'seed_collectibles', '纪念徽章册', '收藏册', '卧室展示柜', 1, 0
  UNION ALL SELECT 'seed_documents', '设备保修卡', '重要凭证', '文件柜 B1', 8, 2
  UNION ALL SELECT 'seed_emergency', '应急手电筒', '应急照明', '卧室门后柜', 2, 1
  UNION ALL SELECT 'seed_hobby', '水彩颜料套装', '绘画材料', '客厅边柜', 1, 0
  UNION ALL SELECT 'seed_network', '千兆交换机', '网络设备', '办公桌线槽', 1, 0
) seed
JOIN `inv_category` category_table
  ON category_table.vault_id = @seed_vault_id
 AND category_table.category_code = seed.category_code
JOIN `inv_space_category` binding
  ON binding.vault_id = @seed_vault_id
 AND binding.category_id = category_table.id
JOIN `inv_space` space_table
  ON space_table.id = binding.space_id
LEFT JOIN `inv_item` existing_item
  ON existing_item.vault_id = @seed_vault_id
 AND existing_item.item_name = seed.item_name
 AND existing_item.deleted = 0
WHERE existing_item.id IS NULL;

-- 为所有开发物品补充初始入库流水，流转轴可验证第二页加载。
INSERT INTO `inv_stock_transaction`
  (`vault_id`, `item_id`, `transaction_type`, `quantity_delta`,
   `quantity_before`, `quantity_after`, `description`,
   `item_name_snapshot`, `space_snapshot`, `category_snapshot`,
   `detail_location_snapshot`, `operator_user_id`, `occurred_at`)
SELECT item.vault_id, item.id, 'IN', item.quantity,
       0, item.quantity, '开发联调示例初始入库',
       item.item_name, space_table.space_name,
       category_table.category_name, item.detail_location,
       @seed_user_id,
       TIMESTAMPADD(
         MINUTE,
         -ROW_NUMBER() OVER (ORDER BY item.id),
         CURRENT_TIMESTAMP(3)
       )
FROM `inv_item` item
JOIN `inv_space` space_table ON space_table.id = item.space_id
JOIN `inv_category` category_table ON category_table.id = item.category_id
LEFT JOIN `inv_stock_transaction` existing_transaction
  ON existing_transaction.item_id = item.id
 AND existing_transaction.description = '开发联调示例初始入库'
WHERE item.vault_id = @seed_vault_id
  AND category_table.category_code LIKE 'seed\_%'
  AND existing_transaction.id IS NULL;
