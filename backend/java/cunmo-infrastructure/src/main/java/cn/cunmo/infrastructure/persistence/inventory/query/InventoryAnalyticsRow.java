package cn.cunmo.infrastructure.persistence.inventory.query;

/**
 * 透视镜聚合查询行。
 */
public record InventoryAnalyticsRow(
        long id,
        String code,
        String name,
        long itemCount,
        long quantity) {
}
