package cn.cunmo.application.inventory.result;

/**
 * 库存分类查询结果。
 *
 * @param id 分类主键
 * @param code 分类编码
 * @param name 分类名称
 * @param itemCount 当前空间下物品种类数
 * @param remainingCount 剩余可创建种类数，无限时为空
 * @param capacityReached 是否达到容量
 */
public record InventoryCategoryResult(
        long id,
        String code,
        String name,
        int itemCount,
        Integer remainingCount,
        boolean capacityReached) {
}
