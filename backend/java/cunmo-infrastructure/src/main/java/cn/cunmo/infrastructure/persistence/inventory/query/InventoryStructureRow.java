package cn.cunmo.infrastructure.persistence.inventory.query;

/**
 * 首屏空间分类扁平查询行。
 */
public record InventoryStructureRow(
        long spaceId,
        String spaceCode,
        String spaceName,
        Long categoryId,
        String categoryCode,
        String categoryName,
        int itemCount) {
}
