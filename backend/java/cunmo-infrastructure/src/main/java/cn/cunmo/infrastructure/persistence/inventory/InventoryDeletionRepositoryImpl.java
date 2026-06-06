package cn.cunmo.infrastructure.persistence.inventory;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.inventory.model.aggregate.InventoryDeletion;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryMovement;
import cn.cunmo.domain.inventory.model.enums.DeletionStrategy;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import cn.cunmo.domain.inventory.repository.InventoryDeletionRepository;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryCategoryDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryItemDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventorySpaceDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryVaultDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.StockTransactionDO;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryCategoryMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryDeletionMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryItemMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventorySpaceMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryVaultMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.SpaceCategoryMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.StockTransactionMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * MySQL 库存结构删除与迁移仓储实现。
 */
@Repository
public class InventoryDeletionRepositoryImpl
        implements InventoryDeletionRepository {
    private static final Logger log = LoggerFactory.getLogger(
            InventoryDeletionRepositoryImpl.class);

    private final InventoryVaultMapper vaultMapper;
    private final InventorySpaceMapper spaceMapper;
    private final InventoryCategoryMapper categoryMapper;
    private final SpaceCategoryMapper bindingMapper;
    private final InventoryItemMapper itemMapper;
    private final InventoryDeletionMapper deletionMapper;
    private final StockTransactionMapper transactionMapper;

    /** 创建库存结构删除仓储。 */
    public InventoryDeletionRepositoryImpl(
            InventoryVaultMapper vaultMapper,
            InventorySpaceMapper spaceMapper,
            InventoryCategoryMapper categoryMapper,
            SpaceCategoryMapper bindingMapper,
            InventoryItemMapper itemMapper,
            InventoryDeletionMapper deletionMapper,
            StockTransactionMapper transactionMapper) {
        this.vaultMapper = vaultMapper;
        this.spaceMapper = spaceMapper;
        this.categoryMapper = categoryMapper;
        this.bindingMapper = bindingMapper;
        this.itemMapper = itemMapper;
        this.deletionMapper = deletionMapper;
        this.transactionMapper = transactionMapper;
    }

    /** 清空并逻辑删除单个物品。 */
    @Override
    public void deleteItem(
            VaultId vaultId,
            InventoryItemId itemId,
            long operatorUserId) {
        InventoryItemDO item = requireItemForUpdate(vaultId, itemId.value());
        deleteOne(vaultId, item, operatorUserId);
        log.info(
                "event=inventory_item_delete stage=persistence_committed vaultId={} itemId={}",
                vaultId.value(),
                itemId.value());
    }

    /** 按策略删除空间及其受影响数据。 */
    @Override
    public void deleteSpace(
            VaultId vaultId,
            long spaceId,
            DeletionStrategy strategy,
            Long targetSpaceId,
            long operatorUserId) {
        requireSpace(vaultId, spaceId);
        List<Long> sourceCategoryIds =
                deletionMapper.selectCategoryIdsBySpace(
                        vaultId.value(),
                        spaceId);
        List<InventoryItemDO> items =
                deletionMapper.selectItemsBySpaceForUpdate(
                        vaultId.value(),
                        spaceId);
        if (strategy == DeletionStrategy.MOVE) {
            InventorySpaceDO targetSpace = requireSpace(
                    vaultId,
                    requireTarget(targetSpaceId, "TARGET_SPACE_NOT_FOUND"));
            Map<Long, Integer> incoming = countByCategory(items);
            for (Map.Entry<Long, Integer> entry : incoming.entrySet()) {
                InventoryCategoryDO category = requireCategory(
                        vaultId,
                        entry.getKey());
                deletionMapper.ensureBinding(
                        vaultId.value(),
                        targetSpace.getId(),
                        category.getId());
                assertCapacity(
                        vaultId,
                        targetSpace.getId(),
                        category.getId(),
                        entry.getValue());
            }
            for (InventoryItemDO item : items) {
                moveOne(
                        vaultId,
                        item,
                        targetSpace,
                        requireCategory(vaultId, item.getCategoryId()),
                        operatorUserId);
            }
        } else {
            for (InventoryItemDO item : items) {
                deleteOne(vaultId, item, operatorUserId);
            }
        }
        deletionMapper.deleteBindingsBySpace(vaultId.value(), spaceId);
        if (deletionMapper.softDeleteSpace(vaultId.value(), spaceId) != 1) {
            throw conflict();
        }
        cleanupOrphanCategories(vaultId, sourceCategoryIds);
        log.info(
                "event=inventory_space_delete stage=persistence_committed vaultId={} spaceId={} strategy={} affectedItems={}",
                vaultId.value(),
                spaceId,
                strategy,
                items.size());
    }

    /** 按策略解除分类与当前空间的绑定。 */
    @Override
    public void unbindCategory(
            VaultId vaultId,
            long spaceId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId,
            long operatorUserId) {
        InventorySpaceDO space = requireSpace(vaultId, spaceId);
        requireCategory(vaultId, categoryId);
        requireBinding(vaultId, spaceId, categoryId);
        List<InventoryItemDO> items =
                deletionMapper.selectItemsByBindingForUpdate(
                        vaultId.value(),
                        spaceId,
                        categoryId);
        if (strategy == DeletionStrategy.MOVE) {
            long targetId = requireTarget(
                    targetCategoryId,
                    "TARGET_CATEGORY_NOT_FOUND");
            InventoryCategoryDO targetCategory = requireCategory(
                    vaultId,
                    targetId);
            requireBinding(vaultId, spaceId, targetId);
            assertCapacity(vaultId, spaceId, targetId, items.size());
            for (InventoryItemDO item : items) {
                moveOne(
                        vaultId,
                        item,
                        space,
                        targetCategory,
                        operatorUserId);
            }
        } else {
            for (InventoryItemDO item : items) {
                deleteOne(vaultId, item, operatorUserId);
            }
        }
        bindingMapper.deleteBinding(
                vaultId.value(),
                categoryId,
                spaceId);
        log.info(
                "event=inventory_category_unbind stage=persistence_committed vaultId={} spaceId={} categoryId={} strategy={} affectedItems={}",
                vaultId.value(),
                spaceId,
                categoryId,
                strategy,
                items.size());
    }

    /** 按策略全局删除分类。 */
    @Override
    public void deleteCategory(
            VaultId vaultId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId,
            long operatorUserId) {
        requireCategory(vaultId, categoryId);
        List<InventoryItemDO> items =
                deletionMapper.selectItemsByCategoryForUpdate(
                        vaultId.value(),
                        categoryId);
        if (strategy == DeletionStrategy.MOVE) {
            long targetId = requireTarget(
                    targetCategoryId,
                    "TARGET_CATEGORY_NOT_FOUND");
            InventoryCategoryDO targetCategory = requireCategory(
                    vaultId,
                    targetId);
            Map<Long, Integer> incoming = countBySpace(items);
            for (Map.Entry<Long, Integer> entry : incoming.entrySet()) {
                InventorySpaceDO space = requireSpace(vaultId, entry.getKey());
                deletionMapper.ensureBinding(
                        vaultId.value(),
                        space.getId(),
                        targetId);
                assertCapacity(
                        vaultId,
                        space.getId(),
                        targetId,
                        entry.getValue());
            }
            for (InventoryItemDO item : items) {
                moveOne(
                        vaultId,
                        item,
                        requireSpace(vaultId, item.getSpaceId()),
                        targetCategory,
                        operatorUserId);
            }
        } else {
            for (InventoryItemDO item : items) {
                deleteOne(vaultId, item, operatorUserId);
            }
        }
        deletionMapper.deleteBindingsByCategory(
                vaultId.value(),
                categoryId);
        if (deletionMapper.softDeleteCategory(
                vaultId.value(),
                categoryId) != 1) {
            throw conflict();
        }
        log.info(
                "event=inventory_category_delete stage=persistence_committed vaultId={} categoryId={} strategy={} affectedItems={}",
                vaultId.value(),
                categoryId,
                strategy,
                items.size());
    }

    /** 迁移单个已锁定物品并写入 MOVE 流水。 */
    private void moveOne(
            VaultId vaultId,
            InventoryItemDO item,
            InventorySpaceDO targetSpace,
            InventoryCategoryDO targetCategory,
            long operatorUserId) {
        InventorySpaceDO sourceSpace = requireSpace(
                vaultId,
                item.getSpaceId());
        InventoryCategoryDO sourceCategory = requireCategory(
                vaultId,
                item.getCategoryId());
        InventoryItem aggregate = toDomain(item);
        InventoryMovement movement = aggregate.moveTo(
                targetSpace.getId(),
                targetCategory.getId());
        if (deletionMapper.moveItem(
                vaultId.value(),
                item.getId(),
                targetSpace.getId(),
                targetCategory.getId(),
                movement.targetDetailLocation(),
                item.getVersion()) != 1) {
            throw conflict();
        }
        writeTransaction(
                item,
                "MOVE",
                0,
                item.getQuantity(),
                item.getQuantity(),
                "物品已迁移，精准位置重置为待整理",
                sourceSpace,
                sourceCategory,
                targetSpace,
                targetCategory,
                operatorUserId);
    }

    /** 清空并删除单个已锁定物品并写入 DELETE 流水。 */
    private void deleteOne(
            VaultId vaultId,
            InventoryItemDO item,
            long operatorUserId) {
        InventorySpaceDO sourceSpace = requireSpace(
                vaultId,
                item.getSpaceId());
        InventoryCategoryDO sourceCategory = requireCategory(
                vaultId,
                item.getCategoryId());
        InventoryDeletion deletion = toDomain(item).clearForDeletion();
        if (deletionMapper.softDeleteItem(
                vaultId.value(),
                item.getId(),
                item.getVersion()) != 1) {
            throw conflict();
        }
        writeTransaction(
                item,
                "DELETE",
                deletion.delta(),
                deletion.quantityBefore(),
                deletion.quantityAfter(),
                "清空库存并删除物品",
                sourceSpace,
                sourceCategory,
                null,
                null,
                operatorUserId);
    }

    /** 写入迁移或删除流水快照。 */
    private void writeTransaction(
            InventoryItemDO item,
            String type,
            int delta,
            int before,
            int after,
            String description,
            InventorySpaceDO sourceSpace,
            InventoryCategoryDO sourceCategory,
            InventorySpaceDO targetSpace,
            InventoryCategoryDO targetCategory,
            long operatorUserId) {
        StockTransactionDO transaction = new StockTransactionDO();
        transaction.setVaultId(item.getVaultId());
        transaction.setItemId(item.getId());
        transaction.setTransactionType(type);
        transaction.setQuantityDelta(delta);
        transaction.setQuantityBefore(before);
        transaction.setQuantityAfter(after);
        transaction.setDescription(description);
        transaction.setItemNameSnapshot(item.getItemName());
        transaction.setSpaceSnapshot(sourceSpace.getSpaceName());
        transaction.setCategorySnapshot(sourceCategory.getCategoryName());
        transaction.setDetailLocationSnapshot(item.getDetailLocation());
        transaction.setSourceSpaceSnapshot(sourceSpace.getSpaceName());
        transaction.setSourceCategorySnapshot(sourceCategory.getCategoryName());
        transaction.setSourceDetailLocationSnapshot(
                item.getDetailLocation());
        transaction.setTargetSpaceSnapshot(
                targetSpace == null ? null : targetSpace.getSpaceName());
        transaction.setTargetCategorySnapshot(
                targetCategory == null
                        ? null
                        : targetCategory.getCategoryName());
        transaction.setTargetDetailLocationSnapshot(
                targetSpace == null ? null : "待整理");
        transaction.setOperatorUserId(operatorUserId);
        transaction.setOccurredAt(Instant.now());
        transactionMapper.insert(transaction);
    }

    /** 校验目标空间分类容量。 */
    private void assertCapacity(
            VaultId vaultId,
            long spaceId,
            long categoryId,
            int incomingCount) {
        InventoryVaultDO vault = vaultMapper.selectById(vaultId.value());
        Integer limit = vault == null
                ? null
                : vault.getItemLimitPerSpaceCategory();
        if (limit == null) {
            return;
        }
        int current = itemMapper.countActiveItems(
                vaultId.value(),
                spaceId,
                categoryId);
        if ((long) current + incomingCount > limit) {
            throw new ApplicationException(
                    "TARGET_CAPACITY_EXCEEDED",
                    "目标空间分类容量不足");
        }
    }

    /** 校验空间属于当前魔方域。 */
    private InventorySpaceDO requireSpace(
            VaultId vaultId,
            long spaceId) {
        InventorySpaceDO space = spaceMapper.selectById(spaceId);
        if (space == null
                || !vaultId.value().equals(space.getVaultId())
                || !Integer.valueOf(1).equals(space.getStatus())
                || !Integer.valueOf(0).equals(space.getDeleted())) {
            throw new ApplicationException(
                    "SPACE_NOT_FOUND",
                    "库存空间不存在");
        }
        return space;
    }

    /** 校验分类属于当前魔方域。 */
    private InventoryCategoryDO requireCategory(
            VaultId vaultId,
            long categoryId) {
        InventoryCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null
                || !vaultId.value().equals(category.getVaultId())
                || !Integer.valueOf(1).equals(category.getStatus())
                || !Integer.valueOf(0).equals(category.getDeleted())) {
            throw new ApplicationException(
                    "CATEGORY_NOT_FOUND",
                    "库存分类不存在");
        }
        return category;
    }

    /** 锁定并读取有效物品。 */
    private InventoryItemDO requireItemForUpdate(
            VaultId vaultId,
            long itemId) {
        InventoryItemDO item = deletionMapper.selectItemForUpdate(
                vaultId.value(),
                itemId);
        if (item == null) {
            throw new ApplicationException(
                    "ITEM_NOT_FOUND",
                    "库存物品不存在");
        }
        return item;
    }

    /** 校验空间分类绑定存在。 */
    private void requireBinding(
            VaultId vaultId,
            long spaceId,
            long categoryId) {
        if (bindingMapper.countBinding(
                vaultId.value(),
                spaceId,
                categoryId) == 0) {
            throw new ApplicationException(
                    "TARGET_CATEGORY_NOT_BOUND",
                    "目标分类未绑定到目标空间");
        }
    }

    /** 读取必填迁移目标。 */
    private long requireTarget(Long targetId, String code) {
        if (targetId == null || targetId <= 0) {
            throw new ApplicationException(code, "请选择有效迁移目标");
        }
        return targetId;
    }

    /** 按分类统计待迁移物品种类数。 */
    private Map<Long, Integer> countByCategory(
            List<InventoryItemDO> items) {
        Map<Long, Integer> counts = new HashMap<>();
        for (InventoryItemDO item : items) {
            counts.merge(item.getCategoryId(), 1, Integer::sum);
        }
        return counts;
    }

    /** 按空间统计待迁移物品种类数。 */
    private Map<Long, Integer> countBySpace(List<InventoryItemDO> items) {
        Map<Long, Integer> counts = new HashMap<>();
        for (InventoryItemDO item : items) {
            counts.merge(item.getSpaceId(), 1, Integer::sum);
        }
        return counts;
    }

    /** 删除空间后清理不再被任何空间引用的空分类。 */
    private void cleanupOrphanCategories(
            VaultId vaultId,
            List<Long> categoryIds) {
        for (Long categoryId : categoryIds) {
            if (deletionMapper.countCategoryBindings(
                    vaultId.value(),
                    categoryId) == 0
                    && deletionMapper.countCategoryItems(
                            vaultId.value(),
                            categoryId) == 0) {
                deletionMapper.softDeleteCategory(
                        vaultId.value(),
                        categoryId);
            }
        }
    }

    /** 创建统一库存并发冲突异常。 */
    private ApplicationException conflict() {
        return new ApplicationException(
                "INVENTORY_CONFLICT",
                "库存结构已变化，请刷新后重试");
    }

    /** 将库存物品数据库对象还原为领域聚合。 */
    private InventoryItem toDomain(InventoryItemDO item) {
        return InventoryItem.reconstitute(
                InventoryItemId.of(item.getId()),
                VaultId.of(item.getVaultId()),
                item.getSpaceId(),
                item.getCategoryId(),
                item.getItemName(),
                item.getSmallCategory(),
                item.getDetailLocation(),
                item.getQuantity(),
                item.getMinimumQuantity(),
                item.getVersion());
    }
}
