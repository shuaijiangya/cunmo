package cn.cunmo.application.inventory.query;

import cn.cunmo.application.inventory.result.CursorPageResult;
import cn.cunmo.application.inventory.result.InventoryAnalyticsResult;
import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
import cn.cunmo.application.inventory.result.InventoryDeletionPreviewResult;
import cn.cunmo.application.inventory.result.InventoryItemResult;
import cn.cunmo.application.inventory.result.StockTransactionResult;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;

/**
 * 库存读模型查询端口。
 */
public interface InventoryQueryService {

    /**
     * 查询库存首屏数据。
     */
    InventoryBootstrapResult bootstrap(VaultId vaultId);

    /**
     * 游标分页查询物品。
     */
    CursorPageResult<InventoryItemResult> items(
            VaultId vaultId,
            Long spaceId,
            Long categoryId,
            String keyword,
            Long cursor,
            int size);

    /**
     * 游标分页查询透视统计。
     */
    CursorPageResult<InventoryAnalyticsResult> analytics(
            VaultId vaultId,
            String dimension,
            Long cursor,
            int size);

    /**
     * 游标分页查询库存流水。
     */
    CursorPageResult<StockTransactionResult> transactions(
            VaultId vaultId,
            Long cursor,
            int size);

    /** 查询物品删除影响预览。 */
    InventoryDeletionPreviewResult previewItemDeletion(
            VaultId vaultId,
            long itemId);

    /** 查询空间删除影响预览。 */
    InventoryDeletionPreviewResult previewSpaceDeletion(
            VaultId vaultId,
            long spaceId);

    /** 查询分类删除或解绑影响预览。 */
    InventoryDeletionPreviewResult previewCategoryDeletion(
            VaultId vaultId,
            long categoryId,
            Long spaceId);
}
