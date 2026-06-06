package cn.cunmo.domain.inventory.model.aggregate;

/**
 * 库存物品迁移结果。
 *
 * @param sourceSpaceId 原空间主键
 * @param sourceCategoryId 原分类主键
 * @param sourceDetailLocation 原精准位置
 * @param targetSpaceId 目标空间主键
 * @param targetCategoryId 目标分类主键
 * @param targetDetailLocation 目标精准位置
 */
public record InventoryMovement(
        long sourceSpaceId,
        long sourceCategoryId,
        String sourceDetailLocation,
        long targetSpaceId,
        long targetCategoryId,
        String targetDetailLocation) {
}
