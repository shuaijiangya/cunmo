package cn.cunmo.trigger.http.converter;

import cn.cunmo.api.inventory.model.response.InventoryItemMutationResponse;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;

/**
 * 库存 HTTP 协议转换器。
 */
public final class InventoryHttpConverter {

    /**
     * 禁止实例化无状态转换器。
     */
    private InventoryHttpConverter() {
    }

    /**
     * 将库存物品聚合转换为写操作响应。
     */
    public static InventoryItemMutationResponse toMutationResponse(
            InventoryItem item) {
        return new InventoryItemMutationResponse(
                item.id().value(),
                item.spaceId(),
                item.categoryId(),
                item.name(),
                item.smallCategory(),
                item.detailLocation(),
                item.quantity(),
                item.minimumQuantity(),
                item.version());
    }
}
