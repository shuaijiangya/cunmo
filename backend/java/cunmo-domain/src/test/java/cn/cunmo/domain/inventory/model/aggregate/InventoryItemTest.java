package cn.cunmo.domain.inventory.model.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.inventory.model.enums.StockTransactionType;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import org.junit.jupiter.api.Test;

/**
 * 库存物品聚合规则测试。
 */
class InventoryItemTest {

    /**
     * 验证库存不能调整为负数。
     */
    @Test
    void rejectsNegativeStock() {
        InventoryItem item = item(1, 1);

        DomainException error = assertThrows(
                DomainException.class,
                () -> item.adjust(-2));

        assertEquals("INSUFFICIENT_STOCK", error.code());
    }

    /**
     * 验证调整后达到预警线时产生预警类型结果。
     */
    @Test
    void warnsWhenQuantityReachesMinimum() {
        InventoryItem item = item(2, 1);

        StockAdjustment adjustment = item.adjust(-1);

        assertEquals(1, adjustment.quantityAfter());
        assertEquals(StockTransactionType.WARN, adjustment.type());
    }

    /**
     * 验证有限容量在达到上限后拒绝继续创建物品。
     */
    @Test
    void rejectsCapacityExceeded() {
        InventoryVault vault = InventoryVault.reconstitute(
                VaultId.of(1L),
                20);

        DomainException error = assertThrows(
                DomainException.class,
                () -> vault.assertCanCreateItem(20));

        assertEquals("CATEGORY_CAPACITY_EXCEEDED", error.code());
    }

    /**
     * 验证无限容量不限制空间分类组合的物品数量。
     */
    @Test
    void acceptsUnlimitedCapacity() {
        InventoryVault vault = InventoryVault.reconstitute(
                VaultId.of(1L),
                null);

        vault.assertCanCreateItem(Integer.MAX_VALUE);
    }

    /**
     * 验证迁移物品会更新空间、分类并将精准位置重置为待整理。
     */
    @Test
    void movesItemAndResetsDetailLocation() {
        InventoryItem item = item(3, 1);

        InventoryMovement movement = item.moveTo(11L, 21L);

        assertEquals(10L, movement.sourceSpaceId());
        assertEquals(20L, movement.sourceCategoryId());
        assertEquals("默认位置", movement.sourceDetailLocation());
        assertEquals(11L, item.spaceId());
        assertEquals(21L, item.categoryId());
        assertEquals("待整理", item.detailLocation());
        assertEquals(3, item.quantity());
    }

    /**
     * 验证清空删除会记录原库存并把当前库存归零。
     */
    @Test
    void clearsQuantityForDeletion() {
        InventoryItem item = item(3, 1);

        InventoryDeletion deletion = item.clearForDeletion();

        assertEquals(3, deletion.quantityBefore());
        assertEquals(0, deletion.quantityAfter());
        assertEquals(-3, deletion.delta());
        assertEquals(0, item.quantity());
    }

    /**
     * 创建测试库存物品。
     */
    private InventoryItem item(int quantity, int minimumQuantity) {
        return InventoryItem.reconstitute(
                InventoryItemId.of(1L),
                VaultId.of(1L),
                10L,
                20L,
                "测试物品",
                "常规",
                "默认位置",
                quantity,
                minimumQuantity,
                0);
    }
}
