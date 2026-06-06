package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 库存结构删除与迁移 Mapper。
 */
public interface InventoryDeletionMapper {

    /** 锁定并读取单个有效物品。 */
    @Select("""
            SELECT id, vault_id, space_id, category_id, item_name,
                   small_category, detail_location, quantity,
                   minimum_quantity, status, version, deleted
            FROM inv_item
            WHERE vault_id = #{vaultId} AND id = #{itemId}
              AND status = 1 AND deleted = 0
            FOR UPDATE
            """)
    InventoryItemDO selectItemForUpdate(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId);

    /** 锁定并读取空间内全部有效物品。 */
    @Select("""
            SELECT id, vault_id, space_id, category_id, item_name,
                   small_category, detail_location, quantity,
                   minimum_quantity, status, version, deleted
            FROM inv_item
            WHERE vault_id = #{vaultId} AND space_id = #{spaceId}
              AND status = 1 AND deleted = 0
            ORDER BY id
            FOR UPDATE
            """)
    List<InventoryItemDO> selectItemsBySpaceForUpdate(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /** 锁定并读取分类下全部有效物品。 */
    @Select("""
            SELECT id, vault_id, space_id, category_id, item_name,
                   small_category, detail_location, quantity,
                   minimum_quantity, status, version, deleted
            FROM inv_item
            WHERE vault_id = #{vaultId} AND category_id = #{categoryId}
              AND status = 1 AND deleted = 0
            ORDER BY id
            FOR UPDATE
            """)
    List<InventoryItemDO> selectItemsByCategoryForUpdate(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);

    /** 锁定并读取空间分类组合内全部有效物品。 */
    @Select("""
            SELECT id, vault_id, space_id, category_id, item_name,
                   small_category, detail_location, quantity,
                   minimum_quantity, status, version, deleted
            FROM inv_item
            WHERE vault_id = #{vaultId} AND space_id = #{spaceId}
              AND category_id = #{categoryId}
              AND status = 1 AND deleted = 0
            ORDER BY id
            FOR UPDATE
            """)
    List<InventoryItemDO> selectItemsByBindingForUpdate(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId,
            @Param("categoryId") long categoryId);

    /** 使用乐观锁迁移物品。 */
    @Update("""
            UPDATE inv_item
            SET space_id = #{targetSpaceId},
                category_id = #{targetCategoryId},
                detail_location = #{targetDetailLocation},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE vault_id = #{vaultId} AND id = #{itemId}
              AND version = #{version} AND status = 1 AND deleted = 0
            """)
    int moveItem(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId,
            @Param("targetSpaceId") long targetSpaceId,
            @Param("targetCategoryId") long targetCategoryId,
            @Param("targetDetailLocation") String targetDetailLocation,
            @Param("version") int version);

    /** 使用乐观锁清空并逻辑删除物品。 */
    @Update("""
            UPDATE inv_item
            SET quantity = 0, status = 0, deleted = 1,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE vault_id = #{vaultId} AND id = #{itemId}
              AND version = #{version} AND status = 1 AND deleted = 0
            """)
    int softDeleteItem(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId,
            @Param("version") int version);

    /** 逻辑删除空间。 */
    @Update("""
            UPDATE inv_space
            SET status = 0, deleted = 1, version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE vault_id = #{vaultId} AND id = #{spaceId}
              AND status = 1 AND deleted = 0
            """)
    int softDeleteSpace(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /** 逻辑删除分类。 */
    @Update("""
            UPDATE inv_category
            SET status = 0, deleted = 1, version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE vault_id = #{vaultId} AND id = #{categoryId}
              AND status = 1 AND deleted = 0
            """)
    int softDeleteCategory(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);

    /** 查询空间当前绑定的分类主键。 */
    @Select("""
            SELECT category_id
            FROM inv_space_category
            WHERE vault_id = #{vaultId} AND space_id = #{spaceId}
            ORDER BY category_id
            """)
    List<Long> selectCategoryIdsBySpace(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /** 查询分类当前绑定数量。 */
    @Select("""
            SELECT COUNT(*)
            FROM inv_space_category
            WHERE vault_id = #{vaultId} AND category_id = #{categoryId}
            """)
    int countCategoryBindings(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);

    /** 查询分类当前有效物品数量。 */
    @Select("""
            SELECT COUNT(*)
            FROM inv_item
            WHERE vault_id = #{vaultId} AND category_id = #{categoryId}
              AND status = 1 AND deleted = 0
            """)
    int countCategoryItems(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);

    /** 确保空间分类绑定存在。 */
    @Insert("""
            INSERT INTO inv_space_category (
                vault_id, space_id, category_id, sort_order
            )
            SELECT #{vaultId}, #{spaceId}, #{categoryId}, 100
            WHERE NOT EXISTS (
                SELECT 1 FROM inv_space_category
                WHERE vault_id = #{vaultId}
                  AND space_id = #{spaceId}
                  AND category_id = #{categoryId}
            )
            """)
    int ensureBinding(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId,
            @Param("categoryId") long categoryId);

    /** 删除空间的全部分类绑定。 */
    @Delete("""
            DELETE FROM inv_space_category
            WHERE vault_id = #{vaultId} AND space_id = #{spaceId}
            """)
    int deleteBindingsBySpace(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /** 删除分类的全部空间绑定。 */
    @Delete("""
            DELETE FROM inv_space_category
            WHERE vault_id = #{vaultId} AND category_id = #{categoryId}
            """)
    int deleteBindingsByCategory(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);
}
