package cn.cunmo.application.inventory.service;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.enums.DeletionStrategy;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.repository.InventoryDeletionRepository;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存结构删除应用服务。
 */
public class InventoryDeletionApplicationService {
    private static final Logger log = LoggerFactory.getLogger(
            InventoryDeletionApplicationService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryDeletionRepository deletionRepository;

    /** 创建库存结构删除应用服务。 */
    public InventoryDeletionApplicationService(
            InventoryRepository inventoryRepository,
            InventoryDeletionRepository deletionRepository) {
        this.inventoryRepository = inventoryRepository;
        this.deletionRepository = deletionRepository;
    }

    /** 将单个物品迁移到目标空间分类。 */
    @Transactional
    public void moveItem(
            long userId,
            long itemId,
            long targetSpaceId,
            long targetCategoryId) {
        InventoryVault vault = inventoryRepository.lockVault(userId);
        InventoryItem item = inventoryRepository.requireItem(
                vault.id(),
                InventoryItemId.of(itemId));
        if (item.spaceId() == targetSpaceId
                && item.categoryId() == targetCategoryId) {
            throw new ApplicationException(
                    "SAME_SOURCE_AND_TARGET",
                    "目标空间和分类不能与当前位置相同");
        }
        log.info(
                "event=inventory_item_move stage=validated userId={} itemId={} targetSpaceId={} targetCategoryId={}",
                userId,
                itemId,
                targetSpaceId,
                targetCategoryId);
        deletionRepository.moveItem(
                vault.id(),
                item.id(),
                targetSpaceId,
                targetCategoryId,
                userId);
        log.info(
                "event=inventory_item_move stage=application_completed userId={} itemId={}",
                userId,
                itemId);
    }

    /** 清空库存并逻辑删除单个物品。 */
    @Transactional
    public void deleteItem(long userId, long itemId) {
        InventoryVault vault = inventoryRepository.lockVault(userId);
        log.info(
                "event=inventory_item_delete stage=validated userId={} itemId={}",
                userId,
                itemId);
        deletionRepository.deleteItem(
                vault.id(),
                InventoryItemId.of(itemId),
                userId);
        log.info(
                "event=inventory_item_delete stage=application_completed userId={} itemId={}",
                userId,
                itemId);
    }

    /** 按指定策略删除空间。 */
    @Transactional
    public void deleteSpace(
            long userId,
            long spaceId,
            DeletionStrategy strategy,
            Long targetSpaceId) {
        requireStrategy(strategy);
        if (strategy == DeletionStrategy.MOVE && targetSpaceId == null) {
            throw new ApplicationException(
                    "TARGET_SPACE_NOT_FOUND",
                    "请选择目标空间");
        }
        if (targetSpaceId != null && targetSpaceId == spaceId) {
            throw new ApplicationException(
                    "SAME_SOURCE_AND_TARGET",
                    "目标空间不能与待删除空间相同");
        }
        InventoryVault vault = inventoryRepository.lockVault(userId);
        log.info(
                "event=inventory_space_delete stage=validated userId={} spaceId={} strategy={} targetSpaceId={}",
                userId,
                spaceId,
                strategy,
                targetSpaceId);
        deletionRepository.deleteSpace(
                vault.id(),
                spaceId,
                strategy,
                targetSpaceId,
                userId);
        log.info(
                "event=inventory_space_delete stage=application_completed userId={} spaceId={} strategy={}",
                userId,
                spaceId,
                strategy);
    }

    /** 按指定策略解除分类与当前空间的绑定。 */
    @Transactional
    public void unbindCategory(
            long userId,
            long spaceId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId) {
        requireCategoryTarget(strategy, categoryId, targetCategoryId);
        InventoryVault vault = inventoryRepository.lockVault(userId);
        log.info(
                "event=inventory_category_unbind stage=validated userId={} spaceId={} categoryId={} strategy={} targetCategoryId={}",
                userId,
                spaceId,
                categoryId,
                strategy,
                targetCategoryId);
        deletionRepository.unbindCategory(
                vault.id(),
                spaceId,
                categoryId,
                strategy,
                targetCategoryId,
                userId);
        log.info(
                "event=inventory_category_unbind stage=application_completed userId={} spaceId={} categoryId={}",
                userId,
                spaceId,
                categoryId);
    }

    /** 按指定策略全局删除分类。 */
    @Transactional
    public void deleteCategory(
            long userId,
            long categoryId,
            DeletionStrategy strategy,
            Long targetCategoryId) {
        requireCategoryTarget(strategy, categoryId, targetCategoryId);
        InventoryVault vault = inventoryRepository.lockVault(userId);
        log.info(
                "event=inventory_category_delete stage=validated userId={} categoryId={} strategy={} targetCategoryId={}",
                userId,
                categoryId,
                strategy,
                targetCategoryId);
        deletionRepository.deleteCategory(
                vault.id(),
                categoryId,
                strategy,
                targetCategoryId,
                userId);
        log.info(
                "event=inventory_category_delete stage=application_completed userId={} categoryId={}",
                userId,
                categoryId);
    }

    /** 校验删除策略不能为空。 */
    private void requireStrategy(DeletionStrategy strategy) {
        if (strategy == null) {
            throw new ApplicationException(
                    "INVALID_DELETE_STRATEGY",
                    "请选择删除策略");
        }
    }

    /** 校验分类删除策略和迁移目标。 */
    private void requireCategoryTarget(
            DeletionStrategy strategy,
            long categoryId,
            Long targetCategoryId) {
        requireStrategy(strategy);
        if (strategy == DeletionStrategy.MOVE
                && targetCategoryId == null) {
            throw new ApplicationException(
                    "TARGET_CATEGORY_NOT_FOUND",
                    "请选择目标分类");
        }
        if (targetCategoryId != null && targetCategoryId == categoryId) {
            throw new ApplicationException(
                    "SAME_SOURCE_AND_TARGET",
                    "目标分类不能与待删除分类相同");
        }
    }
}
