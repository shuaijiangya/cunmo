package cn.cunmo.application.inventory.result;

import java.time.Instant;

/**
 * 库存流转轴查询结果。
 */
public record StockTransactionResult(
        long id,
        long itemId,
        String itemName,
        String type,
        int delta,
        int quantityBefore,
        int quantityAfter,
        String description,
        String spaceName,
        String categoryName,
        String detailLocation,
        Instant occurredAt) {
}
