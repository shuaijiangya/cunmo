package cn.cunmo.application.inventory.result;

/**
 * 透视镜统计结果。
 */
public record InventoryAnalyticsResult(
        long id,
        String code,
        String name,
        long itemCount,
        long quantity,
        int percent) {
}
