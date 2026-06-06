package cn.cunmo.application.inventory.result;

import java.util.List;

/**
 * 库存空间查询结果。
 *
 * @param id 空间主键
 * @param code 空间编码
 * @param name 空间名称
 * @param categories 当前空间可用分类
 */
public record InventorySpaceResult(
        long id,
        String code,
        String name,
        List<InventoryCategoryResult> categories) {

    /**
     * 复制分类列表为不可变集合。
     */
    public InventorySpaceResult {
        categories = List.copyOf(categories);
    }
}
