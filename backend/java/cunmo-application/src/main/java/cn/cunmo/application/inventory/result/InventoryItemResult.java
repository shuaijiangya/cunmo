package cn.cunmo.application.inventory.result;

/**
 * 库存物品查询结果。
 */
public record InventoryItemResult(
        long id,
        String name,
        long spaceId,
        String spaceCode,
        String spaceName,
        long categoryId,
        String categoryCode,
        String categoryName,
        String smallCategory,
        String detailLocation,
        int quantity,
        int minimumQuantity,
        int version) {
}
