package cn.cunmo.domain.inventory.model.entity;

/**
 * 库存分类实体。
 *
 * @param id 分类主键
 * @param code 分类编码
 * @param name 分类名称
 * @param sortOrder 显示顺序
 */
public record InventoryCategory(
        long id,
        String code,
        String name,
        int sortOrder) {
}
