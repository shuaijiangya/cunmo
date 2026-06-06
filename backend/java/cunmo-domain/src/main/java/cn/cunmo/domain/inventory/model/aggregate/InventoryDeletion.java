package cn.cunmo.domain.inventory.model.aggregate;

/**
 * 库存物品清空删除结果。
 *
 * @param delta 库存变化量
 * @param quantityBefore 删除前数量
 * @param quantityAfter 删除后数量
 */
public record InventoryDeletion(
        int delta,
        int quantityBefore,
        int quantityAfter) {
}
