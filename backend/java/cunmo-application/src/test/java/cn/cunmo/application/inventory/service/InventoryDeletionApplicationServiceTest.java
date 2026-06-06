package cn.cunmo.application.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.aggregate.StockAdjustment;
import cn.cunmo.domain.inventory.model.enums.DeletionStrategy;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import cn.cunmo.domain.inventory.repository.InventoryDeletionRepository;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 库存删除应用服务测试。
 */
class InventoryDeletionApplicationServiceTest {

    /**
     * 验证空间迁移策略必须提供目标空间。
     */
    @Test
    void requiresTargetForMovingSpace() {
        InventoryDeletionApplicationService service =
                new InventoryDeletionApplicationService(
                        new FakeInventoryRepository(),
                        new RecordingDeletionRepository());

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> service.deleteSpace(
                        7L,
                        10L,
                        DeletionStrategy.MOVE,
                        null));

        assertEquals("TARGET_SPACE_NOT_FOUND", error.code());
    }

    /**
     * 验证物品删除命令委托给删除仓储。
     */
    @Test
    void delegatesItemDeletion() {
        RecordingDeletionRepository deletionRepository =
                new RecordingDeletionRepository();
        InventoryDeletionApplicationService service =
                new InventoryDeletionApplicationService(
                        new FakeInventoryRepository(),
                        deletionRepository);

        service.deleteItem(7L, 1L);

        assertEquals(1L, deletionRepository.deletedItemId);
    }

    /**
     * 测试用库存仓储。
     */
    private static final class FakeInventoryRepository
            implements InventoryRepository {
        /**
         * 返回测试魔方域。
         */
        @Override
        public InventoryVault getOrCreateDefaultVault(long userId) {
            return vault();
        }

        /**
         * 返回测试魔方域。
         */
        @Override
        public InventoryVault requireVault(long userId) {
            return vault();
        }

        /**
         * 返回测试魔方域。
         */
        @Override
        public InventoryVault lockVault(long userId) {
            return vault();
        }

        /**
         * 返回有效绑定。
         */
        @Override
        public boolean bindingExists(
                VaultId vaultId,
                long spaceId,
                long categoryId) {
            return true;
        }

        /**
         * 返回测试物品数量。
         */
        @Override
        public int countItems(
                VaultId vaultId,
                long spaceId,
                long categoryId) {
            return 0;
        }

        /**
         * 返回传入物品。
         */
        @Override
        public InventoryItem createItem(
                InventoryItem item,
                long operatorUserId) {
            return item;
        }

        /**
         * 返回测试物品。
         */
        @Override
        public InventoryItem requireItem(
                VaultId vaultId,
                InventoryItemId itemId) {
            return InventoryItem.reconstitute(
                    itemId,
                    vaultId,
                    10L,
                    20L,
                    "测试物品",
                    "常规",
                    "默认位置",
                    3,
                    1,
                    0);
        }

        /**
         * 返回调整后的物品。
         */
        @Override
        public InventoryItem saveAdjustment(
                InventoryItem item,
                StockAdjustment adjustment,
                long operatorUserId,
                String description) {
            return item;
        }

        /**
         * 返回测试空间主键。
         */
        @Override
        public long createSpace(VaultId vaultId, String name) {
            return 1L;
        }

        /**
         * 返回测试分类主键。
         */
        @Override
        public long createCategory(
                VaultId vaultId,
                String name,
                List<Long> spaceIds) {
            return 1L;
        }

        /**
         * 忽略测试分类绑定更新。
         */
        @Override
        public void updateCategorySpaces(
                VaultId vaultId,
                long categoryId,
                List<Long> spaceIds) {
        }

        /**
         * 创建测试魔方域。
         */
        private InventoryVault vault() {
            return InventoryVault.reconstitute(VaultId.of(1L), 20);
        }
    }

    /**
     * 记录删除调用的测试仓储。
     */
    private static final class RecordingDeletionRepository
            implements InventoryDeletionRepository {
        private long deletedItemId;

        /**
         * 记录物品删除。
         */
        @Override
        public void deleteItem(
                VaultId vaultId,
                InventoryItemId itemId,
                long operatorUserId) {
            deletedItemId = itemId.value();
        }

        /**
         * 记录空间删除。
         */
        @Override
        public void deleteSpace(
                VaultId vaultId,
                long spaceId,
                DeletionStrategy strategy,
                Long targetSpaceId,
                long operatorUserId) {
        }

        /**
         * 记录分类解绑。
         */
        @Override
        public void unbindCategory(
                VaultId vaultId,
                long spaceId,
                long categoryId,
                DeletionStrategy strategy,
                Long targetCategoryId,
                long operatorUserId) {
        }

        /**
         * 记录分类全局删除。
         */
        @Override
        public void deleteCategory(
                VaultId vaultId,
                long categoryId,
                DeletionStrategy strategy,
                Long targetCategoryId,
                long operatorUserId) {
        }
    }
}
