package cn.cunmo.application.inventory.result;

/**
 * 库存结构删除影响预览。
 *
 * @param targetId 待处理对象主键
 * @param targetName 待处理对象名称
 * @param affectedItemCount 受影响物品种类数
 * @param affectedQuantity 受影响库存总量
 * @param bindingCount 受影响绑定数量
 */
public record InventoryDeletionPreviewResult(
        long targetId,
        String targetName,
        int affectedItemCount,
        long affectedQuantity,
        int bindingCount) {
}
