package cn.cunmo.domain.inventory.model.valueobject;

/**
 * 库存物品标识。
 */
public record InventoryItemId(Long value) {

    /**
     * 校验库存物品主键。
     */
    public InventoryItemId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("库存物品主键必须为正数");
        }
    }

    /**
     * 创建已分配物品标识。
     */
    public static InventoryItemId of(long value) {
        return new InventoryItemId(value);
    }

    /**
     * 创建未分配物品标识。
     */
    public static InventoryItemId unassigned() {
        return new InventoryItemId(null);
    }
}
