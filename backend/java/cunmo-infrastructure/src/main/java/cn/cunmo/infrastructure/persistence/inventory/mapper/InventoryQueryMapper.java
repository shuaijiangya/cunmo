package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.application.inventory.result.InventoryItemResult;
import cn.cunmo.application.inventory.result.InventoryDeletionPreviewResult;
import cn.cunmo.application.inventory.result.StockTransactionResult;
import cn.cunmo.infrastructure.persistence.inventory.query.InventoryAnalyticsRow;
import cn.cunmo.infrastructure.persistence.inventory.query.InventoryStructureRow;
import cn.cunmo.infrastructure.persistence.inventory.query.InventoryVaultSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存读模型查询 Mapper。
 */
public interface InventoryQueryMapper {

    /**
     * 查询魔方域汇总数据。
     */
    @Select("""
            SELECT vault.id AS vaultId,
                   vault.vault_name AS vaultName,
                   vault.item_limit_per_space_category
                     AS itemLimitPerSpaceCategory,
                   COALESCE(SUM(item.quantity), 0) AS totalQuantity
            FROM inv_vault vault
            LEFT JOIN inv_item item
              ON item.vault_id = vault.id
             AND item.status = 1 AND item.deleted = 0
            WHERE vault.id = #{vaultId}
              AND vault.status = 1 AND vault.deleted = 0
            GROUP BY vault.id, vault.vault_name,
                     vault.item_limit_per_space_category
            """)
    InventoryVaultSummaryRow selectVaultSummary(
            @Param("vaultId") long vaultId);

    /**
     * 查询空间分类结构及当前物品数。
     */
    @Select("""
            SELECT space.id AS spaceId,
                   space.space_code AS spaceCode,
                   space.space_name AS spaceName,
                   category.id AS categoryId,
                   category.category_code AS categoryCode,
                   category.category_name AS categoryName,
                   COUNT(item.id) AS itemCount
            FROM inv_space space
            LEFT JOIN inv_space_category binding
              ON binding.vault_id = space.vault_id
             AND binding.space_id = space.id
            LEFT JOIN inv_category category
              ON category.id = binding.category_id
             AND category.vault_id = space.vault_id
             AND category.status = 1 AND category.deleted = 0
            LEFT JOIN inv_item item
              ON item.vault_id = space.vault_id
             AND item.space_id = space.id
             AND item.category_id = category.id
             AND item.status = 1 AND item.deleted = 0
            WHERE space.vault_id = #{vaultId}
              AND space.status = 1 AND space.deleted = 0
            GROUP BY space.id, space.space_code, space.space_name,
                     space.sort_order, category.id,
                     category.category_code, category.category_name,
                     binding.sort_order
            ORDER BY space.sort_order, space.id,
                     binding.sort_order, category.id
            """)
    List<InventoryStructureRow> selectStructure(
            @Param("vaultId") long vaultId);

    /**
     * 游标分页查询物品。
     */
    @Select("""
            <script>
            SELECT item.id, item.item_name AS name,
                   space.id AS spaceId, space.space_code AS spaceCode,
                   space.space_name AS spaceName,
                   category.id AS categoryId,
                   category.category_code AS categoryCode,
                   category.category_name AS categoryName,
                   item.small_category AS smallCategory,
                   item.detail_location AS detailLocation,
                   item.quantity,
                   item.minimum_quantity AS minimumQuantity,
                   item.version
            FROM inv_item item
            JOIN inv_space space ON space.id = item.space_id
            JOIN inv_category category ON category.id = item.category_id
            WHERE item.vault_id = #{vaultId}
              AND item.status = 1 AND item.deleted = 0
            <if test="spaceId != null">
              AND item.space_id = #{spaceId}
            </if>
            <if test="categoryId != null">
              AND item.category_id = #{categoryId}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (item.item_name LIKE CONCAT('%', #{keyword}, '%')
                   OR item.small_category LIKE CONCAT('%', #{keyword}, '%')
                   OR item.detail_location LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="cursor != null">
              AND item.id &lt; #{cursor}
            </if>
            ORDER BY item.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<InventoryItemResult> selectItems(
            @Param("vaultId") long vaultId,
            @Param("spaceId") Long spaceId,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /**
     * 按空间查询透视统计。
     */
    @Select("""
            <script>
            SELECT space.id, space.space_code AS code,
                   space.space_name AS name,
                   COUNT(item.id) AS itemCount,
                   COALESCE(SUM(item.quantity), 0) AS quantity
            FROM inv_space space
            LEFT JOIN inv_item item
              ON item.space_id = space.id
             AND item.vault_id = space.vault_id
             AND item.status = 1 AND item.deleted = 0
            WHERE space.vault_id = #{vaultId}
              AND space.status = 1 AND space.deleted = 0
            <if test="cursor != null">
              AND space.id &gt; #{cursor}
            </if>
            GROUP BY space.id, space.space_code, space.space_name,
                     space.sort_order
            ORDER BY space.id
            LIMIT #{limit}
            </script>
            """)
    List<InventoryAnalyticsRow> selectSpaceAnalytics(
            @Param("vaultId") long vaultId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /**
     * 按分类查询透视统计。
     */
    @Select("""
            <script>
            SELECT category.id, category.category_code AS code,
                   category.category_name AS name,
                   COUNT(item.id) AS itemCount,
                   COALESCE(SUM(item.quantity), 0) AS quantity
            FROM inv_category category
            LEFT JOIN inv_item item
              ON item.category_id = category.id
             AND item.vault_id = category.vault_id
             AND item.status = 1 AND item.deleted = 0
            WHERE category.vault_id = #{vaultId}
              AND category.status = 1 AND category.deleted = 0
            <if test="cursor != null">
              AND category.id &gt; #{cursor}
            </if>
            GROUP BY category.id, category.category_code,
                     category.category_name, category.sort_order
            ORDER BY category.id
            LIMIT #{limit}
            </script>
            """)
    List<InventoryAnalyticsRow> selectCategoryAnalytics(
            @Param("vaultId") long vaultId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /**
     * 游标分页查询库存流水。
     */
    @Select("""
            <script>
            SELECT id, item_id AS itemId,
                   item_name_snapshot AS itemName,
                   transaction_type AS type,
                   quantity_delta AS delta,
                   quantity_before AS quantityBefore,
                   quantity_after AS quantityAfter,
                   description,
                   COALESCE(target_space_snapshot, space_snapshot)
                     AS spaceName,
                   COALESCE(target_category_snapshot, category_snapshot)
                     AS categoryName,
                   COALESCE(target_detail_location_snapshot,
                            detail_location_snapshot) AS detailLocation,
                   occurred_at AS occurredAt
            FROM inv_stock_transaction
            WHERE vault_id = #{vaultId}
            <if test="cursor != null">
              AND id &lt; #{cursor}
            </if>
            ORDER BY id DESC
            LIMIT #{limit}
            </script>
            """)
    List<StockTransactionResult> selectTransactions(
            @Param("vaultId") long vaultId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /** 查询物品删除影响。 */
    @Select("""
            SELECT item.id AS targetId,
                   item.item_name AS targetName,
                   1 AS affectedItemCount,
                   item.quantity AS affectedQuantity,
                   0 AS bindingCount
            FROM inv_item item
            WHERE item.vault_id = #{vaultId} AND item.id = #{itemId}
              AND item.status = 1 AND item.deleted = 0
            """)
    InventoryDeletionPreviewResult selectItemDeletionPreview(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId);

    /** 查询空间删除影响。 */
    @Select("""
            SELECT space.id AS targetId,
                   space.space_name AS targetName,
                   (
                     SELECT COUNT(*)
                     FROM inv_item item
                     WHERE item.vault_id = space.vault_id
                       AND item.space_id = space.id
                       AND item.status = 1 AND item.deleted = 0
                   ) AS affectedItemCount,
                   (
                     SELECT COALESCE(SUM(item.quantity), 0)
                     FROM inv_item item
                     WHERE item.vault_id = space.vault_id
                       AND item.space_id = space.id
                       AND item.status = 1 AND item.deleted = 0
                   ) AS affectedQuantity,
                   (
                     SELECT COUNT(*)
                     FROM inv_space_category binding
                     WHERE binding.vault_id = space.vault_id
                       AND binding.space_id = space.id
                   ) AS bindingCount
            FROM inv_space space
            WHERE space.vault_id = #{vaultId} AND space.id = #{spaceId}
              AND space.status = 1 AND space.deleted = 0
            """)
    InventoryDeletionPreviewResult selectSpaceDeletionPreview(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /** 查询分类删除或当前空间解绑影响。 */
    @Select("""
            <script>
            SELECT category.id AS targetId,
                   category.category_name AS targetName,
                   (
                     SELECT COUNT(*)
                     FROM inv_item item
                     WHERE item.vault_id = category.vault_id
                       AND item.category_id = category.id
                       AND item.status = 1 AND item.deleted = 0
                     <if test="spaceId != null">
                       AND item.space_id = #{spaceId}
                     </if>
                   ) AS affectedItemCount,
                   (
                     SELECT COALESCE(SUM(item.quantity), 0)
                     FROM inv_item item
                     WHERE item.vault_id = category.vault_id
                       AND item.category_id = category.id
                       AND item.status = 1 AND item.deleted = 0
                     <if test="spaceId != null">
                       AND item.space_id = #{spaceId}
                     </if>
                   ) AS affectedQuantity,
                   (
                     SELECT COUNT(*)
                     FROM inv_space_category binding
                     WHERE binding.vault_id = category.vault_id
                       AND binding.category_id = category.id
                     <if test="spaceId != null">
                       AND binding.space_id = #{spaceId}
                     </if>
                   ) AS bindingCount
            FROM inv_category category
            WHERE category.vault_id = #{vaultId}
              AND category.id = #{categoryId}
              AND category.status = 1 AND category.deleted = 0
            </script>
            """)
    InventoryDeletionPreviewResult selectCategoryDeletionPreview(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId,
            @Param("spaceId") Long spaceId);
}
