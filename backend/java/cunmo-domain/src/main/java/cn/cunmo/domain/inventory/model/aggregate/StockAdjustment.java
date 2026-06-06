package cn.cunmo.domain.inventory.model.aggregate;

import cn.cunmo.domain.inventory.model.enums.StockTransactionType;

/**
 * 库存调整领域结果。
 *
 * @param delta 库存变化量
 * @param quantityBefore 调整前数量
 * @param quantityAfter 调整后数量
 * @param type 流水类型
 */
public record StockAdjustment(
        int delta,
        int quantityBefore,
        int quantityAfter,
        StockTransactionType type) {
}
