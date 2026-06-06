package cn.cunmo.domain.inventory.model.entity;

import cn.cunmo.domain.inventory.model.enums.StockTransactionType;
import java.time.Instant;

/**
 * 不可变库存流水实体。
 *
 * @param id 流水主键
 * @param itemId 物品主键
 * @param type 流水类型
 * @param delta 数量变化
 * @param quantityBefore 变化前数量
 * @param quantityAfter 变化后数量
 * @param description 业务说明
 * @param occurredAt 发生时间
 */
public record StockTransaction(
        long id,
        long itemId,
        StockTransactionType type,
        int delta,
        int quantityBefore,
        int quantityAfter,
        String description,
        Instant occurredAt) {
}
