package cn.cunmo.domain.inventory.repository;

import cn.cunmo.domain.inventory.model.enums.DeletionStrategy;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;

/**
 * 库存删除与迁移持久化端口。
 */
public interface InventoryDeletionRepository {

    /** 清空并逻辑删除单个物品。 */
    void deleteItem(
            VaultId vaultId,
            InventoryItemId itemId,
            long operatorUserId);

    /** 按策略删除空间及其受影响数据。 */
    void deleteSpace(
            VaultId vaultId,
            long spaceId,
            DeletionStrategy strategy,
            Long targetSpaceId,
            long operatorUserId);

    /** 按策略解除分类与当前空间的绑定。 */
    void unbindCategory(
            VaultId vaultId,
            long spaceId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId,
            long operatorUserId);

    /** 按策略全局删除分类。 */
    void deleteCategory(
            VaultId vaultId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId,
            long operatorUserId);
}
