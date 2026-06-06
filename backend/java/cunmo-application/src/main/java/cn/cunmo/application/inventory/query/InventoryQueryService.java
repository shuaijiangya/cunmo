package cn.cunmo.application.inventory.query;

import cn.cunmo.application.inventory.result.CursorPageResult;
import cn.cunmo.application.inventory.result.InventoryAnalyticsResult;
import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
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
}
