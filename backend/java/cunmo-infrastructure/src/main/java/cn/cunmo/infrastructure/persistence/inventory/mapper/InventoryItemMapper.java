package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryItemDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 库存物品 Mapper。
 */
public interface InventoryItemMapper extends BaseMapper<InventoryItemDO> {

    /**
     * 按魔方域读取有效物品。
     */
    @Select("""
            SELECT id, vault_id, space_id, category_id, item_name,
                   small_category, detail_location, quantity,
                   minimum_quantity, status, version, deleted
            FROM inv_item
            WHERE id = #{itemId} AND vault_id = #{vaultId}
              AND status = 1 AND deleted = 0
            LIMIT 1
            """)
    InventoryItemDO selectOwnedItem(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId);

    /**
     * 统计空间分类组合有效物品数。
     */
    @Select("""
            SELECT COUNT(*)
            FROM inv_item
            WHERE vault_id = #{vaultId} AND space_id = #{spaceId}
              AND category_id = #{categoryId}
              AND status = 1 AND deleted = 0
            """)
    int countActiveItems(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId,
            @Param("categoryId") long categoryId);

    /**
     * 使用版本号更新库存数量。
     */
    @Update("""
            UPDATE inv_item
            SET quantity = #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{itemId} AND vault_id = #{vaultId}
              AND version = #{version} AND deleted = 0
            """)
    int updateQuantityWithVersion(
            @Param("vaultId") long vaultId,
            @Param("itemId") long itemId,
            @Param("quantity") int quantity,
            @Param("version") int version);
}
