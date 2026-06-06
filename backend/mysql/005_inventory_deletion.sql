-- 库存结构删除与迁移能力
-- MySQL 8.0+
-- 说明：保留原快照字段兼容历史查询，同时增加来源和目标快照。

ALTER TABLE `inv_stock_transaction`
  MODIFY COLUMN `transaction_type` VARCHAR(16) NOT NULL
    COMMENT '流水类型：IN-入库，OUT-出库，WARN-预警，MOVE-迁移，DELETE-清空删除',
  ADD COLUMN `source_space_snapshot` VARCHAR(64) NULL
    COMMENT '迁移或删除前的空间名称快照' AFTER `detail_location_snapshot`,
  ADD COLUMN `source_category_snapshot` VARCHAR(64) NULL
    COMMENT '迁移或删除前的分类名称快照' AFTER `source_space_snapshot`,
  ADD COLUMN `source_detail_location_snapshot` VARCHAR(128) NULL
    COMMENT '迁移或删除前的精准位置快照' AFTER `source_category_snapshot`,
  ADD COLUMN `target_space_snapshot` VARCHAR(64) NULL
    COMMENT '迁移后的目标空间名称快照，非迁移流水为空' AFTER `source_detail_location_snapshot`,
  ADD COLUMN `target_category_snapshot` VARCHAR(64) NULL
    COMMENT '迁移后的目标分类名称快照，非迁移流水为空' AFTER `target_space_snapshot`,
  ADD COLUMN `target_detail_location_snapshot` VARCHAR(128) NULL
    COMMENT '迁移后的目标精准位置快照，非迁移流水为空' AFTER `target_category_snapshot`;
