package cn.cunmo.domain.inventory.model.entity;

/**
 * 库存空间实体。
 *
 * @param id 空间主键
 * @param code 空间编码
 * @param name 空间名称
 * @param sortOrder 显示顺序
 */
public record InventorySpace(
        long id,
        String code,
        String name,
        int sortOrder) {
}
