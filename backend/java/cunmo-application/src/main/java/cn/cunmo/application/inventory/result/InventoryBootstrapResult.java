package cn.cunmo.application.inventory.result;

import java.util.List;

/**
 * 库存首屏初始化结果。
 *
 * @param vaultId 魔方域主键
 * @param vaultName 魔方域名称
 * @param totalQuantity 库存总件数
 * @param itemLimitPerSpaceCategory 组合容量，无限时为空
 * @param unlimited 是否无限容量
 * @param spaces 空间及分类
 */
public record InventoryBootstrapResult(
        long vaultId,
        String vaultName,
        long totalQuantity,
        Integer itemLimitPerSpaceCategory,
        boolean unlimited,
        List<InventorySpaceResult> spaces) {

    /**
     * 复制空间列表为不可变集合。
     */
    public InventoryBootstrapResult {
        spaces = List.copyOf(spaces);
    }
}
