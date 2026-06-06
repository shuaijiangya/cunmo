package cn.cunmo.api.inventory.model.response;

/**
 * 库存物品写操作响应。
 */
public record InventoryItemMutationResponse(
        long id,
        long spaceId,
        long categoryId,
        String name,
        String smallCategory,
        String detailLocation,
        int quantity,
        int minimumQuantity,
        int version) {
}
