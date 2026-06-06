package cn.cunmo.domain.inventory.model.aggregate;

import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;

/**
 * 库存魔方域聚合根。
 */
public final class InventoryVault {
    private final VaultId id;
    private final Integer itemLimitPerSpaceCategory;

    /**
     * 创建库存魔方域。
     */
    private InventoryVault(
            VaultId id,
            Integer itemLimitPerSpaceCategory) {
        this.id = id;
        this.itemLimitPerSpaceCategory = itemLimitPerSpaceCategory;
    }

    /**
     * 从持久化状态还原魔方域。
     */
    public static InventoryVault reconstitute(
            VaultId id,
            Integer itemLimitPerSpaceCategory) {
        if (itemLimitPerSpaceCategory != null
                && itemLimitPerSpaceCategory <= 0) {
            throw new IllegalArgumentException("物品容量必须为正数或无限");
        }
        return new InventoryVault(id, itemLimitPerSpaceCategory);
    }

    /**
     * 校验空间分类组合是否仍可创建物品。
     */
    public void assertCanCreateItem(int currentItemCount) {
        if (itemLimitPerSpaceCategory != null
                && currentItemCount >= itemLimitPerSpaceCategory) {
            throw new DomainException(
                    "CATEGORY_CAPACITY_EXCEEDED",
                    "当前空间分类已达到物品容量上限");
        }
    }

    /**
     * 返回魔方域标识。
     */
    public VaultId id() {
        return id;
    }

    /**
     * 返回每个空间分类组合的物品上限，空值表示无限。
     */
    public Integer itemLimitPerSpaceCategory() {
        return itemLimitPerSpaceCategory;
    }

    /**
     * 判断当前魔方域是否为无限容量。
     */
    public boolean unlimited() {
        return itemLimitPerSpaceCategory == null;
    }
}
