package cn.cunmo.domain.inventory.repository;

import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.aggregate.StockAdjustment;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import java.util.List;

/**
 * 库存聚合持久化端口。
 */
public interface InventoryRepository {

    /**
     * 获取用户魔方域，不存在时创建默认域和结构。
     */
    InventoryVault getOrCreateDefaultVault(long userId);

    /**
     * 按当前用户读取魔方域。
     */
    InventoryVault requireVault(long userId);

    /**
     * 锁定并读取用户魔方域，用于串行执行容量校验。
     */
    InventoryVault lockVault(long userId);

    /**
     * 判断空间分类绑定是否有效。
     */
    boolean bindingExists(VaultId vaultId, long spaceId, long categoryId);

    /**
     * 统计空间分类组合内有效物品数量。
     */
    int countItems(VaultId vaultId, long spaceId, long categoryId);

    default int countSpaces(VaultId vaultId) {
        return 0;
    }

    default int countCategories(VaultId vaultId, long spaceId) {
        return 0;
    }

    default List<Long> findCategorySpaceIds(
            VaultId vaultId,
            long categoryId) {
        return List.of();
    }

    /**
     * 保存新物品并写入初始流水。
     */
    InventoryItem createItem(InventoryItem item, long operatorUserId);

    /**
     * 按魔方域读取物品。
     */
    InventoryItem requireItem(
            VaultId vaultId,
            InventoryItemId itemId);

    /**
     * 使用乐观锁更新数量并写入流水。
     */
    InventoryItem saveAdjustment(
            InventoryItem item,
            StockAdjustment adjustment,
            long operatorUserId,
            String description);

    /**
     * 创建空间并返回主键。
     */
    long createSpace(VaultId vaultId, String name);

    /**
     * 创建分类并绑定多个空间。
     */
    long createCategory(
            VaultId vaultId,
            String name,
            List<Long> spaceIds);

    /**
     * 更新分类空间绑定关系。
     */
    void updateCategorySpaces(
            VaultId vaultId,
            long categoryId,
            List<Long> spaceIds);
}
