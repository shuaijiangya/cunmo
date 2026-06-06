package cn.cunmo.infrastructure.persistence.inventory.query;

/**
 * 魔方域汇总查询行。
 */
public record InventoryVaultSummaryRow(
        long vaultId,
        String vaultName,
        Integer itemLimitPerSpaceCategory,
        long totalQuantity) {
}
